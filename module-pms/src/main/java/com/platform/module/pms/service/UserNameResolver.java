package com.platform.module.pms.service;

import com.platform.module.user.domain.User;
import com.platform.module.user.repository.UserRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 사용자 ID → 이름 변환(응답에 처리자 이름을 같이 내려주기 위함). */
@Component
class UserNameResolver {

    private final UserRepository userRepository;

    UserNameResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    Map<String, String> resolve(Collection<String> ids) {
        Set<String> clean = ids.stream().filter(i -> i != null && !i.isBlank()).collect(Collectors.toSet());
        Map<String, String> names = new HashMap<>();
        if (clean.isEmpty()) {
            return names;
        }
        for (User u : userRepository.findAllById(clean)) {
            names.put(u.getId(), u.getUserName());
        }
        return names;
    }
}