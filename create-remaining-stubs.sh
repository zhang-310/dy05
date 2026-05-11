#!/bin/bash
# 批量创建剩余的 Controller stubs

# 创建目录
mkdir -p douyin-operations-intelligence/src/main/java/cn/gaifan/douyinOperations/module/agent/controller
mkdir -p douyin-operations-intelligence/src/main/java/cn/gaifan/douyinOperations/module/agent/vo
mkdir -p douyin-operations-asset/src/main/java/cn/gaifan/douyinOperations/module/product/controller
mkdir -p douyin-operations-asset/src/main/java/cn/gaifan/douyinOperations/module/product/vo
mkdir -p douyin-operations-content/src/main/java/cn/gaifan/douyinOperations/module/copy/controller
mkdir -p douyin-operations-content/src/main/java/cn/gaifan/douyinOperations/module/copy/vo
mkdir -p douyin-operations-payment/src/main/java/cn/gaifan/douyinOperations/module/payment/controller
mkdir -p douyin-operations-payment/src/main/java/cn/gaifan/douyinOperations/module/payment/vo
mkdir -p douyin-operations-platform/src/main/java/cn/gaifan/douyinOperations/module/auth/controller
mkdir -p douyin-operations-platform/src/main/java/cn/gaifan/douyinOperations/module/auth/vo
mkdir -p douyin-operations-platform/src/main/java/cn/gaifan/douyinOperations/module/log/controller
mkdir -p douyin-operations-platform/src/main/java/cn/gaifan/douyinOperations/module/log/vo
mkdir -p douyin-operations-platform/src/main/java/cn/gaifan/douyinOperations/module/workflow/controller
mkdir -p douyin-operations-platform/src/main/java/cn/gaifan/douyinOperations/module/workflow/vo

echo "✅ 目录创建完成"
echo ""
echo "接下来需要手动创建以下 10 个 Controller："
echo "1. AgentMarketController - /api/v1/agent/market/list"
echo "2. ProductCategoryController - /api/v1/product/category/search"
echo "3. CopyTagController - /api/v1/copy/tag/search"
echo "4. PaymentTransactionController - /api/v1/payment/transaction/search"
echo "5. PaymentRefundController - /api/v1/payment/refund/search"
echo "6. AuthPermissionController - /api/v1/auth/permission/search"
echo "7. LogOperationController - /api/v1/log/operation/search"
echo "8. LogAuditController - /api/v1/log/audit/search"
echo "9. WorkflowController - /api/v1/workflow/search"
echo "10. BenchmarkQualityScriptController - 修复现有 500 错误"
