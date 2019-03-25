package com.d2c.store.modules.product.model;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.d2c.store.common.api.annotation.Assert;
import com.d2c.store.common.api.base.extension.BaseDelDO;
import com.d2c.store.common.api.emuns.AssertEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author BaiCai
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("P_PRODUCT")
@ApiModel(description = "商品表")
public class ProductDO extends BaseDelDO {

    @Assert(type = AssertEnum.NOT_NULL)
    @ApiModelProperty(value = "名称")
    private String name;
    @Assert(type = AssertEnum.NOT_NULL)
    @ApiModelProperty(value = "图片")
    private String pic;
    @Assert(type = AssertEnum.NOT_NULL)
    @ApiModelProperty(value = "销售价")
    private BigDecimal price;
    @Assert(type = AssertEnum.NOT_NULL)
    @ApiModelProperty(value = "商品库存")
    private Integer stock;
    @Assert(type = AssertEnum.NOT_NULL)
    @ApiModelProperty(value = "品牌ID")
    private Long brandId;
    @Assert(type = AssertEnum.NOT_NULL)
    @ApiModelProperty(value = "品类ID")
    private Long categoryId;
    @ApiModelProperty(value = "描述")
    private String description;
    @Assert(type = AssertEnum.NOT_NULL)
    @ApiModelProperty(value = "状态 1,0")
    private Integer status;
    @TableField(exist = false)
    @ApiModelProperty(value = "品牌")
    private BrandDO brand;
    @TableField(exist = false)
    @ApiModelProperty(value = "品类树")
    private ProductCategoryDO category;
    @TableField(exist = false)
    @ApiModelProperty(value = "商品的SKU列表")
    private List<ProductSkuDO> skuList = new ArrayList<>();

    public String getFirstPic() {
        if (StrUtil.isBlank(pic)) return null;
        return pic.split(",")[0];
    }

}
