package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Updates deposit balances using plan-specific interest policies and rounding to two decimal places. */
public class TimeDepositCalculator {

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
        if (deposit.getDays() <= 30) {
            return 0;
        }

        if (deposit.getPlanType().equals("student") && deposit.getDays() < 366) {
            return deposit.getBalance() * 0.03 / 12;
        }
        if (deposit.getPlanType().equals("premium") && deposit.getDays() > 45) {
            return deposit.getBalance() * 0.05 / 12;
        }
        if (deposit.getPlanType().equals("basic")) {
            return deposit.getBalance() * 0.01 / 12;
        }
        return 0;
    }
}
