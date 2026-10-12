package com.safehome.backend.domain.risk.comparison;

import com.safehome.backend.domain.risk.RiskFactor;
import com.safehome.backend.domain.risk.dto.RiskFactorComparisonResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskFactorComparatorTest {

    private final RiskFactorComparator comparator =
            new RiskFactorComparator();

    @Test
    void shouldReturnUnchangedWhenFactorsAreSame() {
        RiskFactor previous = factor(
                1L,
                "MORTGAGE",
                "HIGH",
                "30.00",
                "300000000",
                "Mortgage exists"
        );

        RiskFactor current = factor(
                2L,
                "MORTGAGE",
                "HIGH",
                "30.00",
                "300000000",
                "Mortgage exists"
        );

        List<RiskFactorComparisonResponse> results =
                comparator.compare(
                        List.of(previous),
                        List.of(current)
                );

        assertThat(results).hasSize(1);

        RiskFactorComparisonResponse result = results.get(0);

        assertThat(result.changeType())
                .isEqualTo(RiskFactorChangeType.UNCHANGED);

        assertThat(result.previous()).isNotNull();
        assertThat(result.current()).isNotNull();
    }

    @Test
    void shouldReturnAddedWhenFactorExistsOnlyInCurrent() {
        RiskFactor current = factor(
                2L,
                "SEIZURE",
                "CRITICAL",
                "50.00",
                "SEIZURE",
                "Seizure exists"
        );

        List<RiskFactorComparisonResponse> results =
                comparator.compare(
                        List.of(),
                        List.of(current)
                );

        assertThat(results).hasSize(1);

        RiskFactorComparisonResponse result = results.get(0);

        assertThat(result.changeType())
                .isEqualTo(RiskFactorChangeType.ADDED);

        assertThat(result.previous()).isNull();
        assertThat(result.current()).isNotNull();
    }

    @Test
    void shouldReturnRemovedWhenFactorExistsOnlyInPrevious() {
        RiskFactor previous = factor(
                1L,
                "SEIZURE",
                "CRITICAL",
                "50.00",
                "SEIZURE",
                "Seizure exists"
        );

        List<RiskFactorComparisonResponse> results =
                comparator.compare(
                        List.of(previous),
                        List.of()
                );

        assertThat(results).hasSize(1);

        RiskFactorComparisonResponse result = results.get(0);

        assertThat(result.changeType())
                .isEqualTo(RiskFactorChangeType.REMOVED);

        assertThat(result.previous()).isNotNull();
        assertThat(result.current()).isNull();
    }

    @Test
    void shouldReturnChangedWhenSameFactorTypeHasDifferentValues() {
        RiskFactor previous = factor(
                1L,
                "MORTGAGE",
                "HIGH",
                "30.00",
                "100000000",
                "Mortgage exists"
        );

        RiskFactor current = factor(
                2L,
                "MORTGAGE",
                "HIGH",
                "30.00",
                "150000000",
                "Mortgage exists"
        );

        List<RiskFactorComparisonResponse> results =
                comparator.compare(
                        List.of(previous),
                        List.of(current)
                );

        assertThat(results).hasSize(1);

        RiskFactorComparisonResponse result = results.get(0);

        assertThat(result.changeType())
                .isEqualTo(RiskFactorChangeType.CHANGED);

        assertThat(result.previous().value())
                .isEqualTo("100000000");

        assertThat(result.current().value())
                .isEqualTo("150000000");
    }

    @Test
    void shouldMatchExactFactorBeforeChangedFactorWhenSameTypeExistsMultipleTimes() {
        RiskFactor previousChanged = factor(
                1L,
                "MORTGAGE",
                "HIGH",
                "30.00",
                "100000000",
                "Mortgage exists"
        );

        RiskFactor previousUnchanged = factor(
                1L,
                "MORTGAGE",
                "HIGH",
                "30.00",
                "300000000",
                "Mortgage exists"
        );

        RiskFactor currentChanged = factor(
                2L,
                "MORTGAGE",
                "HIGH",
                "30.00",
                "150000000",
                "Mortgage exists"
        );

        RiskFactor currentUnchanged = factor(
                2L,
                "MORTGAGE",
                "HIGH",
                "30.00",
                "300000000",
                "Mortgage exists"
        );

        List<RiskFactorComparisonResponse> results =
                comparator.compare(
                        List.of(
                                previousChanged,
                                previousUnchanged
                        ),
                        List.of(
                                currentChanged,
                                currentUnchanged
                        )
                );

        assertThat(results).hasSize(2);

        assertThat(results)
                .anySatisfy(result -> {
                    assertThat(result.changeType())
                            .isEqualTo(
                                    RiskFactorChangeType.UNCHANGED
                            );

                    assertThat(result.previous().value())
                            .isEqualTo("300000000");

                    assertThat(result.current().value())
                            .isEqualTo("300000000");
                });

        assertThat(results)
                .anySatisfy(result -> {
                    assertThat(result.changeType())
                            .isEqualTo(
                                    RiskFactorChangeType.CHANGED
                            );

                    assertThat(result.previous().value())
                            .isEqualTo("100000000");

                    assertThat(result.current().value())
                            .isEqualTo("150000000");
                });
    }

    @Test
    void shouldIgnoreBigDecimalScaleWhenComparingScore() {
        RiskFactor previous = factor(
                1L,
                "MORTGAGE",
                "HIGH",
                "30.0",
                "300000000",
                "Mortgage exists"
        );

        RiskFactor current = factor(
                2L,
                "MORTGAGE",
                "HIGH",
                "30.00",
                "300000000",
                "Mortgage exists"
        );

        List<RiskFactorComparisonResponse> results =
                comparator.compare(
                        List.of(previous),
                        List.of(current)
                );

        assertThat(results).hasSize(1);

        assertThat(results.get(0).changeType())
                .isEqualTo(RiskFactorChangeType.UNCHANGED);
    }

    private RiskFactor factor(
            Long riskAnalysisId,
            String factorType,
            String severity,
            String score,
            String value,
            String description
    ) {
        return new RiskFactor(
                riskAnalysisId,
                factorType,
                severity,
                new BigDecimal(score),
                value,
                description
        );
    }
}