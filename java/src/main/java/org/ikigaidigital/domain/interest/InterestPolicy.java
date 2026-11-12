package org.ikigaidigital.domain.interest;

import org.ikigaidigital.TimeDeposit;

/** Calculates unrounded monthly interest according to a plan's eligibility rules. */
public interface InterestPolicy {
    /** Returns unrounded monthly interest, or zero when the deposit is ineligible. */
    double calculateInterest(TimeDeposit deposit);
}
