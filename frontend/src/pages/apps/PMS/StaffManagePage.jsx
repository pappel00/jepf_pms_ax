import { useCallback, useEffect, useState } from 'react'
import { pmsNotifyApi, pmsStaffApi } from '../../../api/entities'
import Modal from '../../../components/Modal'
import { useAuth } from '../../../context/AuthContext'
import './pms.css'
import { loadPmsUsers } from './pmsUsers'
import { GRADES } from './rateStore'
import { SortTh, useSort } from './sortUtil'
import { NOTIFY_CHANNELS } from './teamConfig'

/** PMS 사용자그룹 — 코드는 서버(StaffService.PMS_GROUPS)와 같다. */
const GROUPS = [
  { code: 'PMS_SALES', label: '영업/기획' },
  { code: 'PMS_PM', label: 'PM' },
  { code: 'PMS_MEMBER', label: '수행 인력' },
  { code: 'PMS_PARTNER', label: '협력사 인력' },
  { code: 'PMS_OPS', label: '운영 담당' },
  { code: 'PMS_MANAGER', label: '센터장 (승인 권한 포함)', managerOnly: true },
]
const groupLabel = (code) => GROUPS.find((g) => g.code === code)?.label.replace(/ \(.*\)$/, '') ?? code

const emptyStaff = () => ({
  userId: '',
  loginId: '',
  password: '',
  userName: '',
  email: '',
  useYn: 'Y',
  orgTypeCode: '자사',
  partnerCompany: '',
  jobGradeCode: '',
  notifyChannelCode: 'Mattermost',
  mattermostUserId: '',
  telegramChatId: '',
  groupCodes: [],
})

