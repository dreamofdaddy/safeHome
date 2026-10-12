package com.safehome.backend.domain.risk.comparison;

import com.safehome.backend.domain.risk.RiskFactor;
import com.safehome.backend.domain.risk.dto.RiskFactorComparisonResponse;
import com.safehome.backend.domain.risk.dto.RiskFactorResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class RiskFactorComparator {

    public List<RiskFactorComparisonResponse> compare(
            List<RiskFactor> previousFactors,
            List<RiskFactor> currentFactors
    ) {
        List<RiskFactor> unmatchedPrevious =
                new ArrayList<>(previousFactors);

        List<RiskFactor> unmatchedCurrent =
                new ArrayList<>(currentFactors);

        List<RiskFactorComparisonResponse> results =
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
            List<RiskFactor> previousFactors,
            List<RiskFactor> currentFactors,
            List<RiskFactorComparisonResponse> results
    ) {
        List<RiskFactor> matchedPrevious = new ArrayList<>();
        List<RiskFactor> matchedCurrent = new ArrayList<>();

        for (RiskFactor previous : previousFactors) {

            RiskFactor current = currentFactors.stream()
                    .filter(candidate ->
                            !matchedCurrent.contains(candidate))
                    .filter(candidate ->
                            sameFactor(previous, candidate))
                    .findFirst()
                    .orElse(null);

            if (current == null) {
                continue;
            }

            matchedPrevious.add(previous);
            matchedCurrent.add(current);

            results.add(
                    new RiskFactorComparisonResponse(
                            RiskFactorChangeType.UNCHANGED,
                            RiskFactorResponse.from(previous),
                            RiskFactorResponse.from(current)
                    )
            );
        }

        previousFactors.removeAll(matchedPrevious);
        currentFactors.removeAll(matchedCurrent);
    }

    private void matchChanged(
            List<RiskFactor> previousFactors,
            List<RiskFactor> currentFactors,
            List<RiskFactorComparisonResponse> results
    ) {
        List<RiskFactor> matchedPrevious = new ArrayList<>();
        List<RiskFactor> matchedCurrent = new ArrayList<>();

        for (RiskFactor previous : previousFactors) {

            RiskFactor current = currentFactors.stream()
                    .filter(candidate ->
                            !matchedCurrent.contains(candidate))
                    .filter(candidate ->
                            Objects.equals(
                                    previous.getFactorType(),
                                    candidate.getFactorType()
                            ))
                    .findFirst()
                    .orElse(null);

            if (current == null) {
                continue;
            }

            matchedPrevious.add(previous);
            matchedCurrent.add(current);

            results.add(
                    new RiskFactorComparisonResponse(
                            RiskFactorChangeType.CHANGED,
                            RiskFactorResponse.from(previous),
                            RiskFactorResponse.from(current)
                    )
            );
        }

        previousFactors.removeAll(matchedPrevious);
        currentFactors.removeAll(matchedCurrent);
    }

    private void addRemoved(
            List<RiskFactor> previousFactors,
            List<RiskFactorComparisonResponse> results
    ) {
        previousFactors.forEach(previous ->
                results.add(
                        new RiskFactorComparisonResponse(
                                RiskFactorChangeType.REMOVED,
                                RiskFactorResponse.from(previous),
                                null
                        )
                )
        );
    }

    private void addAdded(
            List<RiskFactor> currentFactors,
            List<RiskFactorComparisonResponse> results
    ) {
        currentFactors.forEach(current ->
                results.add(
                        new RiskFactorComparisonResponse(
                                RiskFactorChangeType.ADDED,
                                null,
                                RiskFactorResponse.from(current)
                        )
                )
        );
    }

    private boolean sameFactor(
            RiskFactor left,
            RiskFactor right
    ) {
        return Objects.equals(
                    left.getFactorType(),
                    right.getFactorType()
                )
                && Objects.equals(
                    left.getSeverity(),
                    right.getSeverity()
                )
                && sameBigDecimal(
                    left.getScore(),
                    right.getScore()
                )
                && Objects.equals(
                    left.getValue(),
                    right.getValue()
                )
                && Objects.equals(
                    left.getDescription(),
                    right.getDescription()
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

    private Comparator<RiskFactorComparisonResponse>
    comparisonOrder() {

        return Comparator
                .comparing(
                        (RiskFactorComparisonResponse response) ->
                                factorType(response),
                        Comparator.nullsLast(String::compareTo)
                )
                .thenComparing(
                        response ->
                                response.changeType().ordinal()
                );
    }

    private String factorType(
            RiskFactorComparisonResponse response
    ) {
        if (response.current() != null) {
            return response.current().factorType();
        }

        if (response.previous() != null) {
            return response.previous().factorType();
        }

        return null;
    }
}