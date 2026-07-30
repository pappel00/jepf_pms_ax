/** 예제 화면 2 — Sample1Page.jsx와 동일한 패턴. 파일 위치 안내 참고. */
export default function Sample2Page() {
  return (
    <div>
      <div className="page-header">
        <h2>예제 화면 2</h2>
      </div>
      <div className="card" style={{ width: 'auto', maxWidth: 480 }}>
        <div className="kv-row">
          <span className="k">어플리케이션</span>
          <span>STD_MGT (표준관리)</span>
        </div>
        <div className="kv-row">
          <span className="k">파일 위치</span>
          <span>frontend/src/pages/apps/STD_MGT/Sample2Page.jsx</span>
        </div>
        <div className="kv-row">
          <span className="k">라우트</span>
          <span>/workspace/STD_MGT/sample2</span>
        </div>
      </div>
      <p style={{ marginTop: 24 }} className="muted">
        같은 어플리케이션(STD_MGT) 안에서도 폴더 하나에 화면 여러 개를 자유롭게 추가할 수
        있습니다 — App.jsx에 라우트를 추가하고, Page 레코드를 하나 더 만들어 같은(또는 새)
        Menu 폴더에 매핑하면 됩니다.
      </p>
    </div>
  )
}
