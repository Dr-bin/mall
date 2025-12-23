package com.macro.mall.search.dao;

import com.macro.mall.search.domain.EsProduct;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 搜索商品管理自定义Dao
 * Created by macro on 2018/6/19.
 */
public interface EsProductDao {
    /**
     * 获取指定ID的搜索商品
     */
    List<EsProduct> getAllEsProductList(@Param("id") Long id);

    /**
     * 根据关键字、品牌ID、分类ID搜索商品（降级方案）
     */
    List<EsProduct> searchProducts(@Param("keyword") String keyword, 
                                   @Param("brandId") Long brandId, 
                                   @Param("productCategoryId") Long productCategoryId);
}
