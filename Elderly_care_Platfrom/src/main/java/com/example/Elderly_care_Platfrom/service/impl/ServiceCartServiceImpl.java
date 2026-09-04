package com.example.Elderly_care_Platfrom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Elderly_care_Platfrom.dao.CartItemVO;
import com.example.Elderly_care_Platfrom.dao.CheckoutRequest;
import com.example.Elderly_care_Platfrom.dao.OrderCheckoutVO;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceCart;
import com.example.Elderly_care_Platfrom.entity.ServiceItem;
import com.example.Elderly_care_Platfrom.entity.ServiceOrder;
import com.example.Elderly_care_Platfrom.entity.ServiceProvider;
import com.example.Elderly_care_Platfrom.mapper.ServiceCartMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceItemMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceProviderMapper;
import com.example.Elderly_care_Platfrom.service.IServiceCartService;
import com.example.Elderly_care_Platfrom.service.IServiceOrderService;
import com.example.Elderly_care_Platfrom.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 购物车表 服务实现类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-04
 */
@Service
public class ServiceCartServiceImpl extends ServiceImpl<ServiceCartMapper, ServiceCart> implements IServiceCartService {

    @Resource
    private ServiceItemMapper serviceItemMapper;
    @Resource
    private ServiceProviderMapper serviceProviderMapper;
    @Resource
    private IServiceOrderService serviceOrderService;

    @Override
    public Result add(ServiceCart cart) {
        Long userId = Long.valueOf(UserContext.get().userId());
        if (cart == null || cart.getItemId() == null || cart.getQuantity() == null) {
            return Result.fail("参数不完整");
        }
        int qty = cart.getQuantity();
        if (qty < 1 || qty > 99) {
            return Result.fail("数量不正确");
        }
        // 项目须存在且上架（下架/删除的项目不可加购）
        ServiceItem item = serviceItemMapper.selectById(cart.getItemId());
        if (item == null || item.getStatus() == null || item.getStatus().intValue() != 1) {
            return Result.fail("服务不存在或已下架");
        }

        // 同 (user,item) 一行：无行插新，有行数量累加（uk_cart_user_item 兜并发双写）
        ServiceCart row = getOne(new QueryWrapper<ServiceCart>()
                .eq("user_id", userId)
                .eq("item_id", cart.getItemId()));
        int newQty = qty;
        if (row == null) {
            row = new ServiceCart();
            row.setUserId(userId);
            row.setItemId(cart.getItemId());
        } else {
            newQty = (row.getQuantity() == null ? 0 : row.getQuantity()) + qty;
            if (newQty > 99) {
                return Result.fail("数量已达上限 99");
            }
        }
        row.setQuantity(newQty);
        saveOrUpdate(row);
        return Result.ok(getById(row.getCartId()));
    }

    @Override
    public Result listCart() {
        Long userId = Long.valueOf(UserContext.get().userId());
        List<ServiceCart> rows = list(new QueryWrapper<ServiceCart>()
                .eq("user_id", userId)
                .orderByAsc("create_time"));
        List<CartItemVO> result = new ArrayList<>(rows.size());
        if (rows.isEmpty()) {
            return Result.ok(result, 0L);
        }

        // item 一次批量查出（行已随项目删除级联清掉，此处仅需下架态判定），再按其 provider 批量联查商家名（防 N+1）
        Set<Long> itemIds = new HashSet<>();
        for (ServiceCart row : rows) {
            if (row.getItemId() != null) itemIds.add(row.getItemId());
        }
        Map<Long, ServiceItem> itemMap = toMap(serviceItemMapper.selectBatchIds(itemIds), ServiceItem::getItemId);
        Set<Long> providerIds = new HashSet<>();
        for (ServiceItem item : itemMap.values()) {
            if (item.getProviderId() != null) providerIds.add(item.getProviderId());
        }
        Map<Long, ServiceProvider> providerMap = providerIds.isEmpty() ? Collections.emptyMap()
                : toMap(serviceProviderMapper.selectBatchIds(providerIds), ServiceProvider::getProviderId);

        for (ServiceCart row : rows) {
            ServiceItem item = row.getItemId() == null ? null : itemMap.get(row.getItemId());
            ServiceProvider provider = item == null || item.getProviderId() == null ? null : providerMap.get(item.getProviderId());

            CartItemVO vo = new CartItemVO();
            vo.setCartId(row.getCartId());
            vo.setItemId(row.getItemId());
            vo.setItemName(item == null ? null : item.getItemName());
            vo.setPrice(item == null ? null : item.getPrice());
            vo.setUnit(item == null ? null : item.getUnit());
            vo.setQuantity(row.getQuantity());
            vo.setItemStatus(item == null || item.getStatus() == null ? null : item.getStatus().intValue());
            vo.setProviderId(item == null ? null : item.getProviderId());
            vo.setProviderName(provider == null ? null : provider.getProviderName());
            vo.setCreateTime(row.getCreateTime());
            result.add(vo);
        }
        return Result.ok(result, (long) result.size());
    }

