import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ companyCode: 'DEMO', loginId: '', password: '' })
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(form)
      navigate('/dashboard')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="page-center">
      <form className="card" onSubmit={handleSubmit}>
        <h1>JEPF 로그인</h1>

        {error && <div className="error-box">{error}</div>}

        <div className="field">
          <label htmlFor="companyCode">회사 코드</label>
          <input id="companyCode" name="companyCode" value={form.companyCode} onChange={handleChange} required />
        </div>
        <div className="field">
          <label htmlFor="loginId">아이디</label>
          <input id="loginId" name="loginId" value={form.loginId} onChange={handleChange} required />
        </div>
        <div className="field">
          <label htmlFor="password">비밀번호</label>
          <input
            id="password"
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            required
          />
        </div>

        <button className="primary" type="submit" disabled={loading}>
          {loading ? '로그인 중...' : '로그인'}
        </button>

        <div className="link-row">
          계정이 없으신가요? <Link to="/register">회원가입</Link>
        </div>
      </form>
    </div>
  )
}
