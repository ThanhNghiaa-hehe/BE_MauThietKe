package com.example.cake.coupon.service;

import com.example.cake.coupon.model.Coupon;
import com.example.cake.coupon.repository.CouponRepository;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * Kiểm tra và áp dụng mã giảm giá
     */
    public ResponseMessage<Map<String, Object>> applyCoupon(String code, Long orderAmount) {
        if (code == null || code.isBlank()) {
            return new ResponseMessage<>(false, "Vui lòng nhập mã giảm giá", null);
        }

        Optional<Coupon> optional = couponRepository.findByCodeIgnoreCaseAndActiveTrue(code.trim());
        if (optional.isEmpty()) {
            return new ResponseMessage<>(false, "Mã giảm giá không tồn tại hoặc đã hết hạn", null);
        }

        Coupon coupon = optional.get();

        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now())) {
            return new ResponseMessage<>(false, "Mã giảm giá đã hết hạn sử dụng", null);
        }

        if (coupon.getMinOrderAmount() != null && orderAmount != null && orderAmount < coupon.getMinOrderAmount()) {
            return new ResponseMessage<>(false, "Đơn hàng chưa đạt giá trị tối thiểu " + coupon.getMinOrderAmount() + " VNĐ để dùng mã này", null);
        }

        long discountCalculated = 0L;
        if (coupon.getDiscountPercent() != null && coupon.getDiscountPercent() > 0) {
            discountCalculated = (orderAmount * coupon.getDiscountPercent()) / 100;
        } else if (coupon.getDiscountAmount() != null && coupon.getDiscountAmount() > 0) {
            discountCalculated = coupon.getDiscountAmount();
        }

        long finalAmount = Math.max(0L, orderAmount - discountCalculated);

        Map<String, Object> data = new HashMap<>();
        data.put("code", coupon.getCode());
        data.put("discountAmount", discountCalculated);
        data.put("finalAmount", finalAmount);
        data.put("discountPercent", coupon.getDiscountPercent());

        return new ResponseMessage<>(true, "Áp dụng mã giảm giá thành công", data);
    }

    /**
     * Admin tạo mã giảm giá mới
     */
    public ResponseMessage<Coupon> createCoupon(Coupon coupon) {
        if (coupon.getCode() == null || coupon.getCode().isBlank()) {
            return new ResponseMessage<>(false, "Mã giảm giá không được để trống", null);
        }

        String cleanCode = coupon.getCode().trim().toUpperCase();
        if (couponRepository.findByCodeIgnoreCase(cleanCode).isPresent()) {
            return new ResponseMessage<>(false, "Mã giảm giá này đã tồn tại", null);
        }

        coupon.setCode(cleanCode);
        coupon.setActive(true);
        if (coupon.getExpirationDate() == null) {
            coupon.setExpirationDate(LocalDateTime.now().plusMonths(1));
        }

        Coupon saved = couponRepository.save(coupon);
        return new ResponseMessage<>(true, "Tạo mã giảm giá thành công", saved);
    }

    /**
     * Lấy tất cả mã giảm giá (Admin)
     */
    public ResponseMessage<List<Coupon>> getAllCoupons() {
        return new ResponseMessage<>(true, "Lấy danh sách mã giảm giá thành công", couponRepository.findAll());
    }
}
