package cn.gaifan.douyinOperations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 抖音运营后台启动类
 */
@SpringBootApplication
@EnableScheduling
public class DouyinOperationsApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DouyinOperationsApplication.class, args);
    }
}
