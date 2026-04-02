package com.example.cake.payment.service.gateway;

import com.example.cake.payment.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Decorator pattern: wraps a PaymentGateway and adds logging/timing without changing behavior.
 */
@Slf4j
@RequiredArgsConstructor
public class LoggingPaymentGatewayDecorator implements PaymentGateway {

    private final PaymentGateway delegate;

    @Override
    public Payment.PaymentMethod getMethod() {
        return delegate.getMethod();
    }

    @Override
    public String createPaymentUrl(Payment payment, String orderInfo, String ipAddress) {
        long start = System.nanoTime();
        try {
            String url = delegate.createPaymentUrl(payment, orderInfo, ipAddress);
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.info("[GatewayDecorator] createPaymentUrl method={} paymentId={} orderCode={} elapsedMs={} url={}",
                    getMethod(), payment != null ? payment.getId() : null,
                    payment != null ? payment.getProviderOrderCode() : null,
                    ms, url);
            return url;
        } catch (Exception e) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.error("[GatewayDecorator] createPaymentUrl FAILED method={} paymentId={} elapsedMs={} err={}",
                    getMethod(), payment != null ? payment.getId() : null, ms, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean verifyCallbackSignature(Map<String, String> params) {
        long start = System.nanoTime();
        boolean ok = delegate.verifyCallbackSignature(params);
        long ms = (System.nanoTime() - start) / 1_000_000;
        log.info("[GatewayDecorator] verifyCallbackSignature method={} ok={} elapsedMs={} keys={}",
                getMethod(), ok, ms, params != null ? params.keySet() : null);
        return ok;
    }

    @Override
    public GatewayCallbackResult parseCallback(Map<String, String> params) {
        long start = System.nanoTime();
        GatewayCallbackResult result = delegate.parseCallback(params);
        long ms = (System.nanoTime() - start) / 1_000_000;
        log.info("[GatewayDecorator] parseCallback method={} elapsedMs={} paymentId={} responseCode={} txnNo={}",
                getMethod(), ms,
                result != null ? result.getPaymentId() : null,
                result != null ? result.getResponseCode() : null,
                result != null ? result.getTransactionNo() : null);
        return result;
    }

    @Override
    public String getResponseMessage(String responseCode) {
        return delegate.getResponseMessage(responseCode);
    }
}

