package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.pms.domain.UserProfile;
import com.platform.module.pms.dto.UserProfileDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.UserProfileRepository;
import com.platform.module.user.domain.User;
import com.platform.module.user.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PMS 사용자 확장 프로필(소속 구분·협력사·메신저 매핑·인력 등급). usr_user 와 1:1 이며 프로필이 없는 사용자는
 * 기본값(자사)으로 응답한다.
 */
@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;

    public UserProfileService(UserProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    /** 활성 사용자 전체 + 각자의 프로필(없으면 기본값). */
    public List<UserProfileDto.Response> getList() {
        Map<String, UserProfile> byUser = new HashMap<>();
        profileRepository.findAllByDeletedYn("N").forEach(p -> byUser.put(p.getUserId(), p));
        return userRepository.findAll().stream()
                .filter(u -> "N".equals(u.getDeletedYn()))
                .map(u -> UserProfileDto.Response.of(u.getId(), u.getLoginId(), u.getUserName(), byUser.get(u.getId())))
                .toList();
    }

    public UserProfileDto.Response get(String userId) {
        User user = findUserOrThrow(userId);
        return UserProfileDto.Response.of(user.getId(), user.getLoginId(), user.getUserName(),
                profileRepository.findByUserIdAndDeletedYn(userId, "N").orElse(null));
    }

    @Transactional
    public UserProfileDto.Response upsert(String userId, UserProfileDto.Request request) {
        User user = findUserOrThrow(userId);
        UserProfile profile = profileRepository.findByUserIdAndDeletedYn(userId, "N")
                .orElseGet(() -> UserProfile.create().withUserId(userId));
        profile.withOrgTypeCode(request.orgTypeCode())
                .withPartnerCompany(PmsSupport.blankToNull(request.partnerCompany()))
                .withNotifyChannelCode(PmsSupport.blankToNull(request.notifyChannelCode()))
                .withTelegramChatId(PmsSupport.blankToNull(request.telegramChatId()))
                .withMattermostUserId(PmsSupport.blankToNull(request.mattermostUserId()))
                .withJobGradeCode(PmsSupport.blankToNull(request.jobGradeCode()));
        UserProfile saved = profileRepository.saveAndFlush(profile);
        return UserProfileDto.Response.of(user.getId(), user.getLoginId(), user.getUserName(), saved);
    }

    private User findUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .filter(u -> "N".equals(u.getDeletedYn()))
                .orElseThrow(() -> new TenantException(PmsErrorCode.USER_NOT_FOUND, "존재하지 않는 사용자입니다: " + userId));
    }
}
