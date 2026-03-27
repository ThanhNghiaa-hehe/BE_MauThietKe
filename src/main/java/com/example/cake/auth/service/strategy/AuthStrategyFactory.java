package com.example.cake.auth.service.strategy;

import com.example.cake.auth.model.AutheProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory Method / Simple Factory: chooses an AuthStrategy by provider.
 */
@Component
public class AuthStrategyFactory {

    private final Map<AutheProvider, AuthStrategy> strategies;

    public AuthStrategyFactory(List<AuthStrategy> strategyList) {
        Map<AutheProvider, AuthStrategy> map = new EnumMap<>(AutheProvider.class);
        for (AuthStrategy s : strategyList) {
            map.put(s.getProvider(), s);
        }
        this.strategies = Map.copyOf(map);
    }

    public AuthStrategy getStrategy(AutheProvider provider) {
        AuthStrategy s = strategies.get(provider);
        if (s == null) {
            throw new IllegalArgumentException("Unsupported auth provider: " + provider);
        }
        return s;
    }
}

