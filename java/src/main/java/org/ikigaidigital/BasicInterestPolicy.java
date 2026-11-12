package org.ikigaidigital;

/** Applies a 1% annual rate, divided by 12, after the first 30 days. */
final class BasicInterestPolicy implements InterestPolicy {
    @Override
    public double calculateInterest(TimeDeposit deposit) {
        if (deposit.getDays() <= 30) {
            return 0;
        }
        return deposit.getBalance() * 0.01 / 12;
    }
}
