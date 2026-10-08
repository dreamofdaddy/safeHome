package com.safehome.backend.common.code;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeGroupRepository extends JpaRepository<CodeGroup, Long> {

    Optional<CodeGroup> findByGroupCode(String groupCode);

    boolean existsByGroupCode(String groupCode);

    List<CodeGroup> findByUseYnOrderBySortOrderAscIdAsc(String useYn);
}