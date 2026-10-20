package com.marwan.walletapi.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByWalletIdOrderByCreatedAtDescIdDesc(Long walletId, Pageable pageable);
}
