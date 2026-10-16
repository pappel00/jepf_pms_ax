import { useSearchParams } from 'react-router-dom'
import { TODAY } from './pmsStore'
import { useOpsProjects } from './opsStore'

export const dday = (date) => {
  if (!date) return ''
  const diff = Math.round((new Date(date) - new Date(TODAY)) / 86400000)
  return diff === 0 ? 'D-Day' : diff > 0 ? `D-${diff}` : `D+${-diff}`
}

/** 운영 중 프로젝트 선택 — `?project=ID`로 선택 상태를 유지한다. 운영 업무·이슈·영업 화면이 공통으로 쓴다. */
export function useOpsProject() {
  const projects = useOpsProjects()
  const [params, setParams] = useSearchParams()
  const project = projects.find((p) => p.id === params.get('project')) ?? projects[0]
  const bar = (
    <div className="search-bar">
      <div className="search-field">
        <label>운영 프로젝트</label>
        <select value={project?.id ?? ''} onChange={(e) => setParams({ project: e.target.value })} style={{ minWidth: 340 }}>
          {projects.length === 0 && <option value="">운영 중인 프로젝트가 없습니다</option>}
          {projects.map((p) => (
            <option key={p.id} value={p.id}>
              {p.id} · {p.name} ({p.client})
            </option>
          ))}
        </select>
      </div>
      {project && (
        <span className="muted">
          운영 기간 {project.operation.startDate} ~ {project.operation.endDate} ({dday(project.operation.endDate)}) · 운영 관리자 {project.operation.manager.name} · SLA {project.operation.sla.level} (응답 {project.operation.sla.respond}h / 해결 {project.operation.sla.resolve}h)
        </span>
      )}
    </div>
  )
  return { project, bar, projects }
}
