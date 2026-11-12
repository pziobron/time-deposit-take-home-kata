package org.ikigaidigital;

/** Calculates unrounded monthly interest according to a plan's eligibility rules. */
interface InterestPolicy {
    /** Returns unrounded monthly interest, or zero when the deposit is ineligible. */
    double calculateInterest(TimeDeposit deposit);
}
