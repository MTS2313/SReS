package br.com.sres.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findBySubject(String subject);
    List<AccountEntity> findAllByOrderByCreatedAtAsc();
}
