package com.safehome.backend.domain.registry;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RegistryRightService {

    private final RegistryRightRepository registryRightRepository;
    private final RegistrySnapshotRepository registrySnapshotRepository;
    private final CodeService codeService;

    public RegistryRightService(
            RegistryRightRepository registryRightRepository,
            RegistrySnapshotRepository registrySnapshotRepository,
            CodeService codeService
    ) {
        this.registryRightRepository = registryRightRepository;
        this.registrySnapshotRepository = registrySnapshotRepository;
        this.codeService = codeService;
    }

    public List<RegistryRight> findAll() {
        return registryRightRepository.findAll();
    }

    public RegistryRight findById(Long id) {
        return registryRightRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                );
    }

    public List<RegistryRight> findBySnapshotId(
            Long registrySnapshotId
    ) {
        return registryRightRepository
                .findByRegistrySnapshotId(registrySnapshotId);
    }

    public List<RegistryRight> findActiveBySnapshotId(
        Long registrySnapshotId
    ) {
        String activeStatus =
                codeService.getRequiredActiveCode(
                        CommonCodes.REGISTRY_RIGHT_STATUS,
                        CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
                );

        return registryRightRepository
                .findByRegistrySnapshotIdAndStatus(
                        registrySnapshotId,
                        activeStatus
                );
    }

    @Transactional
    public RegistryRight create(
            Long registrySnapshotId,
            String rightType,
            String holderName,
            BigDecimal amount,
            Integer priority,
            java.time.LocalDateTime registeredAt
    ) {
        RegistrySnapshot snapshot =
                registrySnapshotRepository.findById(registrySnapshotId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.RESOURCE_NOT_FOUND
                                )
                        );

        validate(
                rightType,
                amount,
                priority
        );

        String activeStatus =
                codeService.getRequiredActiveCode(
                        CommonCodes.REGISTRY_RIGHT_STATUS,
                        CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
                );

        RegistryRight registryRight =
                RegistryRight.create(
                        snapshot,
                        rightType,
                        holderName,
                        amount,
                        priority,
                        registeredAt,
                        activeStatus
                );

        return registryRightRepository.save(registryRight);
    }

    @Transactional
    public void deactivate(Long id) {
        RegistryRight registryRight = findById(id);

        String inactiveStatus =
            codeService.getRequiredActiveCode(
                    CommonCodes.REGISTRY_RIGHT_STATUS,
                    CommonCodes.REGISTRY_RIGHT_STATUS_INACTIVE
            );

        registryRight.deactivate(inactiveStatus);
    }

    private void validate(
            String rightType,
            BigDecimal amount,
            Integer priority
    ) {
        if (rightType == null || rightType.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        validateRightType(rightType);

        if (amount != null
                && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (priority != null && priority <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateRightType(String rightType) {
        if (!codeService.isActive(CommonCodes.REGISTRY_RIGHT_TYPE, rightType)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}