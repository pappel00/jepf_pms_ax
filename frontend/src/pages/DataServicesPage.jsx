import CrudPage from '../components/CrudPage'
import { dataServicesApi } from '../api/entities'

export default function DataServicesPage() {
  return (
    <CrudPage
      title="데이터서비스 관리"
      description="내부 API 엔드포인트 메타데이터. 조회를 포함해 전체 ADMIN 권한이 필요합니다."
      api={dataServicesApi}
      search={{
        keywordFields: ['dataServiceCode', 'dataServiceName', 'url'],
        keywordLabel: '검색 (코드/이름/URL)',
        filters: [
          {
            key: 'httpMethod',
            label: 'HTTP Method',
            options: [
              { value: 'GET', label: 'GET' },
              { value: 'POST', label: 'POST' },
              { value: 'PUT', label: 'PUT' },
              { value: 'DELETE', label: 'DELETE' },
            ],
          },
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
        { key: 'dataServiceCode', label: '코드' },
        { key: 'dataServiceName', label: '이름' },
        { key: 'httpMethod', label: 'Method' },
        { key: 'url', label: 'URL' },
        { key: 'useYn', label: '사용여부' },
        { key: 'deletedYn', label: '삭제여부' },
      ]}
      fields={[
        { name: 'dataServiceCode', label: '데이터서비스 코드', required: true, createOnly: true },
        { name: 'dataServiceName', label: '이름', required: true },
        {
          name: 'httpMethod',
          label: 'HTTP Method',
          required: true,
          default: 'GET',
          type: 'select',
          options: [
            { value: 'GET', label: 'GET' },
            { value: 'POST', label: 'POST' },
            { value: 'PUT', label: 'PUT' },
            { value: 'DELETE', label: 'DELETE' },
          ],
        },
        { name: 'url', label: 'URL', required: true },
        { name: 'useYn', label: '사용여부', type: 'checkbox', editOnly: true, default: 'Y' },
      ]}
    />
  )
}