/** 텔레그램 연결 — 연결 코드를 발급하면 사용자가 봇에게 「/start 코드」를 보내 chat ID 가 저장된다. 연결되면 자동으로 감지한다. */
function TelegramLink({ s, set }) {
  const [link, setLink] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!link || s.telegramChatId) return undefined
    const timer = setInterval(async () => {
      try {
        const me = ((await pmsStaffApi.list()).data || []).find((u) => u.userId === s.userId)
        if (me?.telegramChatId) {
          set({ telegramChatId: me.telegramChatId })
          setLink(null)
        }
      } catch {
        // 다음 주기에 다시 확인한다.
      }
    }, 3000)
    return () => clearInterval(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [link, s.telegramChatId, s.userId])

  const issue = async () => {
    setBusy(true)
    try {
      setLink((await pmsNotifyApi.telegramLinkCode(s.userId)).data)
    } catch (e) {
      window.alert(e.message)
    } finally {
      setBusy(false)
    }
  }
  const unlink = async () => {
    if (!window.confirm('텔레그램 연결을 해제할까요? 해제하면 알림이 가지 않습니다.')) return
    try {
      await pmsNotifyApi.telegramUnlink(s.userId)
      set({ telegramChatId: '' })
    } catch (e) {
      window.alert(e.message)
    }
  }

  return (
    <div className="field">
      <label>텔레그램 연결</label>
      {!s.userId ? (
        <div className="pms-field-hint">저장한 뒤 다시 열어 연결 코드를 발급할 수 있습니다.</div>
      ) : s.telegramChatId ? (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span className="pms-tag done">연결됨</span> <span className="muted">chat {s.telegramChatId}</span>
          <button className="link-btn" onClick={unlink}>
            연결 해제
          </button>
        </div>
      ) : (
        <div>
          <button className="secondary small" disabled={busy} onClick={issue}>
            {link ? '코드 다시 발급' : '연결 코드 발급'}
          </button>
          {link && (
            <div className="pms-hint" style={{ boxShadow: 'none', background: '#f4f5f7', marginTop: 8 }}>
              연결 코드 <b style={{ fontSize: 16, letterSpacing: 2 }}>{link.code}</b> ({link.expiresInMinutes}분간 유효)
              <br />
              사용자가 텔레그램에서 <a href={link.link} target="_blank" rel="noreferrer">이 링크</a>를 열고 「시작」을 누르거나, 봇에게 <code>/start {link.code}</code> 를 보내면 연결됩니다.
              <br />
              <span className="muted">연결 대기 중… 연결되면 자동으로 표시됩니다.</span>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function StaffForm({ initial, canGrantManager, onClose, onSaved }) {
  const [s, setS] = useState(initial)
  const [saving, setSaving] = useState(false)
  const isNew = !s.userId
  const set = (patch) => setS((p) => ({ ...p, ...patch }))
  const toggle = (code) => set({ groupCodes: s.groupCodes.includes(code) ? s.groupCodes.filter((c) => c !== code) : [...s.groupCodes, code] })

  const err = isNew && !s.loginId.trim()
    ? '로그인 ID를 입력하세요.'
    : isNew && s.password.length < 8
      ? '초기 비밀번호는 8자 이상이어야 합니다.'
      : !s.userName.trim()
        ? '이름을 입력하세요.'
        : s.orgTypeCode === '협력사' && !s.partnerCompany.trim()
          ? '협력사명을 입력하세요.'
          : ''

  const submit = async () => {
    setSaving(true)
    try {
      const body = {
        email: s.email,
        userName: s.userName,
        orgTypeCode: s.orgTypeCode,
        partnerCompany: s.orgTypeCode === '협력사' ? s.partnerCompany : '',
        jobGradeCode: s.jobGradeCode,
        notifyChannelCode: s.notifyChannelCode,
        mattermostUserId: s.mattermostUserId,
        telegramChatId: s.telegramChatId,
        groupCodes: s.groupCodes,
      }
      if (isNew) await pmsStaffApi.create({ ...body, loginId: s.loginId.trim(), password: s.password })
      else await pmsStaffApi.update(s.userId, { ...body, useYn: s.useYn })
      await onSaved()
      onClose()
    } catch (e) {
      window.alert(e.message)
    } finally {
      setSaving(false)
    }
  }

  const f = (label, key, extra = {}, required) => (
    <div className="field">
      <label>
        {label}
        {required && <span className="req-mark"> *</span>}
      </label>
      <input value={s[key]} onChange={(e) => set({ [key]: e.target.value })} {...extra} />
    </div>
  )

  return (
    <Modal title={isNew ? '인력 등록' : `인력 수정 — ${s.loginId}`} onClose={onClose} width={600}>
      {isNew ? f('로그인 ID', 'loginId', {}, true) : <div className="pms-field-hint">로그인 ID: <b>{s.loginId}</b> (변경할 수 없습니다)</div>}
      {isNew && f('초기 비밀번호', 'password', { type: 'text', placeholder: '8자 이상 — 최초 로그인 후 변경 안내' }, true)}
      {f('이름', 'userName', {}, true)}
      {f('이메일', 'email', { type: 'email' })}
      <div className="field">
        <label>소속 구분</label>
        <select value={s.orgTypeCode} onChange={(e) => set({ orgTypeCode: e.target.value })}>
          <option>자사</option>
          <option>협력사</option>
        </select>
      </div>
      {s.orgTypeCode === '협력사' && f('협력사명', 'partnerCompany', {}, true)}
      <div className="field">
        <label>인력 등급</label>
        <select value={s.jobGradeCode} onChange={(e) => set({ jobGradeCode: e.target.value })}>
          <option value="">미지정</option>
          {GRADES.map((g) => (
            <option key={g}>{g}</option>
          ))}
        </select>
      </div>
      <div className="field">
        <label>알림 채널</label>
        <select value={s.notifyChannelCode || '미설정'} onChange={(e) => set({ notifyChannelCode: e.target.value })}>
          {NOTIFY_CHANNELS.map((c) => (
            <option key={c}>{c}</option>
          ))}
        </select>
      </div>
      {s.notifyChannelCode === 'Mattermost' && f('Mattermost ID', 'mattermostUserId')}
      {s.notifyChannelCode === '텔레그램' && <TelegramLink s={s} set={set} />}
      <div className="field">
        <label>PMS 역할 (여러 개 선택 가능)</label>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px 16px' }}>
          {GROUPS.map((g) => {
            const locked = g.managerOnly && !canGrantManager
            return (
              <label key={g.code} style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-start', gap: 8, margin: 0, textAlign: 'left', opacity: locked ? 0.5 : 1 }}>
                <input type="checkbox" style={{ width: 'auto', flex: 'none', margin: 0 }} checked={s.groupCodes.includes(g.code)} disabled={locked} onChange={() => toggle(g.code)} />
                <span>
                  {g.label}
                  {locked && <span className="muted"> — 센터장·관리자만 지정</span>}
                </span>
              </label>
            )
          })}
        </div>
      </div>
      {!isNew && (
        <label style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
          <input type="checkbox" style={{ width: 'auto', flex: 'none', margin: 0 }} checked={s.useYn === 'Y'} onChange={(e) => set({ useYn: e.target.checked ? 'Y' : 'N' })} /> 사용 (해제하면 로그인할 수 없습니다)
        </label>
      )}
      {err && <p style={{ color: '#c0392b' }}>{err}</p>}
      <button className="primary" disabled={!!err || saving} onClick={submit}>
        저장
      </button>
    </Modal>
  )
}

/** 인력 관리 — 계정·소속 구분·인력 등급·알림·PMS 역할을 한 곳에서 등록/수정한다(PM·센터장·관리자). */
export default function StaffManagePage() {
  const { auth } = useAuth()
  const canGrantManager = (auth?.roles ?? []).some((r) => ['ROLE_PMS_MANAGER', 'ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN'].includes(r))
  const [list, setList] = useState([])
  const [loaded, setLoaded] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [org, setOrg] = useState('')
  const [edit, setEdit] = useState(null)

  const reload = useCallback(async () => {
    const res = await pmsStaffApi.list()
    setList(res.data || [])
    setLoaded(true)
    loadPmsUsers() // 담당자 선택 목록 등 다른 화면의 사용자 캐시도 갱신
  }, [])
  useEffect(() => {
    reload().catch((e) => window.alert(e.message))
  }, [reload])

  const rows = list.filter(
    (u) => (!org || u.orgTypeCode === org) && (!keyword || `${u.loginId} ${u.userName} ${u.email ?? ''} ${u.partnerCompany ?? ''}`.toLowerCase().includes(keyword.toLowerCase())),
  )
  const { sort, onSort, sorted } = useSort(rows, {
    loginId: (u) => u.loginId,
    userName: (u) => u.userName,
    org: (u) => u.orgTypeCode,
    grade: (u) => GRADES.indexOf(u.jobGradeCode ?? ''),
    channel: (u) => u.notifyChannelCode,
    roles: (u) => u.groupCodes.length,
    use: (u) => u.useYn,
  })

  const checkMapping = (u) =>
    pmsNotifyApi
      .resolve(u.userId)
      .then((res) => window.alert(`${u.userName}: ${res.data.message}`))
      .catch((e) => window.alert(e.message))
  const sendTest = (u) => {
    if (!window.confirm(`${u.userName}님에게 Mattermost 테스트 메시지를 보낼까요?`)) return
    pmsNotifyApi
      .test(u.userId)
      .then((res) => window.alert(`${u.userName}: ${res.data.message}`))
      .catch((e) => window.alert(e.message))
  }

  const open = (u) =>
    setEdit({
      ...emptyStaff(),
      ...u,
      email: u.email ?? '',
      partnerCompany: u.partnerCompany ?? '',
      jobGradeCode: u.jobGradeCode ?? '',
      notifyChannelCode: u.notifyChannelCode ?? '미설정',
      mattermostUserId: u.mattermostUserId ?? '',
      telegramChatId: u.telegramChatId ?? '',
    })

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>인력 관리</h2>
          <p className="page-description">
            PMS를 쓰는 사람의 계정과 소속 구분(자사/협력사), 인력 등급, 알림 채널, PMS 역할을 등록·수정합니다. 프로젝트 투입은 「투입 관리」에서,
            등급별 단가는 「단가 관리」에서 다룹니다.
          </p>
        </div>
        <button className="primary small" onClick={() => setEdit(emptyStaff())}>
          + 인력 등록
        </button>
      </div>


      <div className="search-bar">
        <div className="search-field">
          <label>검색</label>
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="로그인 ID·이름·이메일·협력사" />
        </div>
        <div className="search-field">
          <label>소속 구분</label>
          <select value={org} onChange={(e) => setOrg(e.target.value)}>
            <option value="">전체</option>
            <option>자사</option>
            <option>협력사</option>
          </select>
        </div>
      </div>

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <SortTh k="loginId" sort={sort} onSort={onSort}>로그인 ID</SortTh>
            <SortTh k="userName" sort={sort} onSort={onSort}>이름</SortTh>
            <SortTh k="org" sort={sort} onSort={onSort}>소속 구분</SortTh>
            <SortTh k="grade" sort={sort} onSort={onSort}>인력 등급</SortTh>
            <SortTh k="channel" sort={sort} onSort={onSort}>알림 채널</SortTh>
            <SortTh k="roles" sort={sort} onSort={onSort}>PMS 역할</SortTh>
            <SortTh k="use" sort={sort} onSort={onSort}>사용</SortTh>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {sorted.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={8}>
                {loaded ? '인력이 없습니다.' : '불러오는 중...'}
              </td>
            </tr>
          )}
          {sorted.map((u) => (
            <tr key={u.userId} style={u.useYn === 'N' ? { opacity: 0.55 } : undefined}>
              <td>{u.loginId}</td>
              <td style={{ whiteSpace: 'nowrap' }}>
                <b>{u.userName}</b>
              </td>
              <td>{u.orgTypeCode === '협력사' ? `협력사${u.partnerCompany ? ` (${u.partnerCompany})` : ''}` : '자사'}</td>
              <td>{u.jobGradeCode || <span className="muted">미지정</span>}</td>
              <td style={{ whiteSpace: 'nowrap' }}>
                {u.notifyChannelCode && u.notifyChannelCode !== '미설정' ? u.notifyChannelCode : <span className="muted">미설정</span>}
                {u.notifyChannelCode === '텔레그램' &&
                  (u.telegramChatId ? (
                    <span className="pms-tag done" style={{ marginLeft: 6 }}>연결됨</span>
                  ) : (
                    <span className="pms-tag late" style={{ marginLeft: 6 }}>미연결</span>
                  ))}
              </td>
              <td>
                {u.groupCodes.length ? u.groupCodes.map((c) => (
                  <span key={c} className="pms-tag manual" style={{ marginRight: 4 }}>{groupLabel(c)}</span>
                )) : <span className="muted">-</span>}
              </td>
              <td>{u.useYn === 'Y' ? <span className="pms-tag done">사용</span> : <span className="pms-tag">중지</span>}</td>
              <td style={{ whiteSpace: 'nowrap' }}>
                <button className="link-btn" onClick={() => open(u)}>
                  수정
                </button>
                {/* 알림 채널에 맞는 확인·테스트 버튼만 보여 준다 */}
                {['Mattermost', '텔레그램', '이메일'].includes(u.notifyChannelCode) ? (
                  <>
                    {' '}
                    <button className="link-btn" onClick={() => checkMapping(u)}>
                      {u.notifyChannelCode === '텔레그램' ? '연결 확인' : u.notifyChannelCode === '이메일' ? '주소 확인' : 'Mattermost 확인'}
                    </button>{' '}
                    <button className="link-btn" onClick={() => sendTest(u)}>
                      테스트 발송
                    </button>
                  </>
                ) : (
                  <span className="muted" style={{ marginLeft: 8 }}>
                    채널 미설정
                  </span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="pms-field-hint">
        알림 연결 상태·발송 설정은 「알림 관리」 메뉴에서 합니다. 시스템 관리자 권한은 시스템 관리 &gt; 사용자그룹 관리에서 지정합니다. 계정 삭제·비밀번호 초기화도 시스템 관리 화면에서 합니다.
      </div>

      {edit && <StaffForm key={edit.userId || 'new'} initial={edit} canGrantManager={canGrantManager} onClose={() => setEdit(null)} onSaved={reload} />}
    </div>
  )
}
