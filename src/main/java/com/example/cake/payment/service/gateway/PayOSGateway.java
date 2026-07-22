package com.example.cake.payment.service.gateway;

import com.example.cake.payment.model.Payment;
import com.example.cake.payment.service.PayOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PayOS gateway implementation (Strategy).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayOSGateway implements PaymentGateway {

    private final PayOSService payOSService;

    @Override
    public Payment.PaymentMethod getMethod() {
        return Payment.PaymentMethod.PAYOS;
    }

    @Override
    public String createPaymentUrl(Payment payment, String orderInfo, String ipAddress) {
        // PayOS requires a numeric orderCode.
        long orderCode = Math.abs(payment.getId().hashCode());

        payment.setProviderOrderCode(orderCode);

        Map<String, Object> data = payOSService.createPaymentLink(orderCode, payment.getAmount(),
                (orderInfo == null || orderInfo.isBlank()) ? "Thanh toan khoa hoc" : orderInfo);

        // Prefer checkoutUrl
        String checkoutUrl = (String) data.get("checkoutUrl");
        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            checkoutUrl = (String) data.get("paymentUrl");
        }

        // If PayOS returns orderCode, keep it consistent
        Object returnedOrderCode = data.get("orderCode");
        if (returnedOrderCode instanceof Number n) {
            payment.setProviderOrderCode(n.longValue());
            orderCode = n.longValue();
        }

        log.info("[PayOS] Created payment link orderCode={} paymentId={} checkoutUrl={}", orderCode, payment.getId(), checkoutUrl);
        return checkoutUrl;
    }

    @Override
    public boolean verifyCallbackSignature(Map<String, String> params) {
        // Require verifying signature if raw body + signature are provided (PayOS webhook includes signature in JSON).
        String rawBody = params.get("rawBody");
        String signature = params.get("signature");
        if (rawBody != null && signature != null) {
            return payOSService.verifyWebhookSignature(rawBody, signature);
        }

        // No signature -> reject (security requirement)
        log.error("[PayOS] Missing signature/rawBody in webhook callback");
        return false;
    }

    @Override
    public GatewayCallbackResult parseCallback(Map<String, String> params) {
        // We don't get internal paymentId from PayOS; use provider orderCode to resolve later.
        String orderCode = params.get("orderCode");
        String code = params.get("code");
        String success = params.get("success");

        // Consider success if success=true OR code==00
        boolean ok = "true".equalsIgnoreCase(success) || "00".equalsIgnoreCase(code);

        // Store orderCode into paymentId field temporarily; PaymentFacade will resolve it.
        // (We keep using GatewayCallbackResult contract without expanding it.)
        return GatewayCallbackResult.builder()
                .paymentId(orderCode)
                .responseCode(ok ? "00" : (code != null ? code : "FAILED"))
                .transactionNo(params.getOrDefault("reference", params.get("paymentLinkId")))
                .bankCode(null)
                .build();
    }

    @Override
    public String getResponseMessage(String responseCode) {
        if (responseCode == null) return "Thanh toán thất bại";
        if ("SUCCESS".equalsIgnoreCase(responseCode) || "00".equalsIgnoreCase(responseCode)) {
            return "Thanh toán thành công";
        }
        return "Thanh toán thất bại";
    }
}
