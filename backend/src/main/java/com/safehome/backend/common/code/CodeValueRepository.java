package com.safehome.backend.common.code;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeValueRepository extends JpaRepository<CodeValue, Long> {

    List<CodeValue> findByGroupIdOrderBySortOrderAscIdAsc(Long groupId);

    List<CodeValue> findByGroupIdAndUseYnOrderBySortOrderAscIdAsc(
            Long groupId,
            String useYn
    );

    Optional<CodeValue> findByGroupIdAndCode(Long groupId, String code);

    boolean existsByGroupIdAndCode(Long groupId, String code);
}