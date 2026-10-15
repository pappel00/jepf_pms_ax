package com.platform.module.notice.domain;

import com.platform.framework.core.common.utils.PrefixedIdGenerator;
import com.platform.framework.security.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * 공지/동의서. 사진 속 BackOffice "Data Management > 공지/동의서 관리" 화면 대응.
 * module-code/module-file/module-terms와 달리 기본+상세(버전 이력) 2단 구조가 아니라
 * **단일 목록**이다 — 공지 하나하나가 제목/내용/게시기간을 통째로 갖는 완결된 항목이라
 * 개정 이력을 추적할 필요가 없다고 판단했다(약관과 다른 점).
 *
 * noticeType으로 "공지"와 "동의서"를 한 테이블에서 같이 관리한다 — 둘 다 "포탈에 게시기간을
 * 두고 노출하는 콘텐츠"라는 성격이 같아서 하나의 화면/엔티티로 합쳤다.
 *
 * ⚠️ 약관(TermsGroup)과 동일하게 포탈별로 관리된다 — portalId가 필수다. 다른 참조 컬럼들과
 * 동일하게 진짜 FK가 아니라 느슨한 문자열 참조다(module-notice는 module-portal에 의존하지 않는다).
 *
 * 팝업 노출 관련 상세 설정(applyType/popupFrequency/popupSize/popupShape/popupPosition)은
 * popupYn="Y"일 때 의미가 있는 값들이다 — popupYn="N"이어도 값을 저장해두는 것 자체는 막지
 * 않는다(나중에 팝업으로 전환할 때 이전에 넣어둔 설정이 남아있는 게 더 편하다).
 */
@Entity
@Table(name = "cmn_notice")
public class Notice extends TenantAwareEntity {

    @Column(name = "portal_id", length = 64, nullable = false)
    private String portalId;

    /** "공지" 또는 "동의서". */
    @Column(name = "notice_type", length = 20, nullable = false)
    private String noticeType;

    @Column(name = "title", length = 300, nullable = false)
    private String title;

    // @Lob을 쓰지 않는다: PostgreSQL에서 @Lob String은 oid(Large Object)로 매핑돼 TEXT 컬럼과 어긋난다.
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** 포탈 접속 시 팝업으로 띄울지 여부. */
    @Column(name = "popup_yn", length = 1, nullable = false)
    private String popupYn = "N";

    /** 적용유형: "로그인" 또는 "페이지" — 로그인 시점에 띄울지, 특정 페이지 접근 시 띄울지. */
    @Column(name = "apply_type", length = 20)
    private String applyType;

    /** 팝업빈도: 예) "1회" — 같은 사용자에게 얼마나 자주 다시 보여줄지. */
    @Column(name = "popup_frequency", length = 20)
    private String popupFrequency;

    /** 팝업 크기: "소형"/"중형"/"대형". */
    @Column(name = "popup_size", length = 20)
    private String popupSize;

    /** 팝업 모양: "레이어팝업"/"윈도우팝업". */
    @Column(name = "popup_shape", length = 20)
    private String popupShape;

    /** 팝업 위치: "중앙"/"좌상단"/"부모창". */
    @Column(name = "popup_position", length = 20)
    private String popupPosition;

    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn = "Y";

    protected Notice() {
    }

    private Notice(String portalId, String noticeType, String title, String content,
                    LocalDate startDate, LocalDate endDate, String popupYn,
                    String applyType, String popupFrequency, String popupSize,
                    String popupShape, String popupPosition) {
        super(PrefixedIdGenerator.generate("NTC"));
        this.portalId = portalId;
        this.noticeType = noticeType;
        this.title = title;
        this.content = content;
        this.startDate = startDate;
        this.endDate = endDate;
        this.popupYn = "Y".equals(popupYn) ? "Y" : "N";
        this.applyType = applyType;
        this.popupFrequency = popupFrequency;
        this.popupSize = popupSize;
        this.popupShape = popupShape;
        this.popupPosition = popupPosition;
    }

    public static Notice create(String portalId, String noticeType, String title, String content,
                                 LocalDate startDate, LocalDate endDate, String popupYn,
                                 String applyType, String popupFrequency, String popupSize,
                                 String popupShape, String popupPosition) {
        return new Notice(portalId, noticeType, title, content, startDate, endDate, popupYn,
                applyType, popupFrequency, popupSize, popupShape, popupPosition);
    }

    public void update(String noticeType, String title, String content,
                        LocalDate startDate, LocalDate endDate, String popupYn,
                        String applyType, String popupFrequency, String popupSize,
                        String popupShape, String popupPosition) {
        this.noticeType = noticeType;
        this.title = title;
        this.content = content;
        this.startDate = startDate;
        this.endDate = endDate;
        this.popupYn = "Y".equals(popupYn) ? "Y" : "N";
        this.applyType = applyType;
        this.popupFrequency = popupFrequency;
        this.popupSize = popupSize;
        this.popupShape = popupShape;
        this.popupPosition = popupPosition;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }

    public String getPortalId() {
        return portalId;
    }

    public String getNoticeType() {
        return noticeType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getPopupYn() {
        return popupYn;
    }

    public String getApplyType() {
        return applyType;
    }

    public String getPopupFrequency() {
        return popupFrequency;
    }

    public String getPopupSize() {
        return popupSize;
    }

    public String getPopupShape() {
        return popupShape;
    }

    public String getPopupPosition() {
        return popupPosition;
    }

    public String getUseYn() {
        return useYn;
    }
}
