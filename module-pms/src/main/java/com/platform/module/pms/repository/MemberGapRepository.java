package com.platform.module.pms.repository;

import com.platform.module.pms.domain.MemberGap;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberGapRepository extends JpaRepository<MemberGap, String> {
    Optional<MemberGap> findByIdAndDeletedYn(String id, String deletedYn);

    List<MemberGap> findAllByMemberIdIn(Collection<String> memberIds);

    void deleteAllByMemberIdIn(Collection<String> memberIds);
}