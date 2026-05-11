package cn.gaifan.douyinOperations.module.product.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.product.vo.ProductCategorySearchVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductCategoryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 商品分类 Controller
 */
@RestController
@RequestMapping("/api/v1/product/category")
public class ProductCategoryController {

    @PostMapping("/search")
    public RESTResult<PageResultVO<ProductCategoryVO>> search(@Valid @RequestBody ProductCategorySearchVO vo) {
        vo.validateParams();
        return RESTResult.success(new PageResultVO<>(0L, Collections.emptyList(), vo.getPage(), vo.getRows()));
    }
}
