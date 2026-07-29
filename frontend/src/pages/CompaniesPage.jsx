import CrudPage from '../components/CrudPage'
import { companiesApi } from '../api/entities'

export default function CompaniesPage() {
  return (
    <CrudPage
      title="회사 관리"
      description="테넌트 마스터. 생성/수정/삭제는 SYSTEM_ADMIN 권한이 필요합니다."
      api={companiesApi}
      search={{
        keywordFields: ['companyCode', 'companyName'],
        keywordLabel: '회사 검색 (코드/이름)',
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
        { key: 'companyCode', label: '회사 코드' },
        { key: 'companyName', label: '회사명' },
        { key: 'description', label: '설명' },
        { key: 'useYn', label: '사용여부' },
        { key: 'deletedYn', label: '삭제여부' },
      ]}
      fields={[
        { name: 'companyCode', label: '회사 코드', required: true, createOnly: true },
        { name: 'companyName', label: '회사명', required: true },
        { name: 'description', label: '설명' },
        { name: 'useYn', label: '사용여부', type: 'checkbox', editOnly: true, default: 'Y' },
      ]}
    />
  )
}
