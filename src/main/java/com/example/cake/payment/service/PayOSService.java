package com.example.cake.payment.service;

import com.example.cake.payment.config.PayOSConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * PayOS integration via REST API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayOSService {

    private final PayOSConfig payOSConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Create PayOS payment link and return checkoutUrl + orderCode.
     *
     * PayOS create-link signature:
     * - Use exactly 5 fields: amount, cancelUrl, description, orderCode, returnUrl
     * - Canonical string format: key=value joined by '&' in that exact order
     * - IMPORTANT: values are NOT URL-encoded when signing (PayOS verifies raw string).
     */
    public Map<String, Object> createPaymentLink(long orderCode, long amount, String description) {
        String url = payOSConfig.getApiBaseUrl() + "/v2/payment-requests";

        // Build payload sent to PayOS (schema strict: webhookUrl MUST NOT be present)
        JSONObject body = new JSONObject();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("returnUrl", payOSConfig.getReturnUrl());
        body.put("cancelUrl", payOSConfig.getCancelUrl());

        String canonicalString = buildCreateLinkCanonicalString(orderCode, amount, description,
                payOSConfig.getReturnUrl(), payOSConfig.getCancelUrl());

        String signature = hmacSHA256(payOSConfig.getChecksumKey(), canonicalString);
        body.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", payOSConfig.getClientId());
        headers.set("x-api-key", payOSConfig.getApiKey());

        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("PayOS create payment link failed: HTTP " + response.getStatusCode());
        }

        JSONObject json = new JSONObject(response.getBody());

        // PayOS can respond using either: {success:true,data:{...}} OR {code:"00",desc:"...",data:{...}}
        boolean successFlag = json.optBoolean("success", false);
        String code = json.optString("code", "");
        String desc = json.optString("desc", json.optString("message", "unknown"));

        boolean ok = successFlag || "00".equals(code);
        if (!ok) {
            log.error("PayOS create payment link failed. url={} payload={} canonicalString={} response={}",
                    url, body, canonicalString, response.getBody());
            throw new RuntimeException("PayOS create payment link failed: code=" + (code.isBlank() ? "(none)" : code) + ", desc=" + desc);
        }

        JSONObject data = json.optJSONObject("data");
        if (data == null) {
            log.error("PayOS create payment link missing data. url={} payload={} canonicalString={} response={}",
                    url, body, canonicalString, response.getBody());
            throw new RuntimeException("PayOS create payment link failed: Missing data in response");
        }

        return Map.of(
                "checkoutUrl", data.optString("checkoutUrl"),
                "orderCode", data.optLong("orderCode")
        );
    }

    private String buildCreateLinkCanonicalString(long orderCode, long amount, String description, String returnUrl, String cancelUrl) {
        // NOTE: Raw values (no URL encoding)
        return "amount=" + amount
                + "&cancelUrl=" + (cancelUrl == null ? "" : cancelUrl)
                + "&description=" + (description == null ? "" : description)
                + "&orderCode=" + orderCode
                + "&returnUrl=" + (returnUrl == null ? "" : returnUrl);
    }

    /**
     * Verify PayOS webhook signature.
     * Many setups sign the raw body using checksumKey.
     */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String calculated = hmacSHA256(payOSConfig.getChecksumKey(), rawBody);
        return calculated.equalsIgnoreCase(signature.trim());
    }

    private String hmacSHA256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (Exception e) {
            log.error("PayOS HMAC_SHA256 error: {}", e.getMessage(), e);
            return "";
        }
    }
}
