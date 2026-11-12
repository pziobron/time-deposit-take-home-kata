package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/** Updates deposit balances using plan-specific interest policies and rounding to two decimal places. */
public class TimeDepositCalculator {
    private final Map<String, InterestPolicy> interestPolicies = Map.of(
        "basic", new BasicInterestPolicy(),
        "student", new StudentInterestPolicy(),
        "premium", new PremiumInterestPolicy()
    );

    /**
     * Applies one month's interest to each eligible deposit using its current balance and days.
     *
     * <p>The listed rates are treated as annual rates, divided by 12 to calculate
     * monthly interest, matching the existing implementation.
     *
     * <p>Each invocation applies one update. This method neither schedules updates
     * nor advances the number of days.
     *
     * @param deposits deposits whose balances are updated in place
     */
    public void updateBalance(List<TimeDeposit> deposits) {
        for (TimeDeposit deposit : deposits) {
            double interest = calculateInterest(deposit);
            double roundedInterest = new BigDecimal(interest)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
            deposit.setBalance(deposit.getBalance() + roundedInterest);
        }
    }

    private double calculateInterest(TimeDeposit deposit) {
        InterestPolicy policy = interestPolicies.get(deposit.getPlanType());
        return policy == null ? 0 : policy.calculateInterest(deposit);
    }
}
