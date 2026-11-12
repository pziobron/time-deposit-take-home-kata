package org.ikigaidigital.adapter.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Maps withdrawal history independently so balance updates cannot rewrite it. */
@Table("withdrawals")
public record WithdrawalEntity(@Id int id, @Column("timeDepositId") int timeDepositId,
                               BigDecimal amount, LocalDate date) {
}
