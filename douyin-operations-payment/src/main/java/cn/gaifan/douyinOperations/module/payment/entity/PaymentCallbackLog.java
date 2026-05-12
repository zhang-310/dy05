package cn.gaifan.douyinOperations.module.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * P1-7: 支付回调日志表
 * 记录所有支付回调请求，用于重试控制和问题排查
 */
@Entity
@Table(name = "pay_callback_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 回调唯一标识 */
    @Column(name = "callback_id", nullable = false, unique = true, length = 100)
    private String callbackId;

    /** 订单号 */
    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    /** 回调状态 */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** 请求体 */
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    /** 客户端 IP */
    @Column(name = "client_ip", length = 50)
    private String clientIp;

    /** 处理状态 (SUCCESS/FAILED) */
    @Column(name = "process_status", length = 20)
    private String processStatus;

    /** 错误信息 */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 接收时间 */
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    /** 处理时间 */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
    }
}
