import CrudPage from '../components/CrudPage'
import { applicationsApi, portalsApi } from '../api/entities'

export default function ApplicationsPage() {
  return (
    <CrudPage
      title="어플리케이션 관리"
      api={applicationsApi}
      search={{
        keywordFields: ['applicationCode', 'applicationName'],
        keywordLabel: '어플리케이션 검색 (코드/이름)',
        filters: [
          {
            key: 'portalId',
            label: '포탈',
            optionsApi: portalsApi,
            optionsLabel: (p) => p.portalName,
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
        { key: 'applicationCode', label: '어플리케이션 코드' },
        { key: 'applicationName', label: '어플리케이션명' },
        {
          key: 'portalId',
          label: '포탈',
          resolveApi: portalsApi,
          resolveLabel: (p) => p.portalName,
        },
        { key: 'useYn', label: '사용여부' },
        { key: 'deletedYn', label: '삭제여부' },
      ]}
      fields={[
        { name: 'applicationCode', label: '어플리케이션 코드', required: true, createOnly: true },
        { name: 'applicationName', label: '어플리케이션명', required: true },
        {
          name: 'portalId',
          label: '포탈',
          required: true,
          createOnly: true,
          type: 'select',
          optionsApi: portalsApi,
          optionsLabel: (p) => p.portalName,
        },
        { name: 'useYn', label: '사용여부', type: 'checkbox', editOnly: true, default: 'Y' },
      ]}
    />
  )
}
