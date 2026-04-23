# 支付模块（payment）

## 模块概述

订单管理与支付处理，支持订单创建、支付、退款、交易日志记录。

## 后端结构

```
module/payment/
├── controller/
│   ├── OrderController.java                  # 订单 CRUD
│   ├── PaymentController.java                # 支付处理
│   └── RefundController.java                 # 退款处理
│
├── entity/
│   ├── PaymentOrder.java                     # 订单（payment_order）
│   ├── PaymentOrderItem.java                 # 订单项（payment_order_item）
│   ├── PaymentRefund.java                    # 退款（payment_refund）
│   ├── PaymentTransactionLog.java            # 交易日志（payment_transaction_log）
│   ├── OrderStatus.java                      # 订单状态枚举
│   ├── RefundStatus.java                     # 退款状态枚举
│   ├── TransactionStatus.java                # 交易状态枚举
│   └── TransactionType.java                  # 交易类型枚举
│
├── service/
│   ├── OrderService.java / impl/             # 订单管理
│   ├── RefundService.java / impl/            # 退款管理
│   └── DouyinPaymentService.java             # 抖音支付对接
│
└── vo/
    ├── OrderSaveVO / SearchVO / VO
    └── RefundSaveVO / RefundVO
```

## 前端页面

| 页面 | 文件 | 路由 |
|------|------|------|
| 订单列表 | `pages/payment/OrderListPage.tsx` | `/admin/payment/orders` |
| 支付处理 | `pages/payment/PaymentProcessComponent.tsx` | — |

## 前端 API

文件：`api/payment.ts`

### 接口说明

| 函数 | 方法 | 路径 | 说明 |
|------|------|------|------|
| searchOrders | POST | /payment/orders | 订单列表（@RequestParam page, rows） |
| getOrderByOrderNo | POST | /payment/order/getByOrderNo | 按订单号查询 |
| getOrderDetail | POST | /payment/order/detail | 订单详情 |
| createOrder | POST | /payment/create-order | 创建订单 |
| updateOrder | POST | /payment/order/update | 更新订单 |
| cancelOrder | POST | /payment/order/cancel | 取消订单 |
| deleteOrder | POST | /payment/order/delete | 删除订单 |
| getOrderStatistics | POST | /payment/stats | 订单统计（@RequestParam period） |
| searchPayments | POST | /payment/payment/search | 支付记录 |
| createPayment | POST | /payment/payment/create | 发起支付 |
| processPayment | POST | /payment/payment/process | 处理支付 |
| verifyPayment | POST | /payment/payment/verify | 验证支付 |
| requestRefund | POST | /payment/refund | 申请退款（@RequestParam orderNo, reason） |
| searchRefunds | POST | /payment/refund/search | 退款列表 |
| approveRefund | POST | /payment/refund/approve | 批准退款 |
| rejectRefund | POST | /payment/refund/reject | 拒绝退款 |
| getPaymentStats | POST | /payment/stats | 支付统计（@RequestParam period） |
| triggerReconciliation | POST | /payment/reconciliation | 对账 |

SQL 文件：`sql/payment/`
