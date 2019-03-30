package com.d2c.store.api;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.d2c.store.api.base.BaseController;
import com.d2c.store.common.api.Asserts;
import com.d2c.store.common.api.PageModel;
import com.d2c.store.common.api.Response;
import com.d2c.store.common.api.ResultCode;
import com.d2c.store.common.utils.QueryUtil;
import com.d2c.store.modules.product.model.BrandDO;
import com.d2c.store.modules.product.model.ProductDO;
import com.d2c.store.modules.product.model.ProductSkuDO;
import com.d2c.store.modules.product.query.ProductQuery;
import com.d2c.store.modules.product.query.ProductSkuQuery;
import com.d2c.store.modules.product.service.BrandService;
import com.d2c.store.modules.product.service.ProductService;
import com.d2c.store.modules.product.service.ProductSkuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Cai
 */
@Api(description = "商品业务")
@RestController
@RequestMapping("/api/product")
public class C_ProductController extends BaseController {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductSkuService productSkuService;
    @Autowired
    private BrandService brandService;

    @ApiOperation(value = "分页查询")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public R<Page<ProductDO>> list(PageModel page, ProductQuery query) {
        Page<ProductDO> pager = (Page<ProductDO>) productService.page(page, QueryUtil.buildWrapper(query, false));
        return Response.restResult(pager, ResultCode.SUCCESS);
    }

    @ApiOperation(value = "根据ID查询")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public R<ProductDO> select(@PathVariable Long id) {
        ProductDO product = productService.getById(id);
        Asserts.notNull(ResultCode.RESPONSE_DATA_NULL, product);
        BrandDO brand = brandService.getById(product.getBrandId());
        product.setBrand(brand);
        ProductSkuQuery query = new ProductSkuQuery();
        query.setProductId(id);
        List<ProductSkuDO> skuList = productSkuService.list(QueryUtil.buildWrapper(query));
        product.setSkuList(skuList);
        product.setStandard(this.groupStandard(skuList));
        return Response.restResult(product, ResultCode.SUCCESS);
    }

    // 规格分组
    private Map<String, List<JSONObject>> groupStandard(List<ProductSkuDO> skuList) {
        Map<String, List<JSONObject>> map = new LinkedHashMap<>();
        for (ProductSkuDO sku : skuList) {
            if (StrUtil.isNotBlank(sku.getStandard())) {
                JSONArray array = JSONArray.parseArray(sku.getStandard());
                for (int i = 0; i < array.size(); i++) {
                    JSONObject json = array.getJSONObject(i);
                    String name = json.getString("name");
                    String value = json.getString("value");
                    if (map.get(name) == null) {
                        map.put(name, new ArrayList<>());
                    }
                    if (map.get(name).stream().noneMatch(item -> value.equals(item.getString("value")))) {
                        map.get(name).add(json);
                    }
                }
            }
        }
        return map;
    }

}
