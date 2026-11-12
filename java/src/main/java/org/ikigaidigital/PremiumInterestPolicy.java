package org.ikigaidigital;

/** Applies a 5% annual rate, divided by 12, after the first 45 days. */
final class PremiumInterestPolicy implements InterestPolicy {
    @Override
    public double calculateInterest(TimeDeposit deposit) {
        if (deposit.getDays() <= 45) {
            return 0;
        }
        return deposit.getBalance() * 0.05 / 12;
    }
}
