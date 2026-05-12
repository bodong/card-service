package xyz.pakwo.cardservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.pakwo.cardservice.entity.CardAuthorization;

import java.util.Optional;

/**
 * @author sarwo.wibowo
 **/
public interface CardAuthorizationRepository extends JpaRepository<CardAuthorization, Long> {
    Optional<CardAuthorization> findByTransactionReference(String transactionReference);
    boolean existsByTransactionReference(String transactionReference);
}

