package org.ikigaidigital.application.model;

import java.math.BigDecimal;
import java.util.List;

/** Immutable deposit snapshot, including withdrawal history and decimal balance. */
public record TimeDepositDetails(int id, String planType, BigDecimal balance,
                                 int days, List<Withdrawal> withdrawals) {
    public TimeDepositDetails {
        withdrawals = List.copyOf(withdrawals);
    }
}
