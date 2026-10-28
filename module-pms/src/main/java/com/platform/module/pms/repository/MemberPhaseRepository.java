package com.platform.module.pms.repository;

import com.platform.module.pms.domain.MemberPhase;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberPhaseRepository extends JpaRepository<MemberPhase, String> {
    Optional<MemberPhase> findByIdAndDeletedYn(String id, String deletedYn);

    List<MemberPhase> findAllByMemberIdIn(Collection<String> memberIds);

    void deleteAllByMemberIdIn(Collection<String> memberIds);
}