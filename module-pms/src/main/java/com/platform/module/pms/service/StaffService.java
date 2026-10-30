package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.module.pms.dto.StaffDto;
import com.platform.module.pms.dto.UserProfileDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.user.domain.User;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.domain.UserGroupUser;
import com.platform.module.user.dto.UserDto;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupUserRepository;
import com.platform.module.user.repository.UserRepository;
import com.platform.module.user.service.UserService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인력 관리 — PM 도 계정을 만들고 PMS 역할(사용자그룹)·소속·등급을 정할 수 있게 한다. 프레임워크의 사용자/그룹 API 는
 * ADMIN 전용이므로, PMS 가 관리하는 그룹(영업·PM·수행 인력·협력사·운영·센터장)에 한해 여기서 대신 처리한다.
 * 센터장(PMS_MANAGER) 그룹은 승인 권한이 딸려 있어 센터장·관리자만 부여/회수할 수 있다.
 */
@Service
@Transactional(readOnly = true)
public class StaffService {

    static final String MANAGER_GROUP = "PMS_MANAGER";
    static final List<String> PMS_GROUPS = List.of("PMS_SALES", "PMS_PM", "PMS_MEMBER", "PMS_PARTNER", "PMS_OPS", MANAGER_GROUP);
    private static final Set<String> ORG_TYPES = Set.of("자사", "협력사");

    private final UserService userService;
    private final UserProfileService profileService;
    private final UserRepository userRepository;
    private final UserGroupRepository groupRepository;
    private final UserGroupUserRepository groupUserRepository;

    public StaffService(UserService userService, UserProfileService profileService, UserRepository userRepository,
                        UserGroupRepository groupRepository, UserGroupUserRepository groupUserRepository) {
        this.userService = userService;
        this.profileService = profileService;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupUserRepository = groupUserRepository;
    }

    public List<StaffDto.Response> getList() {
        Map<String, List<String>> groupsByUser = new HashMap<>();
        for (String code : PMS_GROUPS) {
            groupRepository.findByGroupCodeAndDeletedYn(code, "N").ifPresent(g ->
                    groupUserRepository.findAllByUserGroupId(g.getId())
                            .forEach(m -> groupsByUser.computeIfAbsent(m.getUserId(), k -> new ArrayList<>()).add(code)));
        }
        Map<String, User> users = new HashMap<>();
        userRepository.findAll().stream().filter(u -> "N".equals(u.getDeletedYn())).forEach(u -> users.put(u.getId(), u));
        return profileService.getList().stream()
                .filter(p -> users.containsKey(p.userId()))
                .map(p -> toResponse(users.get(p.userId()), p, groupsByUser.getOrDefault(p.userId(), List.of())))
                .toList();
    }

    @Transactional
    public StaffDto.Response create(StaffDto.CreateRequest r) {
        checkOrg(r.orgTypeCode(), r.partnerCompany());
        List<String> wanted = checkGroups(r.groupCodes());
        UserDto.Response created = userService.create(new UserDto.CreateRequest(
                r.loginId().trim(), r.password(), PmsSupport.blankToNull(r.email()), r.userName().trim(), null));
        applyGroups(created.id(), wanted);
        var profile = profileService.upsert(created.id(), new UserProfileDto.Request(
                r.orgTypeCode(), r.partnerCompany(), r.notifyChannelCode(), r.telegramChatId(), r.mattermostUserId(), r.jobGradeCode()));
        return toResponse(userRepository.findById(created.id()).orElseThrow(), profile, groupsOf(created.id()));
    }

    @Transactional
    public StaffDto.Response update(String userId, StaffDto.UpdateRequest r) {
        checkOrg(r.orgTypeCode(), r.partnerCompany());
        List<String> wanted = checkGroups(r.groupCodes());
        UserDto.Response cur = userService.getById(userId);
        userService.update(userId, new UserDto.UpdateRequest(
                PmsSupport.blankToNull(r.email()), r.userName().trim(), cur.departmentId(), r.useYn()));
        applyGroups(userId, wanted);
        var profile = profileService.upsert(userId, new UserProfileDto.Request(
                r.orgTypeCode(), r.partnerCompany(), r.notifyChannelCode(), r.telegramChatId(), r.mattermostUserId(), r.jobGradeCode()));
        return toResponse(userRepository.findById(userId).orElseThrow(), profile, groupsOf(userId));
    }

    private void checkOrg(String orgType, String partnerCompany) {
        if (!ORG_TYPES.contains(orgType)) {
            throw new BusinessException(PmsErrorCode.STAFF_INVALID, "소속 구분은 자사 또는 협력사여야 합니다.");
        }
        if ("협력사".equals(orgType) && PmsSupport.blankToNull(partnerCompany) == null) {
            throw new BusinessException(PmsErrorCode.STAFF_INVALID, "협력사 인력은 협력사명을 입력해야 합니다.");
        }
    }

    private List<String> checkGroups(List<String> codes) {
        List<String> wanted = codes == null ? List.of() : codes.stream().distinct().toList();
        for (String c : wanted) {
            if (!PMS_GROUPS.contains(c)) {
                throw new BusinessException(PmsErrorCode.STAFF_INVALID, "지정할 수 없는 역할입니다: " + c);
            }
        }
        return wanted;
    }

    private boolean canGrantManager() {
        return PmsSupport.hasAnyAuthority("ROLE_PMS_MANAGER", "ROLE_ADMIN", "ROLE_SYSTEM_ADMIN");
    }

    /** PMS 그룹만 맞춘다. 센터장 그룹은 권한이 없으면 바꿀 수 없다. 그 밖의 그룹(기본 권한·관리자)은 건드리지 않는다. */
    private void applyGroups(String userId, List<String> wanted) {
        Set<String> has = new HashSet<>(groupsOf(userId));
        for (String code : PMS_GROUPS) {
            if (MANAGER_GROUP.equals(code) && !canGrantManager()) {
                if (wanted.contains(code) != has.contains(code)) {
                    throw new BusinessException(PmsErrorCode.STAFF_INVALID, "센터장 역할은 센터장·관리자만 부여하거나 해제할 수 있습니다.");
                }
                continue;
            }
            UserGroup group = groupRepository.findByGroupCodeAndDeletedYn(code, "N").orElse(null);
            if (group == null) {
                continue;
            }
            boolean want = wanted.contains(code);
            if (want && !has.contains(code)) {
                groupUserRepository.saveAndFlush(UserGroupUser.create(group.getId(), userId));
            } else if (!want && has.contains(code)) {
                groupUserRepository.findByUserGroupIdAndUserId(group.getId(), userId).ifPresent(groupUserRepository::delete);
            }
        }
        groupUserRepository.flush();
    }

    private List<String> groupsOf(String userId) {
        Map<String, String> codeById = new HashMap<>();
        for (String code : PMS_GROUPS) {
            groupRepository.findByGroupCodeAndDeletedYn(code, "N").ifPresent(g -> codeById.put(g.getId(), code));
        }
        return groupUserRepository.findAllByUserId(userId).stream()
                .map(m -> codeById.get(m.getUserGroupId()))
                .filter(c -> c != null)
                .toList();
    }

    private StaffDto.Response toResponse(User u, UserProfileDto.Response p, List<String> groups) {
        return new StaffDto.Response(u.getId(), u.getLoginId(), u.getUserName(), u.getEmail(), u.getUseYn(), p.orgTypeCode(),
                p.partnerCompany(), p.notifyChannelCode(), p.telegramChatId(), p.mattermostUserId(), p.jobGradeCode(), groups);
    }
}
