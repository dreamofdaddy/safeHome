package com.safehome.backend.common.code;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CodeService {

    private final CodeGroupRepository codeGroupRepository;
    private final CodeValueRepository codeValueRepository;

    public CodeService(
            CodeGroupRepository codeGroupRepository,
            CodeValueRepository codeValueRepository
    ) {
        this.codeGroupRepository = codeGroupRepository;
        this.codeValueRepository = codeValueRepository;
    }

    /**
     * 코드 그룹 조회
     */
    public Optional<CodeGroup> findGroup(String groupCode) {
        return codeGroupRepository.findByGroupCode(groupCode);
    }

    /**
     * 코드 그룹 조회
     *
     * 존재하지 않는 그룹은 Optional.empty()를 반환한다.
     */
    public Optional<CodeGroup> findActiveGroup(String groupCode) {
        return codeGroupRepository.findByGroupCode(groupCode)
                .filter(group -> "Y".equals(group.getUseYn()));
    }

    /**
     * 특정 코드 그룹의 전체 코드값 조회
     */
    public List<CodeValue> findValues(String groupCode) {
        return codeGroupRepository.findByGroupCode(groupCode)
                .map(group ->
                        codeValueRepository.findByGroupIdOrderBySortOrderAscIdAsc(
                                group.getId()
                        )
                )
                .orElseGet(List::of);
    }

    /**
     * 특정 코드 그룹의 사용 중인 코드값만 조회
     */
    public List<CodeValue> findActiveValues(String groupCode) {
        return codeGroupRepository.findByGroupCode(groupCode)
                .filter(group -> "Y".equals(group.getUseYn()))
                .map(group ->
                        codeValueRepository
                                .findByGroupIdAndUseYnOrderBySortOrderAscIdAsc(
                                        group.getId(),
                                        "Y"
                                )
                )
                .orElseGet(List::of);
    }

    public List<CodeGroup> findActiveGroups() {
        return codeGroupRepository.findByUseYnOrderBySortOrderAscIdAsc("Y");
    }

    /**
     * 특정 그룹의 특정 코드 조회
     */
    public Optional<CodeValue> findValue(
            String groupCode,
            String code
    ) {
        return codeGroupRepository.findByGroupCode(groupCode)
                .flatMap(group ->
                        codeValueRepository.findByGroupIdAndCode(
                                group.getId(),
                                code
                        )
                );
    }

    /**
     * 특정 그룹의 특정 코드가 존재하는지 확인
     */
    public boolean exists(
            String groupCode,
            String code
    ) {
        return codeGroupRepository.findByGroupCode(groupCode)
                .map(group ->
                        codeValueRepository.existsByGroupIdAndCode(
                                group.getId(),
                                code
                        )
                )
                .orElse(false);
    }

    /**
     * 특정 그룹의 특정 코드가 현재 사용 가능한지 확인
     */
    public boolean isActive(
            String groupCode,
            String code
    ) {
        return findValue(groupCode, code)
                .map(value -> "Y".equals(value.getUseYn()))
                .orElse(false);
    }

    public String getRequiredActiveCode(
            String groupCode,
            String code
    ) {
        return findValue(groupCode, code)
                .filter(value -> "Y".equals(value.getUseYn()))
                .map(value -> value.getCode())
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.INVALID_REQUEST)
                );
    }
}