package com.safehome.backend.domain.registry.comparison;

import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.registry.dto.RegistryRightComparisonResponse;
import com.safehome.backend.domain.registry.dto.RegistryRightSnapshotResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class RegistryRightComparator {

    public List<RegistryRightComparisonResponse> compare(
            List<RegistryRight> previousRights,
            List<RegistryRight> currentRights
    ) {
        List<RegistryRight> unmatchedPrevious =
                new ArrayList<>(previousRights);

        List<RegistryRight> unmatchedCurrent =
                new ArrayList<>(currentRights);

        List<RegistryRightComparisonResponse> results =
                new ArrayList<>();

        matchUnchanged(
                unmatchedPrevious,
                unmatchedCurrent,
                results
        );

        matchChanged(
                unmatchedPrevious,
                unmatchedCurrent,
                results
        );

        addRemoved(
                unmatchedPrevious,
                results
        );

        addAdded(
                unmatchedCurrent,
                results
        );

        return results.stream()
                .sorted(comparisonOrder())
                .toList();
    }

    private void matchUnchanged(
            List<RegistryRight> previousRights,
            List<RegistryRight> currentRights,
            List<RegistryRightComparisonResponse> results
    ) {
        List<RegistryRight> matchedPrevious =
                new ArrayList<>();

        List<RegistryRight> matchedCurrent =
                new ArrayList<>();

        for (RegistryRight previous : previousRights) {

            RegistryRight current = currentRights.stream()
                    .filter(candidate ->
                            !matchedCurrent.contains(candidate))
                    .filter(candidate ->
                            sameRight(previous, candidate))
                    .findFirst()
                    .orElse(null);

            if (current == null) {
                continue;
            }

            matchedPrevious.add(previous);
            matchedCurrent.add(current);

            results.add(
                    new RegistryRightComparisonResponse(
                            RegistryRightChangeType.UNCHANGED,
                            RegistryRightSnapshotResponse.from(previous),
                            RegistryRightSnapshotResponse.from(current)
                    )
            );
        }

        previousRights.removeAll(matchedPrevious);
        currentRights.removeAll(matchedCurrent);
    }

    private void matchChanged(
            List<RegistryRight> previousRights,
            List<RegistryRight> currentRights,
            List<RegistryRightComparisonResponse> results
    ) {
        List<RegistryRight> matchedPrevious =
                new ArrayList<>();

        List<RegistryRight> matchedCurrent =
                new ArrayList<>();

        for (RegistryRight previous : previousRights) {

            RegistryRight current = currentRights.stream()
                    .filter(candidate ->
                            !matchedCurrent.contains(candidate))
                    .filter(candidate ->
                            sameLogicalIdentity(
                                    previous,
                                    candidate
                            ))
                    .findFirst()
                    .orElse(null);

            if (current == null) {
                continue;
            }

            matchedPrevious.add(previous);
            matchedCurrent.add(current);

            results.add(
                    new RegistryRightComparisonResponse(
                            RegistryRightChangeType.CHANGED,
                            RegistryRightSnapshotResponse.from(previous),
                            RegistryRightSnapshotResponse.from(current)
                    )
            );
        }

        previousRights.removeAll(matchedPrevious);
        currentRights.removeAll(matchedCurrent);
    }

    private void addRemoved(
            List<RegistryRight> previousRights,
            List<RegistryRightComparisonResponse> results
    ) {
        previousRights.forEach(previous ->
                results.add(
                        new RegistryRightComparisonResponse(
                                RegistryRightChangeType.REMOVED,
                                RegistryRightSnapshotResponse.from(previous),
                                null
                        )
                )
        );
    }

    private void addAdded(
            List<RegistryRight> currentRights,
            List<RegistryRightComparisonResponse> results
    ) {
        currentRights.forEach(current ->
                results.add(
                        new RegistryRightComparisonResponse(
                                RegistryRightChangeType.ADDED,
                                null,
                                RegistryRightSnapshotResponse.from(current)
                        )
                )
        );
    }

    private boolean sameRight(
            RegistryRight left,
            RegistryRight right
    ) {
        return Objects.equals(
                    left.getRightType(),
                    right.getRightType()
                )
                && Objects.equals(
                    left.getHolderName(),
                    right.getHolderName()
                )
                && sameBigDecimal(
                    left.getAmount(),
                    right.getAmount()
                )
                && Objects.equals(
                    left.getPriority(),
                    right.getPriority()
                )
                && Objects.equals(
                    left.getRegisteredAt(),
                    right.getRegisteredAt()
                )
                && Objects.equals(
                    left.getStatus(),
                    right.getStatus()
                );
    }

    private boolean sameLogicalIdentity(
            RegistryRight left,
            RegistryRight right
    ) {
        if (left.getHolderName() == null
                || right.getHolderName() == null) {
            return false;
        }

        if (left.getRegisteredAt() == null
                || right.getRegisteredAt() == null) {
            return false;
        }

        return Objects.equals(
                    left.getHolderName(),
                    right.getHolderName()
                )
                && Objects.equals(
                    left.getRegisteredAt(),
                    right.getRegisteredAt()
                );
    }

    private boolean sameBigDecimal(
            BigDecimal left,
            BigDecimal right
    ) {
        if (left == null && right == null) {
            return true;
        }

        if (left == null || right == null) {
            return false;
        }

        return left.compareTo(right) == 0;
    }

    private Comparator<RegistryRightComparisonResponse>
    comparisonOrder() {

        return Comparator
                .comparing(
                        (RegistryRightComparisonResponse response) ->
                                rightType(response),
                        Comparator.nullsLast(String::compareTo)
                )
                .thenComparing(
                        response ->
                                response.changeType().ordinal()
                );
    }

    private String rightType(
            RegistryRightComparisonResponse response
    ) {
        if (response.current() != null) {
            return response.current().rightType();
        }

        if (response.previous() != null) {
            return response.previous().rightType();
        }

        return null;
    }
}