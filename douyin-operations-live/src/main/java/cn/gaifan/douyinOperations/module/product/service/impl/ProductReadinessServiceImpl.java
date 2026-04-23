package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.DyProductScriptRepository;
import cn.gaifan.douyinOperations.module.product.service.ProductReadinessService;
import cn.gaifan.douyinOperations.module.product.vo.ProductReadinessVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductReadinessVO.ReadinessItem;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品上播准备度检测服务实现
 */
@Service
public class ProductReadinessServiceImpl implements ProductReadinessService {

    @Resource
    private DyProductRepository productRepository;

    @Resource
    private DyProductScriptRepository scriptRepository;

    @Override
    public ProductReadinessVO checkReadiness(Long productId, Long userId) {
        // 验证产品权限
        DyProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "产品不存在"));

        if (!product.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问此产品");
        }

        ProductReadinessVO result = new ProductReadinessVO();
        result.setProductId(productId);
        result.setProductName(product.getProductName());

        List<ReadinessItem> items = new ArrayList<>();
        int totalScore = 0;

        // 1. 基础信息完整度
        ReadinessItem basicInfo = checkBasicInfo(product);
        items.add(basicInfo);
        totalScore += basicInfo.getScore();

        // 2. 商品图片
        ReadinessItem imageCheck = checkImages(product);
        items.add(imageCheck);
        totalScore += imageCheck.getScore();

        // 3. 价格与库存
        ReadinessItem priceInventory = checkPriceInventory(product);
        items.add(priceInventory);
        totalScore += priceInventory.getScore();

        // 4. 话术准备
        ReadinessItem scriptCheck = checkScripts(productId);
        items.add(scriptCheck);
        totalScore += scriptCheck.getScore();

        // 5. 卖点提炼
        ReadinessItem sellingPoints = checkSellingPoints(product);
        items.add(sellingPoints);
        totalScore += sellingPoints.getScore();

        int overallScore = totalScore / items.size();
        result.setOverallScore(overallScore);
        result.setReadyForLive(overallScore >= 80);
        result.setItems(items);

        return result;
    }

    private ReadinessItem checkBasicInfo(DyProduct product) {
        int score = 100;
        StringBuilder issues = new StringBuilder();

        if (product.getProductName() == null || product.getProductName().isBlank()) {
            score -= 30;
            issues.append("缺少商品名称；");
        }
        if (product.getProductCategory() == null || product.getProductCategory().isBlank()) {
            score -= 20;
            issues.append("缺少分类；");
        }
        if (product.getDescription() == null || product.getDescription().isBlank()) {
            score -= 30;
            issues.append("缺少商品描述；");
        }
        if (product.getManufacturer() == null || product.getManufacturer().isBlank()) {
            score -= 20;
            issues.append("缺少厂商信息；");
        }

        String status = score >= 80 ? "ok" : score >= 60 ? "warn" : "error";
        String message = score == 100 ? "基础信息完整" : "基础信息不完整：" + issues.toString();

        return new ReadinessItem("基础信息", status, message, Math.max(0, score));
    }

    private ReadinessItem checkImages(DyProduct product) {
        int score = 100;
        String message;

        if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
            score = 0;
            message = "缺少商品主图";
        } else {
            message = "商品主图已设置";
        }

        String status = score >= 80 ? "ok" : score >= 60 ? "warn" : "error";
        return new ReadinessItem("商品图片", status, message, score);
    }

    private ReadinessItem checkPriceInventory(DyProduct product) {
        int score = 100;
        StringBuilder issues = new StringBuilder();

        if (product.getPrice() == null || product.getPrice().doubleValue() <= 0) {
            score -= 50;
            issues.append("价格未设置；");
        }
        if (product.getInventory() == null || product.getInventory() <= 0) {
            score -= 50;
            issues.append("库存不足；");
        }

        String status = score >= 80 ? "ok" : score >= 60 ? "warn" : "error";
        String message = score == 100 ? "价格与库存正常" : "价格库存问题：" + issues.toString();

        return new ReadinessItem("价格与库存", status, message, Math.max(0, score));
    }

    private ReadinessItem checkScripts(Long productId) {
        List<DyProductScript> scripts = scriptRepository.findByProductIdAndDeletedOrderByCreateTimeDesc(productId, 0);
        List<DyProductScript> activeScripts = scriptRepository.findByProductIdAndIsActiveAndDeleted(productId, true, 0);

        int score = 100;
        StringBuilder message = new StringBuilder();

        if (scripts.isEmpty()) {
            score = 0;
            message.append("未生成任何话术");
        } else if (activeScripts.isEmpty()) {
            score = 60;
            message.append("已有 ").append(scripts.size()).append(" 个话术，但未激活任何话术");
        } else {
            message.append("已激活 ").append(activeScripts.size()).append(" 个话术，共 ").append(scripts.size()).append(" 个版本");
        }

        String status = score >= 80 ? "ok" : score >= 60 ? "warn" : "error";
        return new ReadinessItem("话术准备", status, message.toString(), score);
    }

    private ReadinessItem checkSellingPoints(DyProduct product) {
        int score = 100;
        StringBuilder issues = new StringBuilder();

        if (product.getAiSellingPoints() == null || product.getAiSellingPoints().isBlank()) {
            score -= 40;
            issues.append("缺少AI卖点提炼；");
        }
        if (product.getTags() == null || product.getTags().isBlank()) {
            score -= 30;
            issues.append("缺少产品标签；");
        }

        String status = score >= 80 ? "ok" : score >= 60 ? "warn" : "error";
        String message = score == 100 ? "卖点提炼完整" : "卖点信息不完整：" + issues.toString();

        return new ReadinessItem("卖点提炼", status, message, Math.max(0, score));
    }
}
