// PMS_화면설계서_목업.md → 단일 HTML(이미지 base64 포함) 변환. 추가 설치 없음.
// 실행: node docs/pms/tools/build-html.mjs   → docs/pms/PMS_화면설계서_목업.html
// 이 문서에서 쓰는 마크다운 문법(제목·인용·표·목록·코드블록·이미지·굵게·인라인코드·링크·구분선)만 지원한다.
import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const DOCS = join(dirname(fileURLToPath(import.meta.url)), '..')
const SRC = join(DOCS, 'PMS_화면설계서_목업.md')
const OUT = join(DOCS, 'PMS_화면설계서_목업.html')

const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
const inline = (s) =>
  esc(s)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2">$1</a>')

const img = (alt, path) => {
  const b64 = readFileSync(join(DOCS, decodeURI(path))).toString('base64')
  return `<img alt="${esc(alt)}" src="data:image/png;base64,${b64}">`
}

const slug = (t) => 'sec-' + t.replace(/[^\p{L}\p{N}]+/gu, '-')
const lines = readFileSync(SRC, 'utf8').split(/\r?\n/)
const out = []
const toc = []
let i = 0
while (i < lines.length) {
  const l = lines[i]
  let m
  if (l.startsWith('```')) {
    const buf = []
    i++
    while (i < lines.length && !lines[i].startsWith('```')) buf.push(lines[i++])
    i++
    out.push(`<pre>${esc(buf.join('\n'))}</pre>`)
  } else if ((m = l.match(/^(#{1,3}) (.*)/))) {
    const level = m[1].length
    const id = slug(m[2])
    if (level >= 2) toc.push({ level, text: m[2], id })
    out.push(`<h${level} id="${id}">${inline(m[2])}</h${level}>`)
    i++
  } else if (l.trim() === '---') {
    out.push('<hr>')
    i++
  } else if (l.startsWith('>')) {
    const buf = []
    while (i < lines.length && lines[i].startsWith('>')) buf.push(lines[i++].replace(/^>\s?/, ''))
    out.push(`<blockquote>${buf.map(inline).join('<br>')}</blockquote>`)
  } else if (l.startsWith('|')) {
    const rows = []
    while (i < lines.length && lines[i].startsWith('|')) rows.push(lines[i++])
    const cells = (r) => r.replace(/^\||\|$/g, '').split('|').map((c) => c.trim())
    const head = cells(rows[0])
    const body = rows.slice(2).map(cells)
    out.push(`<table><thead><tr>${head.map((c) => `<th>${inline(c)}</th>`).join('')}</tr></thead><tbody>${body.map((r) => `<tr>${r.map((c) => `<td>${inline(c)}</td>`).join('')}</tr>`).join('')}</tbody></table>`)
  } else if ((m = l.match(/^!\[(.*?)\]\((.*?)\)/))) {
    out.push(`<p class="shot">${img(m[1], m[2])}</p>`)
    i++
  } else if (/^\d+\. /.test(l) || /^- /.test(l)) {
    const ordered = /^\d+\. /.test(l)
    const items = []
    while (i < lines.length && (ordered ? /^\d+\. / : /^- /).test(lines[i])) items.push(lines[i++].replace(ordered ? /^\d+\. / : /^- /, ''))
    const tag = ordered ? 'ol' : 'ul'
    out.push(`<${tag}>${items.map((t) => `<li>${inline(t)}</li>`).join('')}</${tag}>`)
  } else if (l.trim() === '') {
    i++
  } else {
    out.push(`<p>${inline(l)}</p>`)
    i++
  }
}

const tocHtml = `<nav class="toc"><strong>목차</strong><ul>${toc.filter((t) => t.level === 2).map((t) => `<li><a href="#${t.id}">${esc(t.text)}</a></li>`).join('')}</ul></nav>`
// 첫 번째 h1 바로 뒤(표지 안내 인용 + 작성일 목록 다음)에 목차를 넣기 위해 첫 <h2> 앞에 삽입
const body = out.join('\n').replace('<h2 ', `${tocHtml}\n<h2 `)

const html = `<!doctype html>
<html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>PMS 화면 설계서</title>
<style>
  :root { --fg:#1f2430; --muted:#5b6275; --line:#e2e5ee; --accent:#3156d3; --bg:#fff; --soft:#f5f7fc; }
  @media (prefers-color-scheme: dark) { :root { --fg:#e6e8ef; --muted:#9aa1b5; --line:#2c3142; --accent:#8aa4ff; --bg:#14161d; --soft:#1b1e28; } }
  body { margin:0; background:var(--bg); color:var(--fg); font:15px/1.7 "Malgun Gothic","Apple SD Gothic Neo",system-ui,sans-serif; }
  main { max-width:1100px; margin:0 auto; padding:32px 24px 80px; }
  h1 { font-size:30px; margin:0 0 12px; }
  h2 { font-size:22px; margin:40px 0 12px; padding-top:8px; }
  h3 { font-size:17px; margin:24px 0 8px; }
  hr { border:0; border-top:1px solid var(--line); margin:36px 0 0; }
  blockquote { margin:12px 0; padding:10px 16px; background:var(--soft); border-left:4px solid var(--accent); color:var(--muted); border-radius:0 8px 8px 0; }
  a { color:var(--accent); }
  code { background:var(--soft); padding:1px 6px; border-radius:4px; font-size:.9em; }
  pre { background:var(--soft); padding:14px 16px; border-radius:8px; overflow:auto; line-height:1.5; }
  table { border-collapse:collapse; width:100%; margin:12px 0; font-size:14px; }
  th, td { border:1px solid var(--line); padding:6px 10px; text-align:left; vertical-align:top; }
  th { background:var(--soft); }
  .shot { margin:16px 0; } .shot img { max-width:100%; height:auto; border:1px solid var(--line); border-radius:8px; display:block; }
  ol li, ul li { margin:4px 0; }
  .toc { background:var(--soft); border-radius:8px; padding:12px 20px; margin:20px 0; } .toc ul { columns:2; margin:6px 0 0; padding-left:18px; }
  @media (max-width:700px) { .toc ul { columns:1; } main { padding:20px 16px 60px; } }
  @media print { .shot img { page-break-inside:avoid; } h2 { page-break-before:always; } }
</style></head>
<body><main>
${body}
</main></body></html>`

writeFileSync(OUT, html)
console.log(`OK ${OUT} (${Math.round(html.length / 1024)}KB, 이미지 ${(html.match(/<img /g) || []).length}장, 섹션 ${toc.filter((t) => t.level === 2).length}개)`)
