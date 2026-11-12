package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.adapter.out.persistence.entity.TimeDepositEntity;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/** Spring Data JDBC queries for deposit reads and explicit balance-only writes. */
public interface TimeDepositJdbcRepository extends Repository<TimeDepositEntity, Integer> {
    List<TimeDepositEntity> findAllByOrderByIdAsc();

    // Keep row locks until the application transaction completes; ordered locking
    // makes concurrent balance updates use committed results rather than stale inputs.
    @Query("""
        SELECT id, "planType", balance, days FROM "timeDeposits"
        ORDER BY id FOR UPDATE
        """)
    List<TimeDepositEntity> findAllForBalanceUpdate();

    @Modifying
    @Query("UPDATE \"timeDeposits\" SET balance = :balance WHERE id = :id")
    void updateBalance(@Param("id") int id, @Param("balance") BigDecimal balance);
}
