/**
 * 예제 화면 1 — "시스템 관리 외 다른 어플리케이션"을 추가할 때의 파일 배치 예시.
 * `src/pages/apps/<앱코드>/` 밑에 화면을 두고, App.jsx에 `/workspace/<앱코드>/...` 라우트로
 * 등록한 뒤, 그 경로를 Page.pageFile에 넣으면 운영 화면 셸(WorkspaceLayout)의 메뉴 트리에서
 * 바로 연결된다 — 파일이 이 폴더 밑 어디에 있는지는 라우팅과 무관하다.
 */
export default function Sample1Page() {
  return (
    <div>
      <div className="page-header">
        <h2>예제 화면 1</h2>
      </div>
      <div className="card" style={{ width: 'auto', maxWidth: 480 }}>
        <div className="kv-row">
          <span className="k">어플리케이션</span>
          <span>STD_MGT (표준관리)</span>
        </div>
        <div className="kv-row">
          <span className="k">파일 위치</span>
          <span>frontend/src/pages/apps/STD_MGT/Sample1Page.jsx</span>
        </div>
        <div className="kv-row">
          <span className="k">라우트</span>
          <span>/workspace/STD_MGT/sample1</span>
        </div>
      </div>
      <p style={{ marginTop: 24 }} className="muted">
        이 화면은 새 어플리케이션을 만들 때의 예시입니다. 실제 화면으로 바꾸려면 이 컴포넌트
        내용만 교체하면 됩니다 — 라우트/DB 등록 방식은 그대로 재사용할 수 있습니다.
      </p>
    </div>
  )
}
