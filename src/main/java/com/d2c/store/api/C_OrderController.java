package com.d2c.store.api;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.exceptions.ApiException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.d2c.store.api.base.BaseController;
import com.d2c.store.api.handler.impl.OrderPromotionHandler;
import com.d2c.store.api.support.OrderRequestBean;
import com.d2c.store.common.api.Asserts;
import com.d2c.store.common.api.PageModel;
import com.d2c.store.common.api.Response;
import com.d2c.store.common.api.ResultCode;
import com.d2c.store.common.api.constant.PrefixConstant;
import com.d2c.store.common.utils.QueryUtil;
import com.d2c.store.common.utils.ReflectUtil;
import com.d2c.store.modules.member.model.AddressDO;
import com.d2c.store.modules.member.model.MemberDO;
import com.d2c.store.modules.member.service.AddressService;
import com.d2c.store.modules.order.model.CartItemDO;
import com.d2c.store.modules.order.model.OrderDO;
import com.d2c.store.modules.order.model.OrderItemDO;
import com.d2c.store.modules.order.query.OrderItemQuery;
import com.d2c.store.modules.order.query.OrderQuery;
import com.d2c.store.modules.order.service.CartItemService;
import com.d2c.store.modules.order.service.OrderItemService;
import com.d2c.store.modules.order.service.OrderService;
import com.d2c.store.modules.product.model.ProductDO;
import com.d2c.store.modules.product.model.ProductSkuDO;
import com.d2c.store.modules.product.service.ProductService;
import com.d2c.store.modules.product.service.ProductSkuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Cai
 */
