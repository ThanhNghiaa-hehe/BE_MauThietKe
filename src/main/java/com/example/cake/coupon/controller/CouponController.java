package com.example.cake.coupon.controller;

import com.example.cake.coupon.model.Coupon;
import com.example.cake.coupon.service.CouponService;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * Áp dụng mã giảm giá (Công khai)
     */
    @PostMapping("/coupons/apply")
    public ResponseEntity<ResponseMessage<Map<String, Object>>> applyCoupon(
            @RequestBody Map<String, Object> request
    ) {
        String code = (String) request.get("code");
        Long orderAmount = 0L;
        if (request.get("orderAmount") != null) {
            orderAmount = Long.valueOf(request.get("orderAmount").toString());
        }

        return ResponseEntity.ok(couponService.applyCoupon(code, orderAmount));
    }

    /**
     * Lấy tất cả mã giảm giá
     */
    @GetMapping("/coupons")
    public ResponseEntity<ResponseMessage<List<Coupon>>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    /**
     * Admin tạo mã giảm giá mới
     */
    @PostMapping("/admin/coupons")
    public ResponseEntity<ResponseMessage<Coupon>> createCoupon(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(couponService.createCoupon(coupon));
    }
}
