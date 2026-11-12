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
     * @param xs deposits whose balances are updated in place
     */
    public void updateBalance(List<TimeDeposit> xs) {
        for (int i = 0; i < xs.size(); i++) {
            double interest = 0;

            if (xs.get(i).getDays() > 30) {
                if (xs.get(i).getPlanType().equals("student")) {
                    if (xs.get(i).getDays() < 366) {
                        interest += xs.get(i).getBalance() * 0.03 / 12;
                    }
                } else if (xs.get(i).getPlanType().equals("premium")) {
                    if (xs.get(i).getDays() > 45) {
                        interest += xs.get(i).getBalance() * 0.05 / 12;
                    }
                } else if (xs.get(i).getPlanType().equals("basic")) {
                    interest += xs.get(i).getBalance() * 0.01 / 12;
                }
            }

            double a2d = xs.get(i).getBalance() + (new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)).doubleValue();
            xs.get(i).setBalance(a2d);
        }
    }
}
