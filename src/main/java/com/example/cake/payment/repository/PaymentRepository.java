package com.example.cake.payment.repository;

import com.example.cake.payment.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    /**
     * Find all payments by user
     */
    List<Payment> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Find payments by status
     */
    List<Payment> findByStatus(Payment.PaymentStatus status);

    /**
     * Find payments by user and status
     */
    List<Payment> findByUserIdAndStatus(String userId, Payment.PaymentStatus status);

    /**
     * Find payment by provider order code (PayOS orderCode).
     */
    java.util.Optional<Payment> findByProviderOrderCode(Long providerOrderCode);
}
