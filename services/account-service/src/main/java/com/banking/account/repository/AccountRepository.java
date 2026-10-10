package com.banking.account.repository;

import com.banking.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Veri erişim katmanı.
// Database-per-service: bu repository yalnızca Account Service'e aittir ve kendi
// veritabanına (account-db) bağlanır. Başka hiçbir servis bu tablolara doğrudan
// erişemez; hesap verisine ihtiyaç duyan servis gRPC/REST kontratından geçmek zorundadır.
public interface AccountRepository extends JpaRepository<Account, UUID> {
}
