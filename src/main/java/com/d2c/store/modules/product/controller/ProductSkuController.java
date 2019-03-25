package com.d2c.store.modules.product.controller;

import com.d2c.store.common.api.base.BaseCtrl;
import com.d2c.store.modules.product.model.ProductSkuDO;
import com.d2c.store.modules.product.query.ProductSkuQuery;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author BaiCai
 */
@Api(description = "商品SKU管理")
@RestController
@RequestMapping("/back/product_sku")
public class ProductSkuController extends BaseCtrl<ProductSkuDO, ProductSkuQuery> {

}
