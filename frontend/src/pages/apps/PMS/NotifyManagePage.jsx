import { useCallback, useEffect, useState } from 'react'
import { pmsNotifyApi } from '../../../api/entities'
import './pms.css'

const DUE_OPTIONS = [
  [0, '당일'],
  [1, '1일 전'],
  [2, '2일 전'],
  [3, '3일 전'],
  [5, '5일 전'],
  [7, '7일 전'],
]
const STATUS_LABEL = { SENT: '발송', FAILED: '실패', SKIPPED: '건너뜀' }
const checkbox = { width: 'auto', flex: 'none', margin: 0 }

/** Mattermost 연결 상태 — 서버 응답·봇 토큰·봇 인증을 확인한다. */
function ConnectionPanel({ onChecked }) {
  const [status, setStatus] = useState(null)
  const [busy, setBusy] = useState(false)

  const check = useCallback(async () => {
    setBusy(true)
    try {
      setStatus((await pmsNotifyApi.status()).data)
      onChecked?.()
    } catch (e) {
      setStatus({ serverReachable: false, message: e.message })
    } finally {
      setBusy(false)
    }
  }, [onChecked])
  useEffect(() => {
    check()
    // 처음 한 번만 확인한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const tone = !status ? '' : status.botReady ? 'done' : status.serverReachable ? 'manual' : 'late'
  const label = !status ? '확인 중...' : status.botReady ? '연결됨' : status.serverReachable ? '서버 정상 · 봇 미설정' : '서버 연결 실패'
  return (
    <div className="pms-panel" style={{ marginBottom: 14 }}>
      <h3 style={{ fontSize: 15, display: 'flex', alignItems: 'center', gap: 8 }}>
        Mattermost 연결 <span className={`pms-tag ${tone}`}>{label}</span>
        <button className="secondary small" style={{ marginLeft: 'auto' }} disabled={busy} onClick={check}>
          연결 확인
        </button>
      </h3>
      {status && (
        <p className="muted" style={{ margin: '4px 0 0' }}>
          {status.baseUrl} {status.serverVersion ? `· 서버 ${status.serverVersion.split('.').slice(0, 3).join('.')}` : ''}{' '}
          {status.botName ? `· 봇 @${status.botName}` : ''} — {status.message}
        </p>
      )}
    </div>
  )
}

/** 텔레그램 봇 연결 상태 — 봇 토큰 인증과 메시지 가져오기 여부를 보여 준다. */
function TelegramPanel() {
  const [status, setStatus] = useState(null)
  const [busy, setBusy] = useState(false)
  const check = useCallback(async () => {
    setBusy(true)
    try {
      setStatus((await pmsNotifyApi.telegramStatus()).data)
    } catch (e) {
      setStatus({ botReady: false, message: e.message })
    } finally {
      setBusy(false)
    }
  }, [])
  useEffect(() => {
    check()
  }, [check])
  const tone = !status ? '' : status.botReady ? 'done' : 'late'
  const label = !status ? '확인 중...' : status.botReady ? '연결됨' : status.tokenConfigured ? '봇 인증 실패' : '봇 토큰 미설정'
  return (
    <div className="pms-panel" style={{ marginBottom: 14 }}>
      <h3 style={{ fontSize: 15, display: 'flex', alignItems: 'center', gap: 8 }}>
        텔레그램 연결 <span className={`pms-tag ${tone}`}>{label}</span>
        <button className="secondary small" style={{ marginLeft: 'auto' }} disabled={busy} onClick={check}>
          연결 확인
        </button>
      </h3>
      {status && (
        <p className="muted" style={{ margin: '4px 0 0' }}>
          {status.botUsername ? `봇 @${status.botUsername} · ` : ''}
          {status.botReady ? `메시지 가져오기 ${status.polling ? '동작 중' : '꺼짐'} · ` : ''}
          {status.message} 사용자 연결은 「인력 관리」에서 연결 코드를 발급해 진행합니다.
        </p>
      )}
    </div>
  )
}

/** 이메일(SMTP) 연결 상태 — 설정 유무와 서버 접속·인증 결과. 협력사 인력 등 메신저를 쓰지 않는 사용자용 채널. */
function EmailPanel() {
  const [status, setStatus] = useState(null)
  const [busy, setBusy] = useState(false)
  const check = useCallback(async () => {
    setBusy(true)
    try {
      setStatus((await pmsNotifyApi.emailStatus()).data)
    } catch (e) {
      setStatus({ configured: false, connected: false, message: e.message })
    } finally {
      setBusy(false)
    }
  }, [])
  useEffect(() => {
    check()
  }, [check])
  const tone = !status ? '' : status.connected ? 'done' : status.configured ? 'late' : 'manual'
  const label = !status ? '확인 중...' : status.connected ? '연결됨' : status.configured ? '접속 실패' : 'SMTP 미설정'
  return (
    <div className="pms-panel" style={{ marginBottom: 14 }}>
      <h3 style={{ fontSize: 15, display: 'flex', alignItems: 'center', gap: 8 }}>
        이메일 연결 <span className={`pms-tag ${tone}`}>{label}</span>
        <button className="secondary small" style={{ marginLeft: 'auto' }} disabled={busy} onClick={check}>
          연결 확인
        </button>
      </h3>
      {status && (
        <p className="muted" style={{ margin: '4px 0 0' }}>
          {status.host ? `${status.host} · ` : ''}
          {status.from ? `보내는 주소 ${status.from} · ` : ''}
          {status.message} 받는 주소는 사용자 계정의 이메일입니다.
        </p>
      )}
    </div>
  )
}

/** 알림 설정 — 알림 종류별 사용 여부와 발송 시각·기준일. 저장해야 반영된다. */
function SettingsPanel() {
  const [s, setS] = useState(null)
  const [types, setTypes] = useState([])
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState(null)

  useEffect(() => {
    Promise.all([pmsNotifyApi.settings(), pmsNotifyApi.types()])
      .then(([a, b]) => {
        setS(a.data)
        setTypes(b.data || [])
      })
      .catch((e) => window.alert(e.message))
  }, [])
  if (!s) return null

  const set = (patch) => setS((p) => ({ ...p, ...patch }))
  const toggleDue = (d) => set({ dueDays: s.dueDays.includes(d) ? s.dueDays.filter((x) => x !== d) : [...s.dueDays, d].sort((a, b) => a - b) })
  const toggleType = (key) => set({ types: { ...s.types, [key]: !s.types[key] } })
  const save = async () => {
    setBusy(true)
    try {
      setS((await pmsNotifyApi.saveSettings(s)).data)
      window.alert('알림 설정을 저장했습니다.')
    } catch (e) {
      window.alert(e.message)
    } finally {
      setBusy(false)
    }
  }
  const run = async (dryRun) => {
    if (!dryRun && !window.confirm('지금 알릴 항목의 담당자에게 실제로 메시지를 보냅니다(저장된 설정 기준). 계속할까요?')) return
    setBusy(true)
    try {
      setResult((await pmsNotifyApi.run(dryRun)).data)
    } catch (e) {
      window.alert(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="pms-panel" style={{ marginBottom: 14 }}>
      <h3 style={{ fontSize: 15, display: 'flex', alignItems: 'center', gap: 8 }}>
        기한 알림 설정 <span className={`pms-tag ${s.enabled ? 'done' : ''}`}>{s.enabled ? '자동 발송 사용' : '자동 발송 꺼짐'}</span>
      </h3>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px 20px', alignItems: 'center' }}>
        <label style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <input type="checkbox" style={checkbox} checked={s.enabled} onChange={(e) => set({ enabled: e.target.checked })} /> 자동 발송
        </label>
        <label style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          발송 시각
          <select value={s.hour} onChange={(e) => set({ hour: Number(e.target.value) })}>
            {Array.from({ length: 24 }, (_, h) => (
              <option key={h} value={h}>
                {String(h).padStart(2, '0')}:00
              </option>
            ))}
          </select>
        </label>
        <span style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          기한 알림
          {DUE_OPTIONS.map(([d, label]) => (
            <label key={d} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <input type="checkbox" style={checkbox} checked={s.dueDays.includes(d)} onChange={() => toggleDue(d)} /> {label}
            </label>
          ))}
        </span>
        <label style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <input type="checkbox" style={checkbox} checked={s.startAlert} onChange={(e) => set({ startAlert: e.target.checked })} /> 업무 시작일 알림
        </label>
      </div>

      <table className="data-table pms-wbs" style={{ marginTop: 12 }}>
        <thead>
          <tr>
            <th style={{ width: 60 }}>발송</th>
            <th>메뉴 (폴더 › 페이지)</th>
            <th>업무 종류</th>
            <th>알림 대상 · 기준</th>
            <th>주기</th>
            <th>기본값</th>
          </tr>
        </thead>
        <tbody>
          {types.map((t, i) => [
            // 메뉴 폴더가 바뀔 때마다 폴더 행을 넣고, 아래에 그 폴더의 페이지별 업무 종류를 메뉴 순서대로 보여 준다.
            i === 0 || types[i - 1].folder !== t.folder ? (
              <tr key={`f-${t.folder}`} className="group-row">
                <td colSpan={6}>
                  <b>{t.folder}</b>
                </td>
              </tr>
            ) : null,
            <tr key={t.key}>
              <td>
                <input type="checkbox" style={checkbox} checked={!!s.types[t.key]} onChange={() => toggleType(t.key)} />
              </td>
              <td style={{ paddingLeft: 24 }}>{t.page}</td>
              <td>
                <b>{t.item}</b>
              </td>
              <td className="muted">{t.description}</td>
              <td>{t.scope === 'SLA' ? '매시' : '매일 발송 시각'}</td>
              <td>{t.defaultOn ? <span className="pms-tag done">발송</span> : <span className="pms-tag">안 보냄</span>}</td>
            </tr>,
          ])}
        </tbody>
      </table>

      <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
        <button className="primary small" disabled={busy} onClick={save}>
          저장
        </button>
        <button className="secondary small" disabled={busy} onClick={() => run(true)}>
          대상 미리보기
        </button>
        <button className="secondary small" disabled={busy} onClick={() => run(false)}>
          지금 실행
        </button>
      </div>
      <p className="muted" style={{ margin: '8px 0 0' }}>
        기한이 선택한 기준일(당일·n일 전)인 미완료 항목의 담당자에게 메시지를 보냅니다(업무 시작일은 시작일 알림 기준). 같은 항목·같은 알림은 하루에
        한 번만 보내고 이미 지난 기한(지연)은 알리지 않습니다. 담당자의 알림 채널이 Mattermost가 아니면 건너뛰고 발송 이력에 남깁니다. 「대상 미리보기」·「지금 실행」은
        자동 발송 여부와 관계없이 저장된 종류 설정으로 동작합니다.
      </p>
      {result && (
        <div className="pms-hint" style={{ boxShadow: 'none', background: '#f4f5f7', marginTop: 8 }}>
          {result.dryRun ? '미리보기' : '실행 결과'}: 대상 {result.targets}건
          {result.dryRun
            ? ` (오늘 이미 보낸 ${result.duplicates}건 제외 시 ${result.targets - result.duplicates}건 발송 예정)`
            : ` — 발송 ${result.sent} · 건너뜀 ${result.skipped} · 실패 ${result.failed} · 중복 제외 ${result.duplicates}`}
          {Object.entries(result.byType ?? {}).map(([k, v]) => (
            <span key={k} className="pms-tag manual" style={{ marginLeft: 6 }}>
              {k} {v}
            </span>
          ))}
          {result.samples?.map((x) => (
            <div key={x} className="muted">
              · {x}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

/** 최근 발송 이력(최대 50건). */
function LogPanel({ reloadKey }) {
  const [logs, setLogs] = useState([])
  const load = useCallback(() => {
    pmsNotifyApi
      .logs()
      .then((res) => setLogs(res.data || []))
      .catch(() => setLogs([]))
  }, [])
  useEffect(() => {
    load()
  }, [load, reloadKey])
  return (
    <div className="pms-panel">
      <h3 style={{ fontSize: 15, display: 'flex', alignItems: 'center', gap: 8 }}>
        발송 이력 <span className="count">{logs.length}</span>
        <button className="secondary small" style={{ marginLeft: 'auto' }} onClick={load}>
          새로고침
        </button>
      </h3>
      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>일시</th>
            <th>받는 사람</th>
            <th>채널</th>
            <th>종류</th>
            <th>내용</th>
            <th>결과</th>
            <th>사유</th>
          </tr>
        </thead>
        <tbody>
          {logs.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={7}>
                발송 이력이 없습니다.
              </td>
            </tr>
          )}
          {logs.map((l) => (
            <tr key={l.id}>
              <td style={{ whiteSpace: 'nowrap' }}>{(l.createdDate ?? '').replace('T', ' ').slice(0, 16)}</td>
              <td style={{ whiteSpace: 'nowrap' }}>{l.userName}</td>
              <td>{l.channelCode}</td>
              <td>{l.kindCode}</td>
              <td className="name-cell">{l.message}</td>
              <td>
                <span className={`pms-tag ${l.statusCode === 'SENT' ? 'done' : l.statusCode === 'FAILED' ? 'late' : ''}`}>{STATUS_LABEL[l.statusCode] ?? l.statusCode}</span>
              </td>
              <td className="muted">{l.errorMessage || '-'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/** 알림 관리 — Mattermost 연결 상태, 기한 알림 종류·시각 설정, 발송 이력(PM·센터장·관리자). */
export default function NotifyManagePage() {
  const [reloadKey, setReloadKey] = useState(0)
  return (
    <div>
      <div className="page-header">
        <div>
          <h2>알림 관리</h2>
          <p className="page-description">
            담당자에게 보내는 메신저 알림의 연결 상태와 발송 설정을 관리합니다. 업무·영업 노트·제안·이슈·운영 업무 등 기한이 도래한 항목을 정해진 시각에
            알려 주고, 사용자별 알림 채널·메신저 ID는 「인력 관리」에서 설정합니다.
          </p>
        </div>
      </div>
      <ConnectionPanel onChecked={() => setReloadKey((k) => k + 1)} />
      <TelegramPanel />
      <EmailPanel />
      <SettingsPanel />
      <LogPanel reloadKey={reloadKey} />
    </div>
  )
}
