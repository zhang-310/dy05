package cn.gaifan.douyinOperations.module.product.service;

import cn.gaifan.douyinOperations.module.product.vo.ProductSaveVO;

import java.io.InputStream;
import java.util.List;

/**
 * 排品表 Excel 导入服务
 * 解析 paiping-260303.xlsx 等排品表格式，转换为 ProductSaveVO 列表
 * 支持提取嵌入图片并上传到 BOS，关联到对应产品行
 */
public interface PaipingImportService {

    /**
     * 从 Excel 输入流解析排品表，返回可导入的产品列表
     *
     * @param inputStream Excel 文件流
     * @param userId      当前用户 ID，用于图片上传路径；null 则跳过图片提取
     * @return 产品列表，可直接用于 ProductService.save
     */
    List<ProductSaveVO> parseFromExcel(InputStream inputStream, Long userId);
}
