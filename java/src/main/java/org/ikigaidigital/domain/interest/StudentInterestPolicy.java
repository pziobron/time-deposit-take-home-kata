package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;

/** Applies a 3% annual rate, divided by 12, from day 31 through day 365 inclusive. */
public final class StudentInterestPolicy implements InterestPolicy {
    @Override
    public double calculateInterest(TimeDeposit deposit) {
        if (deposit.getDays() <= 30 || deposit.getDays() >= 366) {
            return 0;
        }
        return deposit.getBalance() * 0.03 / 12;
    }
}
