package com.platform.module.file.repository;

import com.platform.module.file.domain.FileGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileGroupRepository extends JpaRepository<FileGroup, String> {
    boolean existsByGroupCode(String groupCode);
    Optional<FileGroup> findByIdAndDeletedYn(String id, String deletedYn);

    // 목록 조회는 JpaRepository가 기본 제공하는 findAll(Pageable)을 그대로 쓴다 — 관리자 화면은
    // 삭제된 항목도 상태(deletedYn)와 함께 보여줘야 하므로 deletedYn으로 걸러내지 않는다.
}
