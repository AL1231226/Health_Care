package com.example.Elderly_care_Platfrom.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Elderly_care_Platfrom.dao.MerchantOrderVO;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.dao.UserOrderVO;
import com.example.Elderly_care_Platfrom.entity.ElderProfile;
import com.example.Elderly_care_Platfrom.entity.ServiceComment;
import com.example.Elderly_care_Platfrom.entity.ServiceItem;
import com.example.Elderly_care_Platfrom.entity.ServiceOrder;
import com.example.Elderly_care_Platfrom.entity.ServiceProvider;
import com.example.Elderly_care_Platfrom.entity.SysUser;
import com.example.Elderly_care_Platfrom.entity.UserAddress;
import com.example.Elderly_care_Platfrom.mapper.ElderProfileMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceCommentMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceItemMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceOrderMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceProviderMapper;
import com.example.Elderly_care_Platfrom.mapper.SysUserMapper;
import com.example.Elderly_care_Platfrom.mapper.UserAddressMapper;
import com.example.Elderly_care_Platfrom.service.IServiceOrderService;
import com.example.Elderly_care_Platfrom.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.Elderly_care_Platfrom.utils.ValidationUtil.PHONE_REGEX;

/**
 * <p>
 * 服务订单表 服务实现类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
@Service
public class ServiceOrderServiceImpl extends ServiceImpl<ServiceOrderMapper, ServiceOrder> implements IServiceOrderService {

    @Resource
    private ElderProfileMapper elderProfileMapper;
    @Resource
    private ServiceItemMapper serviceItemMapper;
    @Resource
    private UserAddressMapper userAddressMapper;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private ServiceProviderMapper serviceProviderMapper;
    @Resource
    private ServiceCommentMapper serviceCommentMapper;

    @Override
    public Result createOrder(ServiceOrder order) {
        Long userId = Long.valueOf(UserContext.get().userId());

        // 1. 服务老人：必填且归属当前家属（多老人档案，防跨档案串单）
        if (order.getElderId() == null) {
            return Result.fail("请选择服务老人");
        }
        Long elderCount = elderProfileMapper.selectCount(new QueryWrapper<ElderProfile>()
                .eq("elder_id", order.getElderId())
                .eq("user_id", userId));
        if (elderCount == null || elderCount == 0) {
            return Result.fail("服务老人不存在");
        }

        // 2. 服务项目：须存在且上架中（下架/删除的项目不可再下单）
        if (order.getItemId() == null) {
            return Result.fail("参数不完整");
        }
        ServiceItem item = serviceItemMapper.selectById(order.getItemId());
        if (item == null || item.getStatus() == null || item.getStatus().intValue() != 1) {
            return Result.fail("服务不存在或已下架");
        }
        if (item.getPrice() == null) {
            return Result.fail("服务价格异常，请稍后再试");
        }

        // 3. 数量 1~99（与前端步进器一致）
        if (order.getQuantity() == null || order.getQuantity() < 1 || order.getQuantity() > 99) {
            return Result.fail("数量不正确");
        }

        // 4. 预约时间（可选）：传了须晚于当前时间
        if (order.getServiceTime() != null && !order.getServiceTime().after(new Date())) {
            return Result.fail("预约时间需晚于当前时间");
        }

        // 5. 服务地址（可选）：传了须归属当前用户，否则按未选处理
        if (order.getAddressId() != null) {
            Long addrCount = userAddressMapper.selectCount(new QueryWrapper<UserAddress>()
                    .eq("addr_id", order.getAddressId())
                    .eq("user_id", userId));
            if (addrCount == null || addrCount == 0) {
                order.setAddressId(null);
            }
        }

        // 6. 联系电话：空则默认取登录用户手机号，否则校验手机号格式
        String contactPhone = StrUtil.trim(order.getContactPhone());
        if (StrUtil.isBlank(contactPhone)) {
            SysUser user = sysUserMapper.selectById(userId);
            contactPhone = user == null ? null : user.getPhone();
        }
        if (StrUtil.isBlank(contactPhone)) {
            return Result.fail("联系电话不能为空");
        }
        if (!contactPhone.matches(PHONE_REGEX)) {
            return Result.fail("联系电话格式不正确");
        }

        // 7. 备注：去空、限长
        String remark = StrUtil.trim(order.getRemark());
        if (remark != null && remark.length() > 200) {
            return Result.fail("备注过长");
        }

        // ===== 服务端定值，不信任请求体 =====
        order.setOrderId(null);
        order.setUserId(userId);
        order.setProviderId(item.getProviderId());
        order.setUnitPrice(item.getPrice());
        order.setTotalPrice(item.getPrice().multiply(BigDecimal.valueOf(order.getQuantity())));
        order.setOrderStatus((byte) 0); // 0待接单
        order.setContactPhone(contactPhone);
        order.setRemark(remark);
        order.setOrderNo(genOrderNo());
        // 时间戳走 MyMetaObjectHandler 自动填充，清掉请求体可能带的脏值
        order.setCreateTime(null);
        order.setUpdateTime(null);

        boolean result = save(order);
        if (!result) {
            return Result.fail("下单失败，请稍后重试");
        }
        // 回查返回完整订单（含回填的 create_time 等），前端下单成功页展示
        return Result.ok(getById(order.getOrderId()));
    }

    /** 订单号：yyyyMMddHHmmss + 4 位随机数（业务编号，随机位防同秒冲突） */
    private String genOrderNo() {
        return DateUtil.format(new Date(), "yyyyMMddHHmmss")
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    @Override
    public Result listMerchantOrders(Integer status) {
        Long providerId = Long.valueOf(UserContext.get().userId());
        if (status != null && (status < 0 || status > 3)) {
            return Result.fail("状态参数不正确");
        }
        List<ServiceOrder> orders = list(new QueryWrapper<ServiceOrder>()
                .eq("provider_id", providerId)
                .eq(status != null, "order_status", status)
                .orderByDesc("create_time"));
        List<MerchantOrderVO> result = new ArrayList<>(orders.size());
        if (orders.isEmpty()) {
            return Result.ok(result, 0L);
        }

        // 关联字段一次批量查出（防 N+1）
        Set<Long> itemIds = new HashSet<>();
        Set<Long> elderIds = new HashSet<>();
        Set<Long> addressIds = new HashSet<>();
        for (ServiceOrder o : orders) {
            if (o.getItemId() != null) itemIds.add(o.getItemId());
            if (o.getElderId() != null) elderIds.add(o.getElderId());
            if (o.getAddressId() != null) addressIds.add(o.getAddressId());
        }
        Map<Long, ServiceItem> itemMap = toMap(serviceItemMapper.selectBatchIds(itemIds), ServiceItem::getItemId);
        Map<Long, ElderProfile> elderMap = toMap(elderProfileMapper.selectBatchIds(elderIds), ElderProfile::getElderId);
        Map<Long, UserAddress> addressMap = toMap(userAddressMapper.selectBatchIds(addressIds), UserAddress::getAddrId);

        for (ServiceOrder o : orders) {
            ServiceItem item = o.getItemId() == null ? null : itemMap.get(o.getItemId());
            ElderProfile elder = o.getElderId() == null ? null : elderMap.get(o.getElderId());
            UserAddress addr = o.getAddressId() == null ? null : addressMap.get(o.getAddressId());

            MerchantOrderVO vo = new MerchantOrderVO();
            vo.setOrderId(o.getOrderId());
            vo.setOrderNo(o.getOrderNo());
            vo.setOrderStatus(o.getOrderStatus() == null ? 0 : o.getOrderStatus().intValue());
            vo.setQuantity(o.getQuantity());
            vo.setUnitPrice(o.getUnitPrice());
            vo.setTotalPrice(o.getTotalPrice());
            vo.setServiceTime(o.getServiceTime());
            vo.setCreateTime(o.getCreateTime());
            vo.setContactPhone(o.getContactPhone());
            vo.setRemark(o.getRemark());
            vo.setItemName(item == null ? null : item.getItemName());
            vo.setElderId(elder == null ? null : elder.getElderId());
            vo.setElderName(elder == null ? null : elder.getElderName());
            vo.setGender(elder == null || elder.getGender() == null ? null : elder.getGender().intValue());
            vo.setBirthDate(elder == null ? null : elder.getBirthDate());
            vo.setElderPhone(elder == null ? null : elder.getPhone());
            vo.setHealthNote(elder == null ? null : elder.getHealthNote());
            // 服务地址全文（老人/家属常驻地址见档案，此处是订单所选的服务地址）
            vo.setAddressText(addressText(addr));
            result.add(vo);
        }
        return Result.ok(result, (long) result.size());
    }

    /** 批量查出实体列表转 id -> 实体 map */
    private <T> Map<Long, T> toMap(List<T> list, Function<T, Long> keyFn) {
        return list.stream().collect(Collectors.toMap(keyFn, Function.identity()));
    }

    /** 服务地址全文：省市区+详细，逐段去空拼接（Arrays.asList 允许空段） */
    private String addressText(UserAddress addr) {
        if (addr == null) {
            return null;
        }
        return Arrays.asList(addr.getProvince(), addr.getCity(), addr.getDistrict(), addr.getDetailAddr())
                .stream().filter(StrUtil::isNotBlank).collect(Collectors.joining());
    }

    @Override
    public Result listUserOrders(Integer status) {
        Long userId = Long.valueOf(UserContext.get().userId());
        if (status != null && (status < 0 || status > 3)) {
            return Result.fail("状态参数不正确");
        }
        List<ServiceOrder> orders = list(new QueryWrapper<ServiceOrder>()
                .eq("user_id", userId)
                .eq(status != null, "order_status", status)
                .orderByDesc("create_time"));
        List<UserOrderVO> result = new ArrayList<>(orders.size());
        if (orders.isEmpty()) {
            return Result.ok(result, 0L);
        }

        // 关联字段一次批量查出（防 N+1）
        Set<Long> providerIds = new HashSet<>();
        Set<Long> itemIds = new HashSet<>();
        Set<Long> elderIds = new HashSet<>();
        Set<Long> addressIds = new HashSet<>();
        for (ServiceOrder o : orders) {
            if (o.getProviderId() != null) providerIds.add(o.getProviderId());
            if (o.getItemId() != null) itemIds.add(o.getItemId());
            if (o.getElderId() != null) elderIds.add(o.getElderId());
            if (o.getAddressId() != null) addressIds.add(o.getAddressId());
        }
        Map<Long, ServiceProvider> providerMap = toMap(serviceProviderMapper.selectBatchIds(providerIds), ServiceProvider::getProviderId);
        Map<Long, ServiceItem> itemMap = toMap(serviceItemMapper.selectBatchIds(itemIds), ServiceItem::getItemId);
        Map<Long, ElderProfile> elderMap = toMap(elderProfileMapper.selectBatchIds(elderIds), ElderProfile::getElderId);
        Map<Long, UserAddress> addressMap = toMap(userAddressMapper.selectBatchIds(addressIds), UserAddress::getAddrId);
        // 订单已评记录一次批量查出（一单一评,order_id 唯一索引保证至多一条）
        List<ServiceComment> comments = serviceCommentMapper.selectList(
                new QueryWrapper<ServiceComment>().in("order_id", orders.stream()
                        .map(ServiceOrder::getOrderId).collect(Collectors.toList())));
        Map<Long, ServiceComment> commentMap = toMap(comments, ServiceComment::getOrderId);

        for (ServiceOrder o : orders) {
            ServiceProvider provider = o.getProviderId() == null ? null : providerMap.get(o.getProviderId());
            ServiceItem item = o.getItemId() == null ? null : itemMap.get(o.getItemId());
            ElderProfile elder = o.getElderId() == null ? null : elderMap.get(o.getElderId());
            UserAddress addr = o.getAddressId() == null ? null : addressMap.get(o.getAddressId());
            ServiceComment comment = commentMap.get(o.getOrderId());

            UserOrderVO vo = new UserOrderVO();
            vo.setOrderId(o.getOrderId());
            vo.setOrderNo(o.getOrderNo());
            vo.setOrderStatus(o.getOrderStatus() == null ? 0 : o.getOrderStatus().intValue());
            vo.setQuantity(o.getQuantity());
            vo.setUnitPrice(o.getUnitPrice());
            vo.setTotalPrice(o.getTotalPrice());
            vo.setServiceTime(o.getServiceTime());
            vo.setCreateTime(o.getCreateTime());
            vo.setContactPhone(o.getContactPhone());
            vo.setRemark(o.getRemark());
            vo.setItemName(item == null ? null : item.getItemName());
            vo.setProviderName(provider == null ? null : provider.getProviderName());
            vo.setElderId(elder == null ? null : elder.getElderId());
            vo.setElderName(elder == null ? null : elder.getElderName());
            vo.setAddressText(addressText(addr));
            vo.setCommentScore(comment == null || comment.getScore() == null ? null : comment.getScore());
            vo.setCommentContent(comment == null ? null : comment.getContent());
            result.add(vo);
        }
        return Result.ok(result, (long) result.size());
    }

    @Override
    public Result cancelOrder(Long orderId) {
        Long userId = Long.valueOf(UserContext.get().userId());
        ServiceOrder order = getById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            return Result.fail("无权操作");
        }
        // 仅 0待接单可取消（商家未接单，取消无成本）；接单/完成后需电话协商，不再走系统取消
        int current = order.getOrderStatus() == null ? 0 : order.getOrderStatus().intValue();
        if (current != 0) {
            return Result.fail(current == 1 ? "商家已接单，如需变动请电话联系商家"
                    : current == 2 ? "服务已完成，无法取消"
                    : "订单已取消，请勿重复操作");
        }
        boolean result = update(new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getOrderId, orderId)
                .set(ServiceOrder::getOrderStatus, (byte) 3));
        if (!result) {
            return Result.fail("操作失败，请稍后重试");
        }
        return Result.ok("订单已取消");
    }

    @Override
    public Result updateOrderStatus(Long orderId, Integer status) {
        if (status == null || (status != 1 && status != 2)) {
            return Result.fail("状态参数不正确");
        }
        Long providerId = Long.valueOf(UserContext.get().userId());
        ServiceOrder order = getById(orderId);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!providerId.equals(order.getProviderId())) {
            return Result.fail("无权操作");
        }
        // 只允许 0待接单→1服务中（接单）、1服务中→2已完成（完成服务），其余一律拒绝
        int current = order.getOrderStatus() == null ? 0 : order.getOrderStatus().intValue();
        if (!(status == 1 && current == 0) && !(status == 2 && current == 1)) {
            return Result.fail("当前状态不允许该操作");
        }
        boolean result = update(new LambdaUpdateWrapper<ServiceOrder>()
                .eq(ServiceOrder::getOrderId, orderId)
                .set(ServiceOrder::getOrderStatus, status.byteValue()));
        if (!result) {
            return Result.fail("操作失败，请稍后重试");
        }
        return Result.ok(status == 1 ? "已接单" : "服务已完成");
    }
}
