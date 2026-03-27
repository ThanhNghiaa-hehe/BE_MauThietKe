package com.example.cake.lesson.service.progress;

import lombok.Builder;
import lombok.Value;

/**
 * Result returned by state transitions.
 */
@Value
@Builder
public class StateTransitionResult {
    boolean allowed;
    String message;

    public static StateTransitionResult allow(String msg) {
        return StateTransitionResult.builder().allowed(true).message(msg).build();
    }

    public static StateTransitionResult deny(String msg) {
        return StateTransitionResult.builder().allowed(false).message(msg).build();
    }
}
