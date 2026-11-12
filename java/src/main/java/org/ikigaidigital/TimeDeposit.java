package org.ikigaidigital;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** Represents a time deposit with its plan type, current balance, and age in days. */
@Getter
@AllArgsConstructor
public class TimeDeposit {
    private int id;
    private String planType;
    @Setter
    private Double balance;
    private int days;
}
