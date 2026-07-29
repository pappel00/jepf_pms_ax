import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    companyCode: 'DEMO',
    loginId: '',
    password: '',
    email: '',
    userName: '',
  })
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await register(form)
      setSuccess(true)
      setTimeout(() => navigate('/login'), 1200)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page-center">
      <form className="card" onSubmit={handleSubmit}>
        <h1>JEPF 회원가입</h1>

        {error && <div className="error-box">{error}</div>}
        {success && <div className="success-box">가입 완료! 로그인 화면으로 이동합니다...</div>}

        <div className="field">
          <label htmlFor="companyCode">회사 코드</label>
          <input id="companyCode" name="companyCode" value={form.companyCode} onChange={handleChange} required />
        </div>
        <div className="field">
          <label htmlFor="loginId">아이디</label>
          <input id="loginId" name="loginId" value={form.loginId} onChange={handleChange} required />
        </div>
        <div className="field">
          <label htmlFor="password">비밀번호 (8자 이상)</label>
          <input
            id="password"
            name="password"
            type="password"
            minLength={8}
            value={form.password}
            onChange={handleChange}
            required
          />
        </div>
        <div className="field">
          <label htmlFor="userName">이름</label>
          <input id="userName" name="userName" value={form.userName} onChange={handleChange} required />
        </div>
        <div className="field">
          <label htmlFor="email">이메일</label>
          <input id="email" name="email" type="email" value={form.email} onChange={handleChange} />
        </div>

        <button className="primary" type="submit" disabled={loading}>
          {loading ? '가입 중...' : '가입하기'}
        </button>

        <div className="link-row">
          이미 계정이 있으신가요? <Link to="/login">로그인</Link>
        </div>
      </form>
    </div>
  )
}
