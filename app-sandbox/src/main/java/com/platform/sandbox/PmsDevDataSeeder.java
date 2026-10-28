package com.platform.sandbox;

import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.pms.domain.Customer;
import com.platform.module.pms.domain.CustomerContact;
import com.platform.module.pms.domain.Issue;
import com.platform.module.pms.domain.IssueHistory;
import com.platform.module.pms.domain.IssueTask;
import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.Proposal;
import com.platform.module.pms.domain.ProposalChecklist;
import com.platform.module.pms.domain.ProposalMapping;
import com.platform.module.pms.domain.ProposalVersion;
import com.platform.module.pms.domain.Rfp;
import com.platform.module.pms.domain.RfpEval;
import com.platform.module.pms.domain.RfpEvaluator;
import com.platform.module.pms.domain.RfpItem;
import com.platform.module.pms.domain.StatusHistory;
import com.platform.module.pms.domain.UserProfile;
import com.platform.module.pms.domain.WbsTask;
import com.platform.module.pms.dto.TeamDto;
import com.platform.module.pms.dto.TransitionDto;
import com.platform.module.pms.event.PmsCodeBootstrapListener;
import com.platform.module.pms.repository.CustomerContactRepository;
import com.platform.module.pms.repository.CustomerRepository;
import com.platform.module.pms.repository.IssueHistoryRepository;
import com.platform.module.pms.repository.IssueRepository;
import com.platform.module.pms.repository.IssueTaskRepository;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.ProposalChecklistRepository;
import com.platform.module.pms.repository.ProposalMappingRepository;
import com.platform.module.pms.repository.ProposalRepository;
import com.platform.module.pms.repository.ProposalVersionRepository;
import com.platform.module.pms.repository.RfpEvalRepository;
import com.platform.module.pms.repository.RfpEvaluatorRepository;
import com.platform.module.pms.repository.RfpItemRepository;
import com.platform.module.pms.repository.RfpRepository;
import com.platform.module.pms.repository.StatusHistoryRepository;
import com.platform.module.pms.repository.TransitionRepository;
import com.platform.module.pms.repository.UserProfileRepository;
import com.platform.module.pms.repository.WbsTaskRepository;
import com.platform.module.pms.service.TransitionService;
import com.platform.module.tenant.domain.Company;
import com.platform.module.tenant.repository.CompanyRepository;
import com.platform.module.user.domain.User;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.domain.UserGroupUser;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupUserRepository;
import com.platform.module.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * ⚠️ 로컬 개발/테스트 전용 — PMS 목업의 더미 데이터(고객사·담당자·프로젝트·샘플 사용자)를 DB에 채운다.
 * DevDataSeeder(회사·admin 생성) 다음에 실행되며, 이미 데이터가 있으면 건드리지 않는다(멱등).
 *
 * 샘플 사용자(모두 비밀번호 pms1234, DEFAULT_USER 그룹): kimsales(김영업) choisales(최영업) leedesign(이설계)
 * kimdev(김개발) parkpartner(박협력, 협력사) jungqa(정QA). admin(관리자)은 DevDataSeeder 가 만든다.
 * 운영 배포 시 제거해야 한다.
 */
