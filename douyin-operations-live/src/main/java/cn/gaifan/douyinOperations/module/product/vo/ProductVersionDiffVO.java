package cn.gaifan.douyinOperations.module.product.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 产品话术版本差异 VO（行级 added/removed/changed）
 * 用于 diffVersions 接口返回
 */
public class ProductVersionDiffVO {
    public List<String> added = new ArrayList<>();
    public List<String> removed = new ArrayList<>();
    public List<String> changed = new ArrayList<>();
}
