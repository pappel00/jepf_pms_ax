import CrudPage from '../components/CrudPage'
import { portalsApi } from '../api/entities'

export default function PortalsPage() {
  return (
    <CrudPage
      title="포탈 관리"
      api={portalsApi}
      search={{
        keywordFields: ['portalCode', 'portalName'],
        keywordLabel: '포탈 검색 (코드/이름)',
        filters: [
          {
            key: 'deviceType',
            label: '디바이스 유형',
            options: [
              { value: 'PC', label: 'PC' },
              { value: 'MOBILE', label: 'MOBILE' },
              { value: 'ALL', label: 'ALL' },
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
        { key: 'portalCode', label: '포탈 코드' },
        { key: 'portalName', label: '포탈명' },
        { key: 'deviceType', label: '디바이스 유형' },
        { key: 'useYn', label: '사용여부' },
        { key: 'deletedYn', label: '삭제여부' },
      ]}
      fields={[
        { name: 'portalCode', label: '포탈 코드', required: true, createOnly: true },
        { name: 'portalName', label: '포탈명', required: true },
        {
          name: 'deviceType',
          label: '디바이스 유형',
          required: true,
          default: 'PC',
          type: 'select',
          options: [
            { value: 'PC', label: 'PC' },
            { value: 'MOBILE', label: 'MOBILE' },
            { value: 'ALL', label: 'ALL' },
          ],
        },
        { name: 'description', label: '설명' },
        { name: 'useYn', label: '사용여부', type: 'checkbox', editOnly: true, default: 'Y' },
      ]}
    />
  )
}
