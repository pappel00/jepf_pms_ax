import CrudPage from '../components/CrudPage'
import { messageResourcesApi } from '../api/entities'

export default function MessageResourcesPage() {
  return (
    <CrudPage
      title="메시지자원 관리"
      description="같은 messageResourceCode를 언어별로 여러 번 등록할 수 있습니다 (예: ko/en 각각 한 행씩)."
      api={messageResourcesApi}
      search={{
        keywordFields: ['messageResourceCode', 'value'],
        keywordLabel: '검색 (코드/값)',
        filters: [
          {
            key: 'resourceType',
            label: '유형',
            options: [
              { value: 'Alert', label: 'Alert' },
              { value: 'Text', label: 'Text' },
            ],
          },
          {
            key: 'languageId',
            label: '언어',
            options: [
              { value: 'ko', label: 'ko' },
              { value: 'en', label: 'en' },
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
        { key: 'messageResourceCode', label: '코드' },
        { key: 'resourceType', label: '유형' },
        { key: 'languageId', label: '언어' },
        { key: 'value', label: '값' },
        { key: 'useYn', label: '사용여부' },
        { key: 'deletedYn', label: '삭제여부' },
      ]}
      fields={[
        { name: 'messageResourceCode', label: '메시지자원 코드', required: true, createOnly: true },
        {
          name: 'resourceType',
          label: '유형',
          required: true,
          default: 'Text',
          createOnly: true,
          type: 'select',
          options: [
            { value: 'Alert', label: 'Alert' },
            { value: 'Text', label: 'Text' },
          ],
        },
        {
          name: 'languageId',
          label: '언어',
          required: true,
          createOnly: true,
          default: 'ko',
          type: 'select',
          options: [
            { value: 'ko', label: 'ko' },
            { value: 'en', label: 'en' },
          ],
        },
        { name: 'value', label: '값', required: true },
        { name: 'defaultValue', label: '기본값' },
        { name: 'useYn', label: '사용여부', type: 'checkbox', editOnly: true, default: 'Y' },
      ]}
    />
  )
}
