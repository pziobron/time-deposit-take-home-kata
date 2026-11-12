package org.ikigaidigital.application.port;

import org.ikigaidigital.application.model.BalanceUpdate;
import org.ikigaidigital.application.model.InterestCalculationInput;
import org.ikigaidigital.application.model.TimeDepositDetails;

import java.util.List;

/** Persistence operations required by the deposit application use cases. */
public interface TimeDepositRepository {
    /** Returns all deposits with their withdrawal history, or an empty list. */
    List<TimeDepositDetails> findAll();

    /**
     * Returns calculation inputs without withdrawals. Database adapters must lock
     * these deposits until the surrounding update transaction completes.
     */
    List<InterestCalculationInput> findAllForBalanceUpdate();

    /** Saves balances by deposit ID without changing metadata or withdrawal history. */
    void saveBalances(List<BalanceUpdate> updates);
}