@Component
@Order(2)
public class PmsDevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PmsDevDataSeeder.class);

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupUserRepository userGroupUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final PmsCodeBootstrapListener pmsCodeBootstrapListener;
    private final CustomerRepository customerRepository;
    private final CustomerContactRepository contactRepository;
    private final ProjectRepository projectRepository;
    private final UserProfileRepository profileRepository;
    private final StatusHistoryRepository historyRepository;
    private final TransitionRepository transitionRepository;
    private final TransitionService transitionService;
    private final WbsTaskRepository wbsTaskRepository;
    private final IssueRepository issueRepository;
    private final IssueTaskRepository issueTaskRepository;
    private final IssueHistoryRepository issueHistoryRepository;
    private final RfpRepository rfpRepository;
    private final RfpItemRepository rfpItemRepository;
    private final RfpEvaluatorRepository rfpEvaluatorRepository;
    private final RfpEvalRepository rfpEvalRepository;
    private final ProposalRepository proposalRepository;
    private final ProposalVersionRepository proposalVersionRepository;
    private final ProposalMappingRepository proposalMappingRepository;
    private final ProposalChecklistRepository proposalChecklistRepository;

    public PmsDevDataSeeder(CompanyRepository companyRepository, UserRepository userRepository,
                            UserGroupRepository userGroupRepository, UserGroupUserRepository userGroupUserRepository,
                            PasswordEncoder passwordEncoder, PmsCodeBootstrapListener pmsCodeBootstrapListener,
                            CustomerRepository customerRepository, CustomerContactRepository contactRepository,
                            ProjectRepository projectRepository, UserProfileRepository profileRepository,
                            StatusHistoryRepository historyRepository, TransitionRepository transitionRepository,
                            TransitionService transitionService, WbsTaskRepository wbsTaskRepository,
                            IssueRepository issueRepository, IssueTaskRepository issueTaskRepository,
                            IssueHistoryRepository issueHistoryRepository,
                            RfpRepository rfpRepository, RfpItemRepository rfpItemRepository, RfpEvaluatorRepository rfpEvaluatorRepository, RfpEvalRepository rfpEvalRepository, ProposalRepository proposalRepository, ProposalVersionRepository proposalVersionRepository, ProposalMappingRepository proposalMappingRepository, ProposalChecklistRepository proposalChecklistRepository) {
        this.rfpRepository = rfpRepository;
        this.rfpItemRepository = rfpItemRepository;
        this.rfpEvaluatorRepository = rfpEvaluatorRepository;
        this.rfpEvalRepository = rfpEvalRepository;
        this.proposalRepository = proposalRepository;
        this.proposalVersionRepository = proposalVersionRepository;
        this.proposalMappingRepository = proposalMappingRepository;
        this.proposalChecklistRepository = proposalChecklistRepository;
        this.wbsTaskRepository = wbsTaskRepository;
        this.issueRepository = issueRepository;
        this.issueTaskRepository = issueTaskRepository;
        this.issueHistoryRepository = issueHistoryRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.userGroupUserRepository = userGroupUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.pmsCodeBootstrapListener = pmsCodeBootstrapListener;
        this.customerRepository = customerRepository;
        this.contactRepository = contactRepository;
        this.projectRepository = projectRepository;
        this.profileRepository = profileRepository;
        this.historyRepository = historyRepository;
        this.transitionRepository = transitionRepository;
        this.transitionService = transitionService;
    }

    @Override
    public void run(String... args) {
        Company company = companyRepository.findByCompanyCodeAndDeletedYn("DEMO", "N").orElse(null);
        if (company == null) {
            return;
        }
        TenantContext.setTenantId(company.getId());
        try {
            pmsCodeBootstrapListener.seedDefaults();
            Map<String, User> users = seedUsers();
            seedProfiles(users);
            Map<String, Customer> customers = seedCustomers();
            Map<String, CustomerContact> contacts = seedContacts(customers);
            seedProjects(customers, contacts, users);
            seedExecutionTeams(users);
            seedWbsAndIssues(users);
            seedRfpsAndProposals(users);
        } finally {
            TenantContext.clear();
        }
    }

    // ---------- 사용자 ----------

    private Map<String, User> seedUsers() {
        Map<String, User> byName = new HashMap<>();
        userRepository.findByLoginIdAndDeletedYn("admin", "N").ifPresent(u -> byName.put(u.getUserName(), u));
        UserGroup defaultGroup = userGroupRepository.findByGroupCodeAndDeletedYn("DEFAULT_USER", "N").orElse(null);
        String[][] samples = {
                {"kimsales", "김영업"}, {"choisales", "최영업"}, {"leedesign", "이설계"},
                {"kimdev", "김개발"}, {"parkpartner", "박협력"}, {"jungqa", "정QA"},
        };
        // 센터장: 단계전환·투입 변경 승인 시연용(ADMIN_GROUP → ROLE_ADMIN). 요청자 본인은 승인할 수 없으므로 별도 승인자가 필요하다.
        userRepository.findByLoginIdAndDeletedYn("centerhead", "N").orElseGet(() -> userRepository.saveAndFlush(
                User.create("centerhead", passwordEncoder.encode("pms1234"), "centerhead@example.com", "센터장")));
        User head = userRepository.findByLoginIdAndDeletedYn("centerhead", "N").orElseThrow();
        userGroupRepository.findByGroupCodeAndDeletedYn("ADMIN_GROUP", "N").ifPresent(g -> {
            if (!userGroupUserRepository.existsByUserGroupIdAndUserId(g.getId(), head.getId())) {
                userGroupUserRepository.saveAndFlush(UserGroupUser.create(g.getId(), head.getId()));
            }
        });
        byName.put(head.getUserName(), head);
        for (String[] s : samples) {
            User user = userRepository.findByLoginIdAndDeletedYn(s[0], "N").orElseGet(() -> {
                User created = userRepository.saveAndFlush(
                        User.create(s[0], passwordEncoder.encode("pms1234"), s[0] + "@example.com", s[1]));
                log.info("[PmsDevDataSeeder] 샘플 사용자 생성: loginId={}, userName={} (password=pms1234)", s[0], s[1]);
                return created;
            });
            if (defaultGroup != null && !userGroupUserRepository.existsByUserGroupIdAndUserId(defaultGroup.getId(), user.getId())) {
                userGroupUserRepository.saveAndFlush(UserGroupUser.create(defaultGroup.getId(), user.getId()));
            }
            byName.put(user.getUserName(), user);
        }
        return byName;
    }

    private void seedProfiles(Map<String, User> users) {
        // 인력 등급은 단가표(목업)와 맞춘다.
        Object[][] rows = {
                {"관리자", "자사", null, "특급"}, {"이설계", "자사", null, "특급"}, {"김개발", "자사", null, "고급"},
                {"박협력", "협력사", "가나다시스템", "중급"}, {"정QA", "자사", null, "중급"},
                {"김영업", "자사", null, null}, {"최영업", "자사", null, null},
        };
        for (Object[] r : rows) {
            User user = users.get((String) r[0]);
            if (user == null || profileRepository.findByUserIdAndDeletedYn(user.getId(), "N").isPresent()) {
                continue;
            }
            UserProfile profile = UserProfile.create().withUserId(user.getId()).withOrgTypeCode((String) r[1])
                    .withPartnerCompany((String) r[2]).withJobGradeCode((String) r[3]);
            if ("협력사".equals(r[1])) {
                profile.withNotifyChannelCode("Mattermost");
            }
            profileRepository.saveAndFlush(profile);
        }
    }

    // ---------- 고객사·담당자 ----------

    private Map<String, Customer> seedCustomers() {
        Map<String, Customer> byName = new HashMap<>();
        customerRepository.findAll().forEach(c -> byName.put(c.getCustomerName(), c));
        if (!byName.isEmpty()) {
            return byName;
        }
        String[][] rows = {
                {"CUS-001", "(샘플) 고객사", "IT/서비스", "거래 중", "02-100-0000", "서울 강남구 테헤란로 1", "123-45-67890"},
                {"CUS-002", "한빛전자", "제조", "거래 중", "031-888-0000", "경기 수원시 영통구", null},
                {"CUS-003", "대한물산", "유통/물류", "거래 중", "02-555-0000", "서울 중구 세종대로", null},
                {"CUS-004", "새솔금융", "금융", "거래 중", "02-777-0000", "서울 영등포구 여의대로", null},
                {"CUS-005", "푸른물류", "유통/물류", "잠재", "031-777-0000", "경기 평택시", null},
                {"CUS-006", "미래제약", "의료/제약", "잠재", "043-200-0000", "충북 청주시", null},
                {"CUS-007", "동서에너지", "에너지", "거래 중", "052-300-0000", "울산 남구", null},
        };
        for (String[] r : rows) {
            Customer c = customerRepository.saveAndFlush(Customer.create().withCustomerCode(r[0]).withCustomerName(r[1])
                    .withIndustryCode(r[2]).withDealStatusCode(r[3]).withPhone(r[4]).withAddress(r[5]).withBizNo(r[6]));
            byName.put(c.getCustomerName(), c);
        }
        log.info("[PmsDevDataSeeder] 샘플 고객사 {}건 생성", rows.length);
        return byName;
    }

    /** 키 = 담당자 이름 (목업의 담당자 이름은 고객사 간 중복이 없다). */
    private Map<String, CustomerContact> seedContacts(Map<String, Customer> customers) {
        Map<String, CustomerContact> byName = new HashMap<>();
        contactRepository.findAll().forEach(c -> byName.put(c.getContactName(), c));
        if (!byName.isEmpty()) {
            return byName;
        }
        String[][] rows = {
                {"CTC-001", "(샘플) 고객사", "한고객", "부장", "IT기획팀", "010-1234-5678", "client@example.com"},
                {"CTC-002", "대한물산", "박담당", "과장", "정보시스템팀", "02-555-0101", null},
                {"CTC-003", "새솔금융", "이팀장", "팀장", "디지털전략팀", "010-9876-5432", "lee@saesol.example"},
                {"CTC-004", "푸른물류", "정구매", "대리", "구매팀", "031-777-2020", null},
                {"CTC-005", "대한물산", "송운영", "대리", "운영팀", "02-555-0202", null},
                {"CTC-006", "한빛전자", "유협업", "과장", "업무혁신팀", "031-888-0101", null},
                {"CTC-007", "동서에너지", "오책임", "책임", "스마트팩토리추진팀", "052-300-1100", null},
                {"CTC-008", "한빛전자", "정이사", "이사", "경영지원본부", "031-888-0001", "jung@hanbit.example"},
                {"CTC-009", "(샘플) 고객사", "최상무", "상무", "IT전략실", "02-100-0001", null},
        };
        for (String[] r : rows) {
            CustomerContact c = contactRepository.saveAndFlush(CustomerContact.create().withContactCode(r[0])
                    .withCustomerId(customers.get(r[1]).getId()).withContactName(r[2]).withJobTitle(r[3]).withDeptName(r[4])
                    .withPhone(r[5]).withEmail(r[6]));
            byName.put(c.getContactName(), c);
        }
        log.info("[PmsDevDataSeeder] 샘플 고객사 담당자 {}건 생성", rows.length);
        return byName;
    }

    // ---------- 프로젝트 ----------

    private void seedProjects(Map<String, Customer> customers, Map<String, CustomerContact> contacts, Map<String, User> users) {
        if (!projectRepository.findAll().isEmpty()) {
            return;
        }
        // 코드, 이름, 발주사, 시작, 종료, 금액, 상태, 확률, 담당자, 영업, PM, 사업구분, 입찰방식, 공고번호, 제안마감, 발표일, 경쟁사, 예상원가, 실패코드, 실패상세, 비고
        Object[][] rows = {
                {"PRJ-2026-001", "통합업무포탈 구축사업", "(샘플) 고객사", "2026-09-01", "2027-03-31", 3200000000L, "수행 중", 100, "한고객", "관리자", "관리자",
                        "SI 구축", "협상에 의한 계약", "2026-0412", "2026-07-10", "2026-07-24", "○○시스템, △△정보", 2650000000L, null, null, "WBS 목업의 기준 프로젝트"},
                {"PRJ-2026-002", "차세대 인사시스템 구축", "한빛전자", "2027-01-04", "2027-09-30", 1850000000L, "공고 중", 20, null, "김영업", null,
                        "SI 구축", "공개 경쟁입찰", "2026-1103", "2026-10-30", null, null, null, null, null, "RFP 검토 중"},
                {"PRJ-2026-003", "전사 문서중앙화(ECM) 고도화", "대한물산", "2026-12-01", "2027-05-31", 920000000L, "제안 중", 30, "박담당", "최영업", "관리자",
                        "SI 구축", "제한 경쟁입찰", "2026-0988", "2026-10-21", "2026-11-03", "□□소프트", 740000000L, null, null, null},
                {"PRJ-2026-004", "모바일 앱 통합 플랫폼", "새솔금융", "2026-12-15", "2027-08-31", 2400000000L, "수주", 100, "이팀장", "관리자", "이설계",
                        "SI 구축", "협상에 의한 계약", "2026-0871", "2026-09-25", "2026-10-16", "◇◇정보통신, ○○시스템", 1980000000L, null, null, "발표 PT 준비"},
                {"PRJ-2026-005", "ERP 연계 인터페이스 개선", "푸른물류", "2026-11-16", "2027-02-26", 560000000L, "협상 중", 80, "정구매", "김영업", "김개발",
                        "SI 구축", "수의계약", null, null, null, null, 430000000L, null, null, "단가 협상 중"},
                {"PRJ-2026-006", "데이터 거버넌스 컨설팅", "미래제약", "2027-02-01", "2027-06-30", 300000000L, "공고 전", 10, null, "최영업", null,
                        "컨설팅", null, null, null, null, null, null, null, null, "내년 상반기 발주 예정 (사전 미팅)"},
                {"PRJ-2026-007", "포털 유지보수 (2027)", "대한물산", "2027-01-01", "2027-12-31", 480000000L, "수주", 100, "박담당", "관리자", "김개발",
                        "SM 운영/유지보수", "수의계약", null, null, null, null, 390000000L, null, null, "계약서 날인 진행"},
                {"PRJ-2026-008", "스마트팩토리 MES 구축", "동서에너지", "2026-10-01", "2027-06-30", 4100000000L, "실패", 0, "오책임", "김영업", null,
                        "SI 구축", "공개 경쟁입찰", "2026-0655", "2026-08-14", "2026-08-28", "◇◇정보통신", null, "가격 경쟁력 열세", "경쟁사 대비 약 12% 높음", null},
                {"PRJ-2026-009", "그룹사 인트라넷 운영", "대한물산", "2026-09-01", "2026-12-31", 360000000L, "수행 중", 100, "송운영", "최영업", null,
                        "SM 운영/유지보수", "수의계약", null, null, null, null, 290000000L, null, null, "2026년 하반기 운영 계약"},
                {"PRJ-2026-010", "사내 협업툴 구축", "한빛전자", "2026-03-02", "2026-08-28", 780000000L, "수행 종료", 100, "유협업", "관리자", "관리자",
                        "SI 구축", "협상에 의한 계약", "2026-0210", "2026-01-16", "2026-01-30", null, 620000000L, null, null, "2026-08-28 검수 완료"},
                {"PRJ-2026-011", "그룹웨어 유지보수 (2026 하반기)", "대한물산", "2026-01-05", "2026-06-30", 150000000L, "운영 중", 100, "박담당", "관리자", "관리자",
                        "SM 운영/유지보수", "수의계약", null, null, null, null, 110000000L, null, null, "2026-06-30 검수 완료 후 유상 유지보수 전환"},
        };
        for (Object[] r : rows) {
            Customer customer = customers.get((String) r[2]);
            CustomerContact contact = r[8] == null ? null : contacts.get((String) r[8]);
            Project p = Project.create()
                    .withProjectCode((String) r[0]).withProjectName((String) r[1])
                    .withStatusCode((String) r[6]).withProbabilityPct(BigDecimal.valueOf((Integer) r[7]))
                    .withCustomerId(customer.getId()).withClientName(customer.getCustomerName())
                    .withStartDate(LocalDate.parse((String) r[3])).withEndDate(LocalDate.parse((String) r[4]))
                    .withAmount((Long) r[5]).withVatIncludedYn("N")
                    .withSalesUserId(users.get((String) r[9]).getId())
                    .withPmUserId(r[10] == null ? null : users.get((String) r[10]).getId())
                    .withBusinessTypeCode((String) r[11]).withBidTypeCode((String) r[12]).withNoticeNo((String) r[13])
                    .withDueDate(r[14] == null ? null : LocalDate.parse((String) r[14]))
                    .withPresentDate(r[15] == null ? null : LocalDate.parse((String) r[15]))
                    .withCompetitors((String) r[16]).withCostEstimate((Long) r[17])
                    .withCostSourceType(r[17] == null ? null : "MANUAL")
                    .withFailReasonCode((String) r[18]).withFailReasonDetail((String) r[19]).withMemo((String) r[20])
                    .withViewRestrictedYn("N");
            if (contact != null) {
                p.withContactId(contact.getId()).withContactName(contact.getContactName()).withContactTitle(contact.getJobTitle())
                        .withContactPhone(contact.getPhone()).withContactEmail(contact.getEmail());
            }
            if ("PRJ-2026-010".equals(r[0])) {
                p.withExecEndDate(LocalDate.parse("2026-08-28"));
            }
            Project saved = projectRepository.saveAndFlush(p);
            historyRepository.save(StatusHistory.create().withTargetTypeCode("PROJECT").withTargetId(saved.getId())
                    .withToStatus(saved.getStatusCode()).withActorUserId(saved.getSalesUserId())
                    .withActedAt(LocalDateTime.now()).withMemo("샘플 데이터 등록"));
        }
        log.info("[PmsDevDataSeeder] 샘플 프로젝트 {}건 생성", rows.length);
    }

    // ---------- 수행 팀(프로젝트 관리자·투입 인력) + 승인된 단계전환 이력 ----------

    private TeamDto.Member member(Map<String, User> users, String name, String role, List<String> phases, double mm,
                                  String from, String to, String notify) {
        return new TeamDto.Member(users.get(name).getId(), role, phases, BigDecimal.valueOf(mm), null, LocalDate.parse(from),
                LocalDate.parse(to), false, "편집", notify, List.of(), Map.of());
    }

    private void seedExecutionTeams(Map<String, User> users) {
        if (!transitionRepository.findAll().isEmpty()) {
            return;
        }
        List<String> all4 = List.of("분석", "설계", "개발", "테스트");
        List<String> devTest = List.of("개발", "테스트");
        User head = users.get("센터장");
        // 코드, 전환일, 단계, 관리자, 관리자 역할, 팀
        Object[][] rows = {
                {"PRJ-2026-001", "2026-09-01", all4, "관리자", "PM", List.of(
                        member(users, "이설계", "분석/설계", List.of("분석", "설계"), 4, "2026-09-01", "2026-12-31", null),
                        member(users, "김개발", "개발", devTest, 6, "2026-10-01", "2027-03-31", null),
                        member(users, "박협력", "개발", List.of("개발"), 4.5, "2026-10-06", "2027-02-26", "Mattermost"),
                        member(users, "정QA", "QA", List.of("테스트"), 3, "2026-11-02", "2027-02-26", null))},
                {"PRJ-2026-009", "2026-08-25", devTest, "이설계", "PL", List.of(
                        member(users, "김개발", "개발", devTest, 2, "2026-09-01", "2026-12-31", null),
                        member(users, "정QA", "QA", List.of("테스트"), 1.5, "2026-10-01", "2026-12-31", null))},
                {"PRJ-2026-010", "2026-02-23", all4, "관리자", "PM", List.of(
                        member(users, "이설계", "분석/설계", List.of("분석", "설계"), 3, "2026-03-02", "2026-05-29", null),
                        member(users, "김개발", "개발", devTest, 4, "2026-04-01", "2026-08-28", null),
                        member(users, "박협력", "개발", List.of("개발"), 3, "2026-04-01", "2026-07-31", "Mattermost"),
                        member(users, "정QA", "QA", List.of("테스트"), 1.5, "2026-07-01", "2026-08-28", null))},
                {"PRJ-2026-011", "2026-01-02", devTest, "관리자", "PM", List.of(
                        member(users, "관리자", "PL", devTest, 3, "2026-01-05", "2026-06-30", null))},
        };
        for (Object[] r : rows) {
            Project project = projectRepository.findByProjectCodeAndDeletedYn((String) r[0], "N").orElse(null);
            if (project == null) {
                continue;
            }
            LocalDate date = LocalDate.parse((String) r[1]);
            @SuppressWarnings("unchecked")
            List<TeamDto.Member> team = (List<TeamDto.Member>) r[5];
            TeamDto.Spec spec = new TeamDto.Spec(new TeamDto.Manager(users.get((String) r[3]).getId(), (String) r[4]), team, false);
            TransitionDto.Request request = new TransitionDto.Request((List<String>) r[2], null, List.of(), 0, "auto",
                    "샘플 단계전환 (" + r[0] + ")", spec);
            transitionService.seedApproved(project.getId(), request, date, users.get((String) r[3]).getId(), head.getId());
            projectRepository.saveAndFlush(project.withExecStartDate(date));
        }
        log.info("[PmsDevDataSeeder] 샘플 수행 팀·단계전환 이력 {}건 생성", rows.length);
    }

    // ---------- WBS · 이슈 (pms-seed-wbs.json: 목업의 더미 WBS·이슈를 옮긴 것) ----------

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null || v.toString().isBlank() ? null : v.toString();
    }

    private static LocalDate date(Map<String, Object> m, String key) {
        String v = str(m, key);
        return v == null ? null : LocalDate.parse(v);
    }

    private static BigDecimal num(Map<String, Object> m, String key) {
        String v = str(m, key);
        return v == null ? null : new BigDecimal(v);
    }

    @SuppressWarnings("unchecked")
    private void seedWbsAndIssues(Map<String, User> users) {
        Project project = projectRepository.findByProjectCodeAndDeletedYn("PRJ-2026-001", "N").orElse(null);
        if (project == null || wbsTaskRepository.existsByProjectIdAndDeletedYn(project.getId(), "N")) {
            return;
        }
        Map<String, Object> root;
        try (InputStream in = new ClassPathResource("pms-seed-wbs.json").getInputStream()) {
            root = JsonMapper.builder().build().readValue(in, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("[PmsDevDataSeeder] pms-seed-wbs.json 을 읽지 못해 WBS·이슈 샘플을 건너뜁니다: {}", e.getMessage());
            return;
        }
        Map<String, WbsTask> byMockId = new HashMap<>();
        List<WbsTask> tasks = new java.util.ArrayList<>();
        int order = 0;
        for (Map<String, Object> t : (List<Map<String, Object>>) root.get("tasks")) {
            String mockId = str(t, "id");
            String type = str(t, "parentId") == null ? "PHASE" : mockId.startsWith("C-") ? "CATEGORY" : "LEAF";
            User assignee = str(t, "assignee") == null ? null : users.get(str(t, "assignee"));
            WbsTask task = WbsTask.create().withProjectId(project.getId()).withNodeTypeCode(type).withPhaseCode(str(t, "phase"))
                    .withCategoryName(str(t, "category")).withTaskName(str(t, "name")).withReqCode(str(t, "reqId"))
                    .withFeatureName(str(t, "feature")).withAssigneeUserId(assignee == null ? null : assignee.getId())
                    .withStartDate(date(t, "start")).withEndDate(date(t, "end")).withWeight(num(t, "weight"))
                    .withProgressPct("LEAF".equals(type) ? (num(t, "progress") == null ? BigDecimal.ZERO : num(t, "progress")) : null)
                    .withSortOrder(order++);
            byMockId.put(mockId, task);
            tasks.add(task);
        }
        for (Map<String, Object> t : (List<Map<String, Object>>) root.get("tasks")) {
            WbsTask task = byMockId.get(str(t, "id"));
            if (str(t, "parentId") != null) {
                task.withParentId(byMockId.get(str(t, "parentId")).getId());
            }
            if (str(t, "predId") != null) {
                task.withPredTaskId(byMockId.get(str(t, "predId")).getId());
            }
        }
        wbsTaskRepository.saveAllAndFlush(tasks);

        int issues = 0;
        for (Map<String, Object> i : (List<Map<String, Object>>) root.get("issues")) {
            Project p = projectRepository.findByProjectCodeAndDeletedYn(str(i, "projectId"), "N").orElse(null);
            if (p == null) {
                continue;
            }
            Issue issue = issueRepository.saveAndFlush(Issue.create().withIssueCode(str(i, "id")).withProjectId(p.getId())
                    .withTitle(str(i, "title")).withTypeCode(str(i, "type")).withPriorityCode(str(i, "priority"))
                    .withStatusCode(str(i, "status")).withReporterUserId(users.get(str(i, "reporter")).getId())
                    .withAssigneeUserId(users.get(str(i, "assignee")).getId()).withRegisteredDate(date(i, "registeredDate"))
                    .withDueDate(date(i, "dueDate")).withResolvedDate(date(i, "resolvedDate"))
                    .withImpactDays(Integer.valueOf(str(i, "impactDays"))).withContent(str(i, "content"))
                    .withResolution(str(i, "resolution")));
            for (Object taskId : (List<Object>) i.get("taskIds")) {
                WbsTask linked = byMockId.get(taskId.toString());
                if (linked != null) {
                    issueTaskRepository.save(IssueTask.create().withIssueId(issue.getId()).withTaskId(linked.getId())
                            .withLinkedBy(issue.getReporterUserId()).withLinkedAt(issue.getRegisteredDate().atStartOfDay()));
                }
            }
            int n = 0;
            for (Map<String, Object> h : (List<Map<String, Object>>) i.get("history")) {
                String text = str(h, "text");
                String action = text.startsWith("이슈 등록") ? "등록" : text.startsWith("상태 변경") ? "상태 변경" : "메모";
                issueHistoryRepository.save(IssueHistory.create().withIssueId(issue.getId()).withActionCode(action)
                        .withActorUserId(users.get(str(h, "author")).getId())
                        .withActedAt(date(h, "date").atStartOfDay().plusMinutes(n++)).withHistoryText(text));
            }
            issues++;
        }
        log.info("[PmsDevDataSeeder] 샘플 WBS 업무 {}건·이슈 {}건 생성", tasks.size(), issues);
    }

    // ---------- RFP · 평가 · 제안서 (pms-seed-rfp.json: 목업의 더미 데이터를 옮긴 것) ----------

    @SuppressWarnings("unchecked")
    private void seedRfpsAndProposals(Map<String, User> users) {
        if (!rfpRepository.findAll().isEmpty()) {
            return;
        }
        Map<String, Object> root;
        try (InputStream in = new ClassPathResource("pms-seed-rfp.json").getInputStream()) {
            root = JsonMapper.builder().build().readValue(in, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("[PmsDevDataSeeder] pms-seed-rfp.json 을 읽지 못해 RFP·제안서 샘플을 건너뜁니다: {}", e.getMessage());
            return;
        }
        Map<String, Object> evals = (Map<String, Object>) root.get("evals");
        Map<String, Object> subs = (Map<String, Object>) root.get("subs");
        Map<String, Rfp> rfpByMockId = new HashMap<>();
        Map<String, RfpItem> itemByMockId = new HashMap<>();
        int itemCount = 0;
        for (Map<String, Object> r : (List<Map<String, Object>>) root.get("rfps")) {
            Project project = projectRepository.findByProjectCodeAndDeletedYn(str(r, "projectId"), "N").orElse(null);
            if (project == null) {
                continue;
            }
            Rfp rfp = rfpRepository.saveAndFlush(Rfp.create().withRfpCode(str(r, "id")).withProjectId(project.getId()).withTitle(str(r, "title"))
                    .withRoundCode(str(r, "round")).withReceivedDate(date(r, "receivedDate")).withDueDate(date(r, "dueDate")));
            rfpByMockId.put(str(r, "id"), rfp);
            int order = 0;
            for (Map<String, Object> i : (List<Map<String, Object>>) r.get("items")) {
                RfpItem item = rfpItemRepository.save(RfpItem.create().withRfpId(rfp.getId()).withItemCode(str(i, "code"))
                        .withCategoryCode(str(i, "category")).withArea(str(i, "area")).withItemName(str(i, "name")).withContent(str(i, "content"))
                        .withSourcePage(i.get("page") == null ? null : Integer.valueOf(str(i, "page")))
                        .withMandatoryYn(Boolean.TRUE.equals(i.get("mandatory")) ? "Y" : "N").withSortOrder(order++));
                itemByMockId.put(str(i, "id"), item);
                itemCount++;
            }
            for (Object name : (List<Object>) r.get("evaluators")) {
                User u = users.get(name.toString());
                boolean submitted = Boolean.TRUE.equals(subs.get(str(r, "id") + "|" + name));
                rfpEvaluatorRepository.save(RfpEvaluator.create().withRfpId(rfp.getId()).withEvaluatorUserId(u.getId())
                        .withSubmittedYn(submitted ? "Y" : "N").withSubmittedAt(submitted ? LocalDateTime.now() : null));
            }
        }
        for (Map.Entry<String, Object> e : evals.entrySet()) {
            String[] k = e.getKey().split("[|]");
            RfpItem item = itemByMockId.get(k[1]);
            User u = users.get(k[2]);
            if (item == null || u == null) {
                continue;
            }
            Map<String, Object> v = (Map<String, Object>) e.getValue();
            rfpEvalRepository.save(RfpEval.create().withRfpItemId(item.getId()).withEvaluatorUserId(u.getId()).withKindCode(str(v, "kind"))
                    .withImplCode(str(v, "impl")).withDifficultyCode(str(v, "difficulty")).withMd(num(v, "md")).withCommentText(str(v, "comment")));
        }
        int proposals = 0;
        for (Map<String, Object> p : (List<Map<String, Object>>) root.get("proposals")) {
            Rfp rfp = rfpByMockId.get(str(p, "rfpId"));
            if (rfp == null) {
                continue;
            }
            Proposal proposal = proposalRepository.saveAndFlush(Proposal.create().withProposalCode(str(p, "id")).withProjectId(rfp.getProjectId())
                    .withRfpId(rfp.getId()).withTitle(str(p, "title")).withStatusCode(str(p, "status"))
                    .withOwnerUserId(users.get(str(p, "owner")).getId()).withDueDate(date(p, "dueDate"))
                    .withSubmittedDate(date(p, "submittedDate")).withPrice(p.get("price") == null ? null : Long.valueOf(str(p, "price")))
                    .withCostEstimate(p.get("costEstimate") == null ? null : Long.valueOf(str(p, "costEstimate"))));
            List<Map<String, Object>> versions = (List<Map<String, Object>>) p.get("versions");
            for (int i = 0; i < versions.size(); i++) {
                Map<String, Object> v = versions.get(i);
                proposalVersionRepository.save(ProposalVersion.create().withProposalId(proposal.getId()).withVer(str(v, "ver"))
                        .withVersionDate(date(v, "date")).withAuthorUserId(users.get(str(v, "author")).getId()).withSummary(str(v, "summary"))
                        .withFileName(str(v, "fileName")).withFileSize(str(v, "fileSize"))
                        .withSubmittedYn("제출 완료".equals(str(p, "status")) && i == versions.size() - 1 ? "Y" : "N"));
            }
            for (Map.Entry<String, Object> m : ((Map<String, Object>) p.get("mapping")).entrySet()) {
                RfpItem item = itemByMockId.get(m.getKey());
                if (item == null) {
                    continue;
                }
                Map<String, Object> v = (Map<String, Object>) m.getValue();
                User owner = str(v, "owner") == null ? null : users.get(str(v, "owner"));
                proposalMappingRepository.save(ProposalMapping.create().withProposalId(proposal.getId()).withRfpItemId(item.getId())
                        .withSection(str(v, "section")).withResponseCode(str(v, "response")).withSummary(str(v, "summary"))
                        .withOwnerUserId(owner == null ? null : owner.getId()));
            }
            int order = 0;
            for (Map<String, Object> c : (List<Map<String, Object>>) p.get("checklist")) {
                proposalChecklistRepository.save(ProposalChecklist.create().withProposalId(proposal.getId()).withItemName(str(c, "name"))
                        .withDoneYn(Boolean.TRUE.equals(c.get("done")) ? "Y" : "N").withSortOrder(order++));
            }
            proposals++;
        }
        log.info("[PmsDevDataSeeder] 샘플 RFP {}건(항목 {}건)·평가 {}건·제안서 {}건 생성", rfpByMockId.size(), itemCount, evals.size(), proposals);
    }
}
