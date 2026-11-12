package org.ikigaidigital.application.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Read-only withdrawal history returned with a deposit. */
public record Withdrawal(int id, int timeDepositId, BigDecimal amount, LocalDate date) {
}
