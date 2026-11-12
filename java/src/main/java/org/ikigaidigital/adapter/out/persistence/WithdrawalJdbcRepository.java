package org.ikigaidigital.adapter.out.persistence;

import org.ikigaidigital.adapter.out.persistence.entity.WithdrawalEntity;
import org.springframework.data.repository.Repository;

import java.util.List;

/** Read-only Spring Data JDBC access to withdrawal history. */
public interface WithdrawalJdbcRepository extends Repository<WithdrawalEntity, Integer> {
    List<WithdrawalEntity> findAllByOrderByIdAsc();
}
