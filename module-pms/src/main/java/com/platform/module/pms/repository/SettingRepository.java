package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Setting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, String> {
    Optional<Setting> findByIdAndDeletedYn(String id, String deletedYn);
}
