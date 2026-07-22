package com.example.cake.payment.repository;

import com.example.cake.payment.model.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {

    List<Invoice> findByUserIdOrderByIssuedAtDesc(String userId);

    List<Invoice> findByUserPhoneNumberOrderByIssuedAtDesc(String userPhoneNumber);

    Optional<Invoice> findByPaymentId(String paymentId);

    Optional<Invoice> findByProviderOrderCode(Long providerOrderCode);

    void deleteByUserId(String userId);
}