    @Override
    public Result updateQuantity(Long cartId, Integer quantity) {
        Long userId = Long.valueOf(UserContext.get().userId());
        if (quantity == null || quantity < 1 || quantity > 99) {
            return Result.fail("数量不正确");
        }
        ServiceCart row = getById(cartId);
        if (row == null) {
            return Result.fail("购物车记录不存在");
        }
        if (!userId.equals(row.getUserId())) {
            return Result.fail("无权操作");
        }
        row.setQuantity(quantity);
        if (!updateById(row)) {
            return Result.fail("操作失败，请稍后重试");
        }
        return Result.ok();
    }

    @Override
    public Result remove(Long cartId) {
        Long userId = Long.valueOf(UserContext.get().userId());
        ServiceCart row = getById(cartId);
        if (row == null) {
            return Result.fail("购物车记录不存在");
        }
        if (!userId.equals(row.getUserId())) {
            return Result.fail("无权操作");
        }
        if (!removeById(cartId)) {
            return Result.fail("操作失败，请稍后重试");
        }
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result checkout(CheckoutRequest req) {
        Long userId = Long.valueOf(UserContext.get().userId());
        if (req == null || req.getCartIds() == null || req.getCartIds().isEmpty()) {
            return Result.fail("请选择要结算的服务");
        }

        // 1. 勾选行归属校验：非本人行/已删行一律整批拒绝（行数对不上即状态已变化）
        List<Long> cartIds = req.getCartIds().stream().distinct().collect(Collectors.toList());
        List<ServiceCart> rows = list(new QueryWrapper<ServiceCart>()
                .eq("user_id", userId)
                .in("cart_id", cartIds));
        if (rows.size() != cartIds.size()) {
            return Result.fail("购物车状态已变化，请刷新后重试");
        }
        Map<Long, ServiceCart> rowMap = toMap(rows, ServiceCart::getCartId);

        // 2. 逐行现查 item（防列表快照竞态）：不存在或非上架 → 整批拒绝并点名
        Set<Long> itemIds = new HashSet<>();
        for (ServiceCart row : rows) {
            if (row.getItemId() != null) itemIds.add(row.getItemId());
        }
        Map<Long, ServiceItem> itemMap = toMap(serviceItemMapper.selectBatchIds(itemIds), ServiceItem::getItemId);
        List<String> invalidNames = new ArrayList<>();
        for (ServiceCart row : rows) {
            ServiceItem item = row.getItemId() == null ? null : itemMap.get(row.getItemId());
            if (item == null || item.getStatus() == null || item.getStatus().intValue() != 1) {
                invalidNames.add(item == null ? "服务已删除" : item.getItemName());
            }
        }
        if (!invalidNames.isEmpty()) {
            return Result.fail("以下服务已下架或失效，请先在购物车移除：" + String.join("、", invalidNames));
        }

        // 3. 逐行生成订单：复用下单 createOrder 同源校验/单价总价快照/落库（无孤儿单，全部同事务）
        List<OrderCheckoutVO> vos = new ArrayList<>(cartIds.size());
        for (Long cartId : cartIds) {
            ServiceCart row = rowMap.get(cartId);
            ServiceOrder order = new ServiceOrder();
            order.setItemId(row.getItemId());
            order.setQuantity(row.getQuantity());
            order.setElderId(req.getElderId());
            order.setAddressId(req.getAddressId());
            order.setServiceTime(req.getServiceTime());
            order.setContactPhone(req.getContactPhone());
            order.setRemark(req.getRemark());
            Result orderResult = serviceOrderService.createOrder(order);
            if (!Boolean.TRUE.equals(orderResult.getSuccess())) {
                // 前面可能已有订单落库：标记回滚，外层事务整体回滚，不产生部分订单
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Result.fail(orderResult.getErrorMsg());
            }
            ServiceOrder created = (ServiceOrder) orderResult.getData();
            ServiceItem item = itemMap.get(row.getItemId());
            OrderCheckoutVO vo = new OrderCheckoutVO();
            vo.setOrderNo(created.getOrderNo());
            vo.setItemName(item == null ? null : item.getItemName());
            vo.setQuantity(created.getQuantity());
            vo.setTotalPrice(created.getTotalPrice());
            vos.add(vo);
        }

        // 4. 清除已结算购物车行（与订单同事务，任一步失败整体回滚）
        removeByIds(cartIds);
        return Result.ok(vos, (long) vos.size());
    }

    /** 批量查出实体列表转 id -> 实体 map */
    private <T> Map<Long, T> toMap(List<T> list, Function<T, Long> keyFn) {
        return list.stream().collect(Collectors.toMap(keyFn, Function.identity()));
    }
}
