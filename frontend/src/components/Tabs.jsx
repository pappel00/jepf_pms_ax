export default function Tabs({ tabs, activeKey, onChange }) {
  return (
    <div className="tabs-bar">
      {tabs.map((tab) => (
        <button
          key={tab.key}
          type="button"
          className={'tab-btn' + (tab.key === activeKey ? ' active' : '')}
          onClick={() => onChange(tab.key)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}
