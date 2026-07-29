package com.platform.module.file.repository;

import com.platform.module.file.domain.FileItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileItemRepository extends JpaRepository<FileItem, String> {
    List<FileItem> findAllByGroupIdOrderBySortOrderAsc(String groupId);
    Optional<FileItem> findByIdAndGroupId(String id, String groupId);
}
