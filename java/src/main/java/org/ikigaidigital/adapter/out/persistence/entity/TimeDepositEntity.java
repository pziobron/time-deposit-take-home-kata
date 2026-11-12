package org.ikigaidigital.adapter.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/** Maps deposit storage without adding persistence annotations to the shared model. */
@Table("timeDeposits")
public record TimeDepositEntity(@Id int id, @Column("planType") String planType,
                                BigDecimal balance, int days) {
}
