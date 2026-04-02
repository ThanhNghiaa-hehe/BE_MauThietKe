package com.example.cake.payment.service.payos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter pattern: adapts PayOS webhook JSON schema (adaptee) into a normalized map (target)
 * consumed by PaymentFacade/PaymentGateway without changing existing business logic.
 */
@Slf4j
@Component
public class PayOSWebhookAdapter {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Parse PayOS webhook raw JSON and adapt it to the legacy params map contract used by PaymentFacade.
     *
     * Output keys (kept stable): orderCode, amount, reference, paymentLinkId, signature, rawBody, code, success
     */
    public AdaptedWebhook adapt(String rawBody, Map<String, String> headers) {
        try {
            JsonNode root = mapper.readTree(rawBody);

            String signature = root.path("signature").asText(null);
            boolean success = root.path("success").asBoolean(false);
            String topCode = root.path("code").asText(null);

            JsonNode data = root.path("data");
            Long orderCode = data.path("orderCode").isMissingNode() ? null : data.path("orderCode").asLong();
            Long amount = data.path("amount").isMissingNode() ? null : data.path("amount").asLong();
            String dataCode = data.path("code").asText(null);
            String reference = data.path("reference").asText(null);
            String paymentLinkId = data.path("paymentLinkId").asText(null);

            Map<String, String> params = new HashMap<>();
            if (orderCode != null) params.put("orderCode", String.valueOf(orderCode));
            if (amount != null) params.put("amount", String.valueOf(amount));
            if (reference != null) params.put("reference", reference);
            if (paymentLinkId != null) params.put("paymentLinkId", paymentLinkId);
            if (signature != null) params.put("signature", signature);
            params.put("rawBody", rawBody);

            // Normalize code like existing controller logic
            String normalizedCode = (dataCode != null) ? dataCode : topCode;
            params.put("code", normalizedCode);
            params.put("success", String.valueOf(success));

            log.info("[PayOSWebhookAdapter] Adapted webhook orderCode={} success={} code={} dataCode={} ref={} linkId={} headers={}",
                    orderCode, success, topCode, dataCode, reference, paymentLinkId,
                    headers != null ? headers.keySet() : null);

            return AdaptedWebhook.builder()
                    .params(params)
                    .orderCode(orderCode)
                    .amount(amount)
                    .success(success)
                    .code(normalizedCode)
                    .signature(signature)
                    .build();
        } catch (Exception e) {
            log.error("[PayOSWebhookAdapter] Parse/adapt error: {}", e.getMessage(), e);
            return AdaptedWebhook.builder()
                    .params(null)
                    .errorMessage("Webhook payload không hợp lệ")
                    .build();
        }
    }

    @Value
    @Builder
    public static class AdaptedWebhook {
        Map<String, String> params;
        Long orderCode;
        Long amount;
        Boolean success;
        String code;
        String signature;
        String errorMessage;
    }
}