@Api(description = "订单业务")
@RestController
@RequestMapping("/api/order")
public class C_OrderController extends BaseController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private AddressService addressService;
    @Autowired
    private CartItemService cartItemService;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductSkuService productSkuService;
    @Autowired
    private OrderPromotionHandler orderPromotionHandler;
    @Autowired
    private RedisTemplate redisTemplate;

    @ApiOperation(value = "立即结算")
    @RequestMapping(value = "/settle", method = RequestMethod.POST)
    public R<OrderDO> settle(@RequestBody OrderRequestBean orderRequest) {
        // 参数校验
        List<Long> cartIds = orderRequest.getCartIds();
        Long skuId = orderRequest.getSkuId();
        Integer quantity = orderRequest.getQuantity();
        if (cartIds == null && skuId == null && quantity == null) {
            throw new ApiException(ResultCode.REQUEST_PARAM_NULL);
        }
        // 登录用户
        MemberDO member = loginMemberHolder.getLoginMember();
        // 构建订单
        OrderDO order = this.initOrder(member);
        // 构建订单明细
        List<OrderItemDO> orderItemList = this.initOrderItemList(cartIds, skuId, quantity, member);
        order.setOrderItemList(orderItemList);
        // 处理订单促销
        orderPromotionHandler.operator(order);
        return Response.restResult(order, ResultCode.SUCCESS);
    }

    @ApiOperation(value = "创建订单")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public R<OrderDO> create(@RequestBody OrderRequestBean orderRequest) {
        // 参数校验
        List<Long> cartIds = orderRequest.getCartIds();
        Long skuId = orderRequest.getSkuId();
        Integer quantity = orderRequest.getQuantity();
        Long addressId = orderRequest.getAddressId();
        if (cartIds == null && skuId == null && quantity == null) {
            throw new ApiException(ResultCode.REQUEST_PARAM_NULL);
        }
        // 登录用户
        MemberDO member = loginMemberHolder.getLoginMember();
        // 防止重复提交
        try {
            Object doing = redisTemplate.opsForValue().get("C_CREATE_ORDER::" + member.getAccount());
            Asserts.isNull("您尚有正在处理中的订单，请勿重复操作", doing);
            redisTemplate.opsForValue().set("C_CREATE_ORDER::" + member.getAccount(), 1, 1, TimeUnit.MINUTES);
            // 收货地址
            AddressDO address = addressService.getById(addressId);
            Asserts.notNull("收货地址不能为空", address);
            Asserts.eq(address.getMemberId(), member.getId(), "收货地址不属于本人");
            // 构建订单
            OrderDO order = this.initOrder(address, member);
            // 构建订单明细
            List<OrderItemDO> orderItemList = this.initOrderItemList(cartIds, skuId, quantity, member);
            order.setOrderItemList(orderItemList);
            // 处理订单促销
            orderPromotionHandler.operator(order);
            // 创建订单
            order = orderService.doCreate(order);
            return Response.restResult(order, ResultCode.SUCCESS);
        } finally {
            redisTemplate.delete("C_CREATE_ORDER::" + member.getAccount());
        }
    }

    private OrderDO initOrder(MemberDO member) {
        OrderDO order = new OrderDO();
        order.setMemberId(member.getId());
        order.setMemberAccount(member.getAccount());
        order.setP2pId(member.getAccountInfo().getP2pId());
        order.setType(OrderDO.TypeEnum.NORMAL.name());
        order.setStatus(OrderDO.StatusEnum.WAIT_PAY.name());
        order.setProductAmount(BigDecimal.ZERO);
        order.setPayAmount(BigDecimal.ZERO);
        return order;
    }

    private OrderDO initOrder(AddressDO address, MemberDO member) {
        OrderDO order = new OrderDO();
        BeanUtil.copyProperties(address, order, ReflectUtil.clearPublicFields());
        Snowflake snowFlake = new Snowflake(1, 1);
        order.setSn(PrefixConstant.ORDER_PREFIX + String.valueOf(snowFlake.nextId()));
        order.setP2pId(member.getAccountInfo().getP2pId());
        order.setType(OrderDO.TypeEnum.NORMAL.name());
        order.setStatus(OrderDO.StatusEnum.WAIT_PAY.name());
        order.setProductAmount(BigDecimal.ZERO);
        order.setPayAmount(BigDecimal.ZERO);
        return order;
    }

    // 构建订单明细
    private List<OrderItemDO> initOrderItemList(List<Long> cartIds, Long skuId, Integer quantity, MemberDO member) {
        List<OrderItemDO> orderItemList = new ArrayList<>();
        if (cartIds != null && cartIds.size() > 0) {
            // 从购物车结算
            return this.buildCartOrderItems(cartIds);
        } else if (skuId != null && quantity != null) {
            // 从立即购买结算
            return this.buildBuyNowOrderItems(skuId, quantity, member);
        } else {
            throw new ApiException(ResultCode.REQUEST_PARAM_NULL);
        }
    }

    // 从购物车结算
    private List<OrderItemDO> buildCartOrderItems(List<Long> cartIds) {
        List<OrderItemDO> orderItemList = new ArrayList<>();
        List<CartItemDO> list = (List<CartItemDO>) cartItemService.listByIds(cartIds);
        List<Long> skuIds = new ArrayList<>();
        Map<Long, CartItemDO> map = new ConcurrentHashMap<>();
        for (CartItemDO cartItem : list) {
            skuIds.add(cartItem.getProductSkuId());
            map.put(cartItem.getProductSkuId(), cartItem);
        }
        Asserts.gt(skuIds.size(), 0, "购物车数据异常");
        List<ProductSkuDO> skuList = (List<ProductSkuDO>) productSkuService.listByIds(skuIds);
        for (ProductSkuDO sku : skuList) {
            if (map.get(sku.getId()) != null) {
                Asserts.ge(sku.getStock(), map.get(sku.getId()).getQuantity(), sku.getId() + "的SKU库存不足");
                OrderItemDO orderItem = this.initOrderItem(map, sku);
                this.buildOrderItem(sku, orderItem);
                orderItemList.add(orderItem);
            }
        }
        return orderItemList;
    }

    // 从立即购买结算
    private List<OrderItemDO> buildBuyNowOrderItems(Long skuId, Integer quantity, MemberDO member) {
        List<OrderItemDO> orderItemList = new ArrayList<>();
        Asserts.gt(quantity, 0, "数量必须大于0");
        ProductSkuDO sku = productSkuService.getById(skuId);
        Asserts.notNull(ResultCode.RESPONSE_DATA_NULL, sku);
        Asserts.ge(sku.getStock(), quantity, sku.getId() + "的SKU库存不足");
        ProductDO product = productService.getById(sku.getProductId());
        Asserts.notNull(ResultCode.RESPONSE_DATA_NULL, product);
        OrderItemDO orderItem = this.initOrderItem(quantity, member, sku, product);
        this.buildOrderItem(sku, orderItem);
        orderItemList.add(orderItem);
        return orderItemList;
    }

    private OrderItemDO initOrderItem(Map<Long, CartItemDO> map, ProductSkuDO sku) {
        OrderItemDO orderItem = new OrderItemDO();
        CartItemDO cartItem = map.get(sku.getId());
        BeanUtil.copyProperties(cartItem, orderItem, ReflectUtil.clearPublicFields());
        return orderItem;
    }

    private OrderItemDO initOrderItem(Integer quantity, MemberDO member, ProductSkuDO sku, ProductDO product) {
        OrderItemDO orderItem = new OrderItemDO();
        orderItem.setP2pId(member.getAccountInfo().getP2pId());
        orderItem.setMemberId(member.getId());
        orderItem.setMemberAccount(member.getAccount());
        orderItem.setProductId(sku.getProductId());
        orderItem.setProductSkuId(sku.getId());
        orderItem.setQuantity(quantity);
        orderItem.setStandard(sku.getStandard());
        orderItem.setProductName(product.getName());
        orderItem.setProductPic(product.getFirstPic());
        return orderItem;
    }

    private void buildOrderItem(ProductSkuDO sku, OrderItemDO orderItem) {
        orderItem.setType(OrderDO.TypeEnum.NORMAL.name());
        orderItem.setStatus(OrderItemDO.StatusEnum.WAIT_PAY.name());
        orderItem.setBrandId(sku.getBrandId());
        orderItem.setSupplierId(sku.getSupplierId());
        orderItem.setProductPrice(sku.getSellPrice());
        orderItem.setRealPrice(sku.getSellPrice());
        orderItem.setPayAmount(BigDecimal.ZERO);
    }

    @ApiOperation(value = "分页查询")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public R<Page<OrderDO>> list(PageModel page, OrderQuery query) {
        query.setMemberId(loginMemberHolder.getLoginId());
        Page<OrderDO> pager = (Page<OrderDO>) orderService.page(page, QueryUtil.buildWrapper(query));
        List<String> orderSns = new ArrayList<>();
        Map<String, OrderDO> orderMap = new ConcurrentHashMap<>();
        for (OrderDO order : pager.getRecords()) {
            orderSns.add(order.getSn());
            orderMap.put(order.getSn(), order);
        }
        if (orderSns.size() == 0) return Response.restResult(pager, ResultCode.SUCCESS);
        OrderItemQuery itemQuery = new OrderItemQuery();
        itemQuery.setOrderSn(orderSns.toArray(new String[0]));
        List<OrderItemDO> orderItemList = orderItemService.list(QueryUtil.buildWrapper(itemQuery));
        for (OrderItemDO orderItem : orderItemList) {
            if (orderMap.get(orderItem.getOrderSn()) != null) {
                orderMap.get(orderItem.getOrderSn()).getOrderItemList().add(orderItem);
            }
        }
        return Response.restResult(pager, ResultCode.SUCCESS);
    }

    @ApiOperation(value = "根据ID查询")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public R<OrderDO> select(@PathVariable Long id) {
        OrderDO order = orderService.getById(id);
        Asserts.notNull(ResultCode.RESPONSE_DATA_NULL, order);
        OrderItemQuery itemQuery = new OrderItemQuery();
        itemQuery.setOrderSn(new String[]{order.getSn()});
        List<OrderItemDO> orderItemList = orderItemService.list(QueryUtil.buildWrapper(itemQuery));
        order.getOrderItemList().addAll(orderItemList);
        return Response.restResult(order, ResultCode.SUCCESS);
    }

    @ApiOperation(value = "取消订单")
    @RequestMapping(value = "/cancel", method = RequestMethod.POST)
    public R cancel(Long id) {
        OrderDO order = orderService.getById(id);
        Asserts.notNull(ResultCode.RESPONSE_DATA_NULL, order);
        Asserts.eq(order.getStatus(), OrderDO.StatusEnum.WAIT_PAY.name(), "订单状态异常");
        MemberDO member = loginMemberHolder.getLoginMember();
        Asserts.eq(order.getMemberId(), member.getId(), "订单不属于本人");
        OrderItemQuery itemQuery = new OrderItemQuery();
        itemQuery.setOrderSn(new String[]{order.getSn()});
        List<OrderItemDO> orderItemList = orderItemService.list(QueryUtil.buildWrapper(itemQuery));
        order.setOrderItemList(orderItemList);
        orderService.doClose(order);
        return Response.restResult(null, ResultCode.SUCCESS);
    }

    @ApiOperation(value = "删除订单")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public R delete(Long id) {
        OrderDO order = orderService.getById(id);
        Asserts.notNull(ResultCode.RESPONSE_DATA_NULL, order);
        Asserts.eq(order.getStatus(), OrderDO.StatusEnum.CLOSED.name(), "订单状态异常");
        MemberDO member = loginMemberHolder.getLoginMember();
        Asserts.eq(order.getMemberId(), member.getId(), "订单不属于本人");
        orderService.doDelete(order.getSn());
        return Response.restResult(null, ResultCode.SUCCESS);
    }

}