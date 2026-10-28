package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Setting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, String> {
    Optional<Setting> findByIdAndDeletedYn(String id, String deletedYn);

    List<Setting> findAllByDeletedYn(String deletedYn);

    Optional<Setting> findBySettingKeyAndDeletedYn(String settingKey, String deletedYn);
}
