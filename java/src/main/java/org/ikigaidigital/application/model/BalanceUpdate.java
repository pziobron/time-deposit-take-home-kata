package org.ikigaidigital.application.model;

import java.math.BigDecimal;

/** A replacement balance for an existing deposit, identified by ID. */
public record BalanceUpdate(int id, BigDecimal balance) {
}
