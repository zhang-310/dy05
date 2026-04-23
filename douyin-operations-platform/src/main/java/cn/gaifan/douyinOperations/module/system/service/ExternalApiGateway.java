package cn.gaifan.douyinOperations.module.system.service;

import org.springframework.http.ResponseEntity;
import java.util.Map;

public interface ExternalApiGateway {
    ResponseEntity<String> call(String providerCode, String endpoint, String method, Map<String, Object> params);
}
