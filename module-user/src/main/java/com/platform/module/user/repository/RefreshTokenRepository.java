package com.platform.module.user.repository;

import com.platform.module.user.domain.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHashAndRevokedYn(String tokenHash, String revokedYn);
}
