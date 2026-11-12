package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimeDepositCalculatorTest {
    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @ParameterizedTest(name = "{0} receives no interest on day {1}")
    @CsvSource({
        "basic, 0", "basic, 29", "basic, 30",
        "student, 0", "student, 29", "student, 30",
        "premium, 0", "premium, 29", "premium, 30"
    })
    void noPlanEarnsInterestDuringTheFirstThirtyDays(String plan, int days) {
        assertUpdatedBalance(plan, 1200.0, days, 1200.0);
    }

    @Test
    void basicEarnsOnePercentDividedByTwelveAfterThirtyDays() {
        assertUpdatedBalance("basic", 1200.0, 31, 1201.0);
    }

    @Test
    void basicContinuesEarningInterestAfterOneYear() {
        assertUpdatedBalance("basic", 1200.0, 366, 1201.0);
    }

    @ParameterizedTest(name = "day {0}")
    @ValueSource(ints = {31, 364, 365})
    void studentEarnsThreePercentDividedByTwelveThroughDay365(int days) {
        assertUpdatedBalance("student", 1200.0, days, 1203.0);
    }

    @ParameterizedTest(name = "day {0}")
    @ValueSource(ints = {366, 400})
    void studentReceivesNoInterestAfterDay365(int days) {
        assertUpdatedBalance("student", 1200.0, days, 1200.0);
    }

    @ParameterizedTest(name = "day {0}")
    @ValueSource(ints = {31, 44, 45})
    void premiumReceivesNoInterestThroughDay45(int days) {
        assertUpdatedBalance("premium", 1200.0, days, 1200.0);
    }

    @ParameterizedTest(name = "day {0}")
    @ValueSource(ints = {46, 366})
    void premiumEarnsFivePercentDividedByTwelveAfterDay45(int days) {
        assertUpdatedBalance("premium", 1200.0, days, 1205.0);
    }

    @ParameterizedTest(name = "{0} has balance {1} after rounding fractional interest")
    @CsvSource({
        "student, 100.26",
        "premium, 100.43"
    })
    void roundsFractionalInterestForStudentAndPremiumPlans(String plan, double expectedBalance) {
        // On a balance of 100.01, student interest is 0.250025 and
        // premium interest is approximately 0.41670833: round down and up respectively.
        assertUpdatedBalance(plan, 100.01, 46, expectedBalance);
    }

    @Test
    void roundsInterestBelowHalfACentDown() {
        assertUpdatedBalance("basic", 5.99, 31, 5.99);
    }

    @Test
    void roundsInterestAtHalfACentUp() {
        assertUpdatedBalance("basic", 6.0, 31, 6.01);
    }

    @Test
    void roundsInterestAboveHalfACentUp() {
        assertUpdatedBalance("basic", 6.01, 31, 6.02);
    }

    @Test
    void preservesExistingBinaryFloatingPointRoundingAtAHalfCentBoundary() {
        // Converting the double 0.015 to BigDecimal exposes a value below 0.015.
        // Preserve this observable behavior during the subsequent refactor.
        assertUpdatedBalance("basic", 18.0, 31, 18.01);
    }

    @Test
    void roundsOnlyTheInterestRatherThanTheEntireBalance() {
        assertUpdatedBalance("basic", 1200.123, 31, 1201.123);
    }

    @ParameterizedTest(name = "plan: [{0}]")
    @ValueSource(strings = {"basic", "student", "premium"})
    void zeroBalancesRemainZeroForEligiblePlans(String plan) {
        assertUpdatedBalance(plan, 0.0, 46, 0.0);
    }

    @ParameterizedTest(name = "plan: [{0}]")
    @ValueSource(strings = {"unknown", "Basic", ""})
    void unrecognisedPlanTypesReceiveNoInterest(String plan) {
        assertUpdatedBalance(plan, 1200.0, 46, 1200.0);
    }

    @Test
    void acceptsAnEmptyList() {
        calculator.updateBalance(List.of());
    }

    @Test
    void updatesEveryDepositIndependentlyWithoutReplacingOrReorderingThem() {
        TimeDeposit basic = new TimeDeposit(1, "basic", 1200.0, 31);
        TimeDeposit student = new TimeDeposit(2, "student", 2400.0, 365);
        TimeDeposit premium = new TimeDeposit(3, "premium", 3600.0, 46);
        TimeDeposit ineligible = new TimeDeposit(4, "premium", 4800.0, 45);
        List<TimeDeposit> deposits = List.of(basic, student, premium, ineligible);

        calculator.updateBalance(deposits);

        assertThat(deposits).containsExactly(basic, student, premium, ineligible);
        assertThat(basic.getBalance()).isEqualTo(1201.0);
        assertThat(student.getBalance()).isEqualTo(2406.0);
        assertThat(premium.getBalance()).isEqualTo(3615.0);
        assertThat(ineligible.getBalance()).isEqualTo(4800.0);
    }

    @Test
    void eachInvocationAppliesInterestToTheCurrentBalanceWithoutAdvancingDays() {
        TimeDeposit deposit = new TimeDeposit(1, "student", 1200.0, 31);

        calculator.updateBalance(List.of(deposit));
        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(1206.01);
        assertThat(deposit.getDays()).isEqualTo(31);
    }

    private void assertUpdatedBalance(String plan, double balance, int days, double expectedBalance) {
        TimeDeposit deposit = new TimeDeposit(42, plan, balance, days);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(expectedBalance);
        assertThat(deposit.getId()).isEqualTo(42);
        assertThat(deposit.getPlanType()).isEqualTo(plan);
        assertThat(deposit.getDays()).isEqualTo(days);
    }
}
