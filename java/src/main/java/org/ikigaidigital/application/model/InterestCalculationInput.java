package org.ikigaidigital.application.model;

import java.math.BigDecimal;

/** Deposit fields required to calculate interest, without loading withdrawal history. */
public record InterestCalculationInput(int id, String planType, BigDecimal balance, int days) {
}
