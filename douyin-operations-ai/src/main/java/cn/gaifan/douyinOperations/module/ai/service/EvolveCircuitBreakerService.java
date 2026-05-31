package cn.gaifan.douyinOperations.module.ai.service;

import java.util.Map;

/**
 * Guards the evolution scheduler from repeatedly creating tasks when the LLM
 * provider is in a non-recoverable account state such as insufficient balance.
 */
public interface EvolveCircuitBreakerService {

    boolean isOpen();

    String currentReason();

    void recordFailure(String errorMessage);

    Map<String, Object> status();
}
