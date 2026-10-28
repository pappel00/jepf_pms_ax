package com.platform.module.pms.repository;

import com.platform.module.pms.domain.WbsTask;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WbsTaskRepository extends JpaRepository<WbsTask, String> {
    Optional<WbsTask> findByIdAndDeletedYn(String id, String deletedYn);

    boolean existsByProjectIdAndDeletedYn(String projectId, String deletedYn);

    List<WbsTask> findAllByProjectIdAndDeletedYnOrderBySortOrderAscCreatedDateAsc(String projectId, String deletedYn);

    @Query("select t from WbsTask t where t.deletedYn = 'N' and (:projectId = '' or t.projectId = :projectId) "
            + "order by t.projectId, t.sortOrder, t.createdDate")
    List<WbsTask> search(@Param("projectId") String projectId);
}
