package com.example.cake.coupon.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "coupons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    @Id
    private String id;

    /** Coupon code (e.g., "CHAOHE2026", "GIAM50K") */
    private String code;

    /** Discount percentage (0-100) */
    private Integer discountPercent;

    /** Fixed discount amount in VND */
    private Long discountAmount;

    /** Minimum order amount in VND required to use coupon */
    private Long minOrderAmount;

    /** Expiration date */
    private LocalDateTime expirationDate;

    /** Active status */
    private boolean active;
}
