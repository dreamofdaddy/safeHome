package com.safehome.backend.domain.registry;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.registry.comparison.RegistryRightChangeType;
import com.safehome.backend.domain.registry.comparison.RegistryRightComparator;
import com.safehome.backend.domain.registry.dto.RegistryRightComparisonResponse;
import com.safehome.backend.domain.registry.dto.RegistrySnapshotComparisonResponse;
import com.safehome.backend.domain.registry.dto.RegistrySnapshotResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class RegistrySnapshotComparisonService {

    private final PropertyRepository propertyRepository;
    private final RegistrySnapshotRepository registrySnapshotRepository;
    private final RegistryRightRepository registryRightRepository;
    private final RegistryRightComparator registryRightComparator;

    public RegistrySnapshotComparisonService(
            PropertyRepository propertyRepository,
            RegistrySnapshotRepository registrySnapshotRepository,
            RegistryRightRepository registryRightRepository,
            RegistryRightComparator registryRightComparator
    ) {
        this.propertyRepository = propertyRepository;
        this.registrySnapshotRepository = registrySnapshotRepository;
        this.registryRightRepository = registryRightRepository;
        this.registryRightComparator = registryRightComparator;
    }

    public RegistrySnapshotComparisonResponse compareLatest(
            Long propertyId
    ) {
        validateProperty(propertyId);

        List<RegistrySnapshot> snapshots =
                registrySnapshotRepository
                        .findTop2ByPropertyIdOrderByObservedAtDescIdDesc(
                                propertyId
                        );

        if (snapshots.size() < 2) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        RegistrySnapshot current = snapshots.get(0);
        RegistrySnapshot previous = snapshots.get(1);

        return compareSnapshots(
                propertyId,
                previous,
                current
        );
    }

    public RegistrySnapshotComparisonResponse compare(
            Long propertyId,
            Long previousSnapshotId,
            Long currentSnapshotId
    ) {
        validateProperty(propertyId);

        RegistrySnapshot previous =
                registrySnapshotRepository.findById(previousSnapshotId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.RESOURCE_NOT_FOUND
                                )
                        );

        RegistrySnapshot current =
                registrySnapshotRepository.findById(currentSnapshotId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.RESOURCE_NOT_FOUND
                                )
                        );

        if (!propertyId.equals(previous.getProperty().getId())
                || !propertyId.equals(current.getProperty().getId())) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        return compareSnapshots(
                propertyId,
                previous,
                current
        );
    }

    private RegistrySnapshotComparisonResponse compareSnapshots(
            Long propertyId,
            RegistrySnapshot previous,
            RegistrySnapshot current
    ) {
        List<RegistryRight> previousRights =
                registryRightRepository
                        .findByRegistrySnapshotId(
                                previous.getId()
                        );

        List<RegistryRight> currentRights =
                registryRightRepository
                        .findByRegistrySnapshotId(
                                current.getId()
                        );

        List<RegistryRightComparisonResponse> rightChanges =
                registryRightComparator.compare(
                        previousRights,
                        currentRights
                );

        boolean sourceChanged =
                !Objects.equals(
                        previous.getSource(),
                        current.getSource()
                );

        boolean documentChanged =
                !Objects.equals(
                        previous.getDocumentHash(),
                        current.getDocumentHash()
                );

        boolean rightsChanged =
                rightChanges.stream()
                        .anyMatch(change ->
                                change.changeType()
                                        != RegistryRightChangeType.UNCHANGED
                        );

        return new RegistrySnapshotComparisonResponse(
                propertyId,
                RegistrySnapshotResponse.from(previous),
                RegistrySnapshotResponse.from(current),
                sourceChanged,
                documentChanged,
                rightsChanged,
                rightChanges
        );
    }

    private void validateProperty(Long propertyId) {
        if (propertyId == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        propertyRepository.findById(propertyId)
                .orElseThrow(() ->
                        new PropertyNotFoundException(propertyId)
                );
    }
}