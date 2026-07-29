import CrudPage from '../components/CrudPage'
import { rolesApi } from '../api/entities'

export default function RolesPage() {
  return (
    <CrudPage
      title="Role 관리"
      description="역할 마스터. 사용자그룹에 매핑해야 실제 사용자에게 권한이 반영됩니다 (사용자그룹 관리 화면 참고)."
      api={rolesApi}
      search={{
        keywordFields: ['roleCode', 'roleName'],
        keywordLabel: 'Role 검색 (코드/이름)',
        filters: [
          {
            key: 'useYn',
            label: '사용여부',
            options: [
              { value: 'Y', label: '사용' },
              { value: 'N', label: '미사용' },
            ],
          },
        ],
      }}
      columns={[
        { key: 'roleCode', label: '역할 코드' },
        { key: 'roleName', label: '역할명' },
        { key: 'category', label: '분류' },
        { key: 'description', label: '설명' },
        { key: 'useYn', label: '사용여부' },
        { key: 'deletedYn', label: '삭제여부' },
      ]}
      fields={[
        { name: 'roleCode', label: '역할 코드 (예: ROLE_ADMIN)', required: true, createOnly: true },
        { name: 'roleName', label: '역할명', required: true },
        { name: 'roleAlias', label: '별칭' },
        { name: 'category', label: '분류' },
        { name: 'description', label: '설명' },
        { name: 'useYn', label: '사용여부', type: 'checkbox', editOnly: true, default: 'Y' },
      ]}
    />
  )
}
