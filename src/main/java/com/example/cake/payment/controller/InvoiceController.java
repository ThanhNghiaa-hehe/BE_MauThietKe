package com.example.cake.payment.controller;

import com.example.cake.auth.model.User;
import com.example.cake.auth.repository.UserRepository;
import com.example.cake.payment.model.Invoice;
import com.example.cake.payment.service.InvoiceService;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách hóa đơn của tôi
     */
    @GetMapping("/my-invoices")
    public ResponseEntity<ResponseMessage<List<Invoice>>> getMyInvoices(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null) {
            return ResponseEntity.ok(new ResponseMessage<>(false, "Vui lòng đăng nhập", null));
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(new ResponseMessage<>(false, "Không tìm thấy người dùng", null));
        }

        return ResponseEntity.ok(invoiceService.getUserInvoices(user.getId()));
    }

    /**
     * Tra cứu hóa đơn công khai theo Số điện thoại (dành cho cả Khách vãng lai & Học viên)
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseMessage<List<Invoice>>> searchInvoicesByPhone(
            @RequestParam("phone") String phone
    ) {
        return ResponseEntity.ok(invoiceService.getInvoicesByPhoneNumber(phone));
    }

    /**
     * Xem chi tiết hóa đơn theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseMessage<Invoice>> getInvoiceById(
            @PathVariable String id,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.ok(new ResponseMessage<>(false, "Vui lòng đăng nhập", null));
        }

        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    /**
     * Thống kê Doanh thu & Tất cả hóa đơn dành cho Admin
     */
    @GetMapping("/admin/all")
    public ResponseEntity<ResponseMessage<java.util.Map<String, Object>>> getAdminInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoicesForAdmin());
    }
}
