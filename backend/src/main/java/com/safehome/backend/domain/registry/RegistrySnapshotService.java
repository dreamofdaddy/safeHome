package com.safehome.backend.domain.registry;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.domain.property.Property;
import com.safehome.backend.domain.property.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RegistrySnapshotService {

    private final CodeService codeService;
    private final RegistrySnapshotRepository registrySnapshotRepository;
    private final PropertyRepository propertyRepository;

    public RegistrySnapshotService(
            RegistrySnapshotRepository registrySnapshotRepository,
            PropertyRepository propertyRepository,
            CodeService codeService
    ) {
        this.registrySnapshotRepository = registrySnapshotRepository;
        this.propertyRepository = propertyRepository;
        this.codeService = codeService;
    }

    public List<RegistrySnapshot> findAll() {
        return registrySnapshotRepository.findAll();
    }

    public RegistrySnapshot findById(Long id) {
        return registrySnapshotRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                );
    }

    public List<RegistrySnapshot> findByPropertyId(Long propertyId) {
        return registrySnapshotRepository.findByPropertyId(propertyId);
    }

    public RegistrySnapshot findLatestByPropertyId(Long propertyId) {
        return registrySnapshotRepository
                .findTopByPropertyIdOrderByObservedAtDesc(propertyId)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                );
    }

    @Transactional
    public RegistrySnapshot create(
            Long propertyId,
            String source,
            LocalDateTime observedAt,
            String documentHash
    ) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() ->
                        new PropertyNotFoundException(propertyId)
                );

        validate(source, observedAt);

        RegistrySnapshot snapshot = RegistrySnapshot.create(
                property,
                source,
                observedAt,
                documentHash
        );

        return registrySnapshotRepository.save(snapshot);
    }

    private void validate(
            String source,
            LocalDateTime observedAt
    ) {
        if (source == null || source.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        codeService.getRequiredActiveCode(
                "REGISTRY_SOURCE",
                source
        );

        if (observedAt == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}