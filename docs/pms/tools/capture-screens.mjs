// PMS 목업 화면 캡처 — Edge(headless) + CDP, 추가 설치 없음 (Node 24 내장 WebSocket/fetch 사용).
//
// 사전 조건: 백엔드(http://localhost:8080)와 프론트(http://localhost:5173)가 떠 있어야 한다.
// 실행:      node docs/pms/tools/capture-screens.mjs        → docs/pms/images/NN-메뉴명.png 생성
// 주의:      목업은 새로고침하면 더미 데이터가 초기화되므로 로그인 직후부터는 SPA 이동(pushState)만 쓴다.
//            캡처 순서는 상태 의존성 때문에 메뉴 순서와 다르다(MENUS의 order 참고).
import { spawn } from 'node:child_process'
import { mkdirSync, mkdtempSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const OUT = join(HERE, '..', 'images')
const EDGE = process.env.EDGE_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const API = process.env.API_URL || 'http://localhost:8080'
const WEB = process.env.WEB_URL || 'http://localhost:5173'
const PORT = 9333
const WIDTH = 1440

// no = 이미지/문서 번호(메뉴 순서), order = 캡처 순서.
//  - 투입 관리(staffing)는 운영 전환 전에 찍어야 과투입이 과장되지 않는다.
//  - 운영 「프로젝트 관리」는 운영단계전환을 실제 실행한 뒤에 찍는다(ops-transition 실행 → ops-project).
export const MENUS = [
  { no: 1, route: 'customers', name: '고객사관리' },
  { no: 2, route: 'customer-contacts', name: '고객사담당자관리' },
  { no: 3, route: 'sales-notes', name: '영업노트' },
  { no: 4, route: 'projects', name: '프로젝트관리' },
  { no: 5, route: 'rfp', name: 'RFP관리' },
  { no: 6, route: 'rfp-eval', name: 'RFP평가' },
  { no: 7, route: 'rfp-eval-admin', name: 'RFP평가관리' },
  { no: 8, route: 'proposals', name: '제안서관리' },
  { no: 9, route: 'phase-transition', name: '수행단계전환', prep: 'selectFirstRow' },
  { no: 10, route: 'ops-transition', name: '운영단계전환', prep: 'selectFirstRow' },
  { no: 11, route: 'exec-project', name: '수행-프로젝트관리' },
  { no: 12, route: 'doc-tailoring', name: '문서테일러링관리' },
  { no: 13, route: 'wbs', name: 'WBS진척관리' },
  { no: 14, route: 'issues', name: '이슈관리' },
  { no: 15, route: 'report-weekly', name: '주간보고' },
  { no: 16, route: 'report-monthly', name: '월간보고' },
  { no: 17, route: 'dashboard', name: '내업무대시보드' },
  { no: 18, route: 'ops-project', name: '운영-프로젝트관리', after: 'executeOpsTransition' },
  { no: 19, route: 'staffing', name: '투입관리' },
  { no: 20, route: 'rates', name: '단가관리' },
]
const ONLY = (process.env.ONLY || '').split(',').filter(Boolean) // 일부만 다시 찍을 때: ONLY=customers,rfp
const ORDER = [...MENUS.filter((m) => m.no !== 18).sort((a, b) => a.no - b.no), MENUS.find((m) => m.no === 18)].filter((m) => !ONLY.length || ONLY.includes(m.route))

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

// ---------- CDP ----------
async function launchEdge() {
  const dir = mkdtempSync(join(tmpdir(), 'edge-cap-'))
  const proc = spawn(EDGE, [`--remote-debugging-port=${PORT}`, `--user-data-dir=${dir}`, '--headless=new', '--hide-scrollbars', '--no-first-run', '--disable-gpu', `--window-size=${WIDTH},900`, 'about:blank'], { stdio: 'ignore' })
  for (let i = 0; i < 50; i++) {
    try {
      const list = await (await fetch(`http://127.0.0.1:${PORT}/json`)).json()
      const page = list.find((t) => t.type === 'page')
      if (page) return { proc, wsUrl: page.webSocketDebuggerUrl }
    } catch {
      /* 아직 기동 중 */
    }
    await sleep(200)
  }
  throw new Error('Edge 기동 실패')
}

function connect(wsUrl) {
  const ws = new WebSocket(wsUrl)
  let id = 0
  const pending = new Map()
  ws.onmessage = (e) => {
    const m = JSON.parse(e.data)
    if (m.id && pending.has(m.id)) {
      const { resolve, reject } = pending.get(m.id)
      pending.delete(m.id)
      m.error ? reject(new Error(m.error.message)) : resolve(m.result)
    }
  }
  const opened = new Promise((r) => (ws.onopen = r))
  const send = (method, params = {}) =>
    new Promise((resolve, reject) => {
      const i = ++id
      pending.set(i, { resolve, reject })
      ws.send(JSON.stringify({ id: i, method, params }))
    })
  const evaluate = async (expression) => {
    const r = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true })
    if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || r.exceptionDetails.text)
    return r.result.value
  }
  return { opened, send, evaluate, close: () => ws.close() }
}

// ---------- 페이지 내 보조 동작 ----------
const waitFor = (cdp, expr, ms = 8000) =>
  (async () => {
    const end = Date.now() + ms
    while (Date.now() < end) {
      if (await cdp.evaluate(expr)) return true
      await sleep(150)
    }
    return false
  })()

const goto = (cdp, route) =>
  cdp.evaluate(`(() => { history.pushState({}, '', '/workspace/PMS/${route}'); dispatchEvent(new PopStateEvent('popstate')); })()`)

const PREPS = {
  // 대상 프로젝트 목록의 첫 행을 눌러 선택한 상태를 만든다.
  async selectFirstRow(cdp) {
    await cdp.evaluate(`document.querySelector('tbody tr')?.click()`)
    await sleep(500)
  },
}

async function executeOpsTransition(cdp) {
  await goto(cdp, 'ops-transition')
  await sleep(900)
  await PREPS.selectFirstRow(cdp)
  const ok = await cdp.evaluate(`(() => { const b=[...document.querySelectorAll('button')].find(x=>x.textContent.includes('운영단계전환 실행')); if(!b) return false; b.click(); return true })()`)
  await sleep(800)
  if (!ok) throw new Error('운영단계전환 실행 버튼을 찾지 못함')
}

async function shoot(cdp, file) {
  // 충분히 큰 뷰포트에서 콘텐츠 하단까지의 높이를 재고, 그 높이로 맞춰 캡처한다.
  await cdp.send('Emulation.setDeviceMetricsOverride', { width: WIDTH, height: 3000, deviceScaleFactor: 1, mobile: false })
  await sleep(350)
  const { top, need } = await cdp.evaluate(`(() => {
    const body = document.querySelector('.app-body'); const top = body.getBoundingClientRect().top
    const c = document.querySelector('.app-content')
    // 콘텐츠 영역은 뷰포트 높이만큼 늘어나 있으므로, 실제 내용(하위 요소)의 맨 아래를 기준으로 잰다.
    const bottom = [...c.querySelectorAll('*')].reduce((m, e) => (getComputedStyle(e).position === 'fixed' ? m : Math.max(m, e.getBoundingClientRect().bottom)), 0)
    const rail = document.querySelector('.workspace-rail')
    const railNeed = rail ? [...rail.children].reduce((s, e) => s + e.getBoundingClientRect().height, 0) : 0
    return { top, need: Math.ceil(Math.max(bottom - top + 40, railNeed + 20, 640)) }
  })()`)
  const height = Math.min(need, 6000)
  await cdp.send('Emulation.setDeviceMetricsOverride', { width: WIDTH, height: Math.ceil(top) + height, deviceScaleFactor: 1, mobile: false })
  await sleep(350)
  const shot = await cdp.send('Page.captureScreenshot', { format: 'png', clip: { x: 0, y: top, width: WIDTH, height, scale: 1 } })
  writeFileSync(file, Buffer.from(shot.data, 'base64'))
  return height
}

// ---------- 메인 ----------
async function main() {
  mkdirSync(OUT, { recursive: true })
  const res = await fetch(`${API}/api/v1/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ companyCode: 'DEMO', loginId: 'admin', password: 'admin1234' }) })
  const login = await res.json()
  if (!login.data) throw new Error('로그인 실패: ' + JSON.stringify(login))

  const { proc, wsUrl } = await launchEdge()
  const cdp = connect(wsUrl)
  const failed = []
  try {
    await cdp.opened
    await cdp.send('Page.enable')
    await cdp.send('Runtime.enable')
    await cdp.send('Page.navigate', { url: `${WEB}/login` })
    await sleep(1500)
    await cdp.evaluate(`localStorage.setItem('jepf-auth', ${JSON.stringify(JSON.stringify(login.data))})`)
    await cdp.send('Page.navigate', { url: `${WEB}/workspace/home` })
    await waitFor(cdp, `document.querySelectorAll('.workspace-app-tab').length > 0`, 15000)
    await cdp.evaluate(`window.confirm = () => true; window.alert = () => {}; window.prompt = () => ''`)
    const clicked = await cdp.evaluate(`(() => { const t=[...document.querySelectorAll('.workspace-app-tab')].find(x=>x.textContent.trim()==='PMS'); if(!t) return false; t.click(); return true })()`)
    if (!clicked) throw new Error("상단 'PMS' 탭을 찾지 못함(메뉴 시드/권한 확인)")
    await waitFor(cdp, `document.querySelectorAll('.workspace-menu-link').length > 0 || document.querySelectorAll('.tree-toggle').length > 0`)

    for (const m of ORDER) {
      const file = join(OUT, `${String(m.no).padStart(2, '0')}-${m.name}.png`)
      try {
        if (m.after === 'executeOpsTransition') await executeOpsTransition(cdp)
        await goto(cdp, m.route)
        await sleep(900)
        // 접힌 폴더를 모두 펼쳐(메뉴 행 안의 토글은 클릭하면 그 페이지로 이동해 버리므로 제외) 메뉴 트리 전체가 보이게 한다.
        await cdp.evaluate(`[...document.querySelectorAll('.tree-toggle')].filter((b) => !b.closest('.workspace-menu-link') && b.textContent.includes('▸')).forEach((b) => b.click())`)
        await sleep(300)
        const ok = await waitFor(cdp, `!!document.querySelector('.workspace-menu-link.active')`, 3000)
        if (!ok) throw new Error('메뉴 선택(active) 상태가 아님 — 메뉴 시드/라우트 확인')
        if (m.prep) await PREPS[m.prep](cdp)
        if (process.env.DEBUG) console.log('   at', await cdp.evaluate(`location.pathname + ' | ' + (document.querySelector('h2')?.innerText ?? '')`))
        const h = await shoot(cdp, file)
        console.log(`OK  ${String(m.no).padStart(2, '0')} ${m.route} (${h}px)`)
      } catch (e) {
        failed.push(`${m.no} ${m.route}: ${e.message}`)
        console.log(`FAIL ${m.no} ${m.route}: ${e.message}`)
      }
    }
  } finally {
    cdp.close()
    proc.kill()
  }
  if (failed.length) console.log('\n캡처 실패:\n' + failed.join('\n'))
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
