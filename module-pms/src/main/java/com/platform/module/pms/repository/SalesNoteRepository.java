package com.platform.module.pms.repository;

import com.platform.module.pms.domain.SalesNote;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesNoteRepository extends JpaRepository<SalesNote, String> {
    Optional<SalesNote> findByIdAndDeletedYn(String id, String deletedYn);
}
