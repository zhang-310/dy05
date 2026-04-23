# payment 模块 API 文档

## 文件结构
```
config/PaymentGatewayProperties.java
controller/OrderController.java
controller/PaymentController.java
controller/RefundController.java
controller/SubscriptionController.java
entity/OrderStatus.java
entity/PaymentOrder.java
entity/PaymentOrderItem.java
entity/PaymentRefund.java
entity/PaymentTransactionLog.java
entity/RefundStatus.java
entity/Subscription.java
entity/TransactionStatus.java
entity/TransactionType.java
entity/UsageRecord.java
repository/PaymentOrderRepository.java
repository/PaymentRefundRepository.java
repository/PaymentTransactionLogRepository.java
repository/SubscriptionRepository.java
repository/UsageRecordRepository.java
service/DouyinPaymentService.java
service/OrderService.java
service/RefundService.java
service/SubscriptionService.java
service/UsageQuotaService.java
service/impl/OrderServiceImpl.java
service/impl/RefundServiceImpl.java
service/impl/SubscriptionServiceImpl.java
service/impl/UsageQuotaServiceImpl.java
vo/OrderSaveVO.java
vo/OrderSearchVO.java
vo/OrderVO.java
vo/RefundSaveVO.java
vo/RefundVO.java
```

## API 接口

### OrderController
```
@RequestMapping("/api/v1/payment/order")
@PostMapping("/create")
public RESTResult<?> createOrder(@Valid @RequestBody OrderSaveVO vo) {
@PostMapping("/get")
public RESTResult<?> getOrder(@RequestBody Map<String, Long> request) {
@PostMapping("/getByOrderNo")
public RESTResult<?> getByOrderNo(@RequestBody Map<String, String> request) {
@PostMapping("/list")
public RESTResult<?> listOrders(@Valid @RequestBody OrderSearchVO vo) {
@PostMapping("/confirmPayment")
public RESTResult<?> confirmPayment(@RequestBody Map<String, String> request) {
@PostMapping("/ship")
public RESTResult<?> shipOrder(@RequestBody Map<String, String> request) {
@PostMapping("/complete")
public RESTResult<?> completeOrder(@RequestBody Map<String, Long> request) {
@PostMapping("/cancel")
public RESTResult<?> cancelOrder(@RequestBody Map<String, Long> request) {
```

### PaymentController
```
@RequestMapping("/api/v1/payment")
@PostMapping("/create-order")
public ResponseEntity<RESTResult<?>> createOrder(
@GetMapping("/order/{orderNo}")
public ResponseEntity<RESTResult<?>> getOrder(@PathVariable String orderNo) {
@PostMapping("/callback")
public ResponseEntity<?> handlePaymentCallback(
@PostMapping("/refund")
public ResponseEntity<RESTResult<?>> requestRefund(
@GetMapping("/orders")
public ResponseEntity<RESTResult<?>> listOrders(
@PostMapping("/reconciliation")
public ResponseEntity<RESTResult<?>> triggerReconciliation() {
@GetMapping("/stats")
public ResponseEntity<RESTResult<?>> getPaymentStats(
```

### RefundController
```
@RequestMapping("/api/v1/payment/refund")
@PostMapping("/create")
public RESTResult<?> createRefund(@Valid @RequestBody RefundSaveVO vo) {
@PostMapping("/get")
public RESTResult<?> getRefund(@RequestBody Map<String, Long> request) {
@PostMapping("/listByOrder")
public RESTResult<?> listByOrder(@RequestBody Map<String, Long> request) {
@PostMapping("/approve")
public RESTResult<?> approveRefund(@RequestBody Map<String, Long> request) {
@PostMapping("/reject")
public RESTResult<?> rejectRefund(@RequestBody Map<String, String> request) {
@PostMapping("/complete")
public RESTResult<?> completeRefund(@RequestBody Map<String, Long> request) {
```

### SubscriptionController
```
@RequestMapping("/api/v1/payment/subscription")
@PostMapping("/current")
public RESTResult<Subscription> current(@CurrentUserId Long userId) {
@PostMapping("/upgrade")
public RESTResult<Subscription> upgrade(@CurrentUserId Long userId, @RequestBody Map<String, String> body) {
@PostMapping("/check-quota")
public RESTResult<Map<String, Object>> checkQuota(@CurrentUserId Long userId, @RequestBody Map<String, String> body) {
@PostMapping("/plans")
public RESTResult<Map<String, Object>> plans(@RequestBody(required = false) Map<String, String> body) {
```

## Entity 字段

### OrderStatus
```
```

### PaymentOrder
```
@Id
private Long id;
@Column(nullable = false, unique = true)
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
@Column(name = "deleted", nullable = false)
private Integer deleted = 0;
@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;
@Column(nullable = false)
private LocalDateTime updatedAt;
```

### PaymentOrderItem
```
@Id
private Long id;
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
private LocalDateTime createdAt;
```

### PaymentRefund
```
@Id
private Long id;
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
private LocalDateTime createdAt;
```

### PaymentTransactionLog
```
@Id
private Long id;
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
@Column(nullable = false)
private LocalDateTime createdAt;
```

### RefundStatus
```
```

### Subscription
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "org_id")
private Long orgId;
@Column(nullable = false, length = 20)
private String plan = "free";
@Column(nullable = false, length = 20)
private String status = "active";
@Column(name = "started_at")
private Timestamp startedAt;
@Column(name = "expires_at")
private Timestamp expiresAt;
@Column(name = "auto_renew")
private Boolean autoRenew = false;
@Column(name = "max_live_sessions")
private Integer maxLiveSessions = 5;
@Column(name = "max_sv_projects")
private Integer maxSvProjects = 10;
@Column(name = "max_ai_generations")
private Integer maxAiGenerations = 50;
@Column(name = "max_storage_mb")
private Integer maxStorageMb = 500;
@Column
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

### TransactionStatus
```
```

### TransactionType
```
```

### UsageRecord
```
@Id
private Long id;
@Column(name = "user_id", nullable = false)
private Long userId;
@Column(name = "org_id")
private Long orgId;
@Column(nullable = false, length = 50)
private String metric;
@Column(nullable = false)
private Integer delta = 1;
@Column(name = "recorded_at")
private Timestamp recordedAt;
@Column
private Integer deleted = 0;
@Column(name = "create_time")
private Timestamp createTime;
@Column(name = "update_time")
private Timestamp updateTime;
```

