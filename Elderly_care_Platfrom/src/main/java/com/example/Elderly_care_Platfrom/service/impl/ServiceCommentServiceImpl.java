package com.example.Elderly_care_Platfrom.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Elderly_care_Platfrom.dao.MyCommentVO;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceComment;
import com.example.Elderly_care_Platfrom.entity.ServiceItem;
import com.example.Elderly_care_Platfrom.entity.ServiceOrder;
import com.example.Elderly_care_Platfrom.entity.ServiceProvider;
import com.example.Elderly_care_Platfrom.mapper.ServiceCommentMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceItemMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceOrderMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceProviderMapper;
import com.example.Elderly_care_Platfrom.service.IServiceCommentService;
import com.example.Elderly_care_Platfrom.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务评价表 服务实现类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
@Service
public class ServiceCommentServiceImpl extends ServiceImpl<ServiceCommentMapper, ServiceComment> implements IServiceCommentService {

    @Resource
    private ServiceOrderMapper serviceOrderMapper;
    @Resource
    private ServiceProviderMapper serviceProviderMapper;
    @Resource
    private ServiceItemMapper serviceItemMapper;

    @Override
    public Result createComment(ServiceComment comment) {
        Long userId = Long.valueOf(UserContext.get().userId());
        if (comment == null || comment.getOrderId() == null) {
            return Result.fail("参数不完整");
        }

        // 1. 订单须存在且归属当前家属(防评他人订单)
        ServiceOrder order = serviceOrderMapper.selectById(comment.getOrderId());
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            return Result.fail("无权操作");
        }

        // 2. 仅已完成(2)可评:待接单/服务中提示进行中,已取消不给评
        int status = order.getOrderStatus() == null ? 0 : order.getOrderStatus().intValue();
        if (status != 2) {
            return Result.fail(status == 3 ? "订单已取消，无法评价" : "服务完成后才能评价");
        }

        // 3. 评分 1~5 必填
        Integer score = comment.getScore();
        if (score == null || score < 1 || score > 5) {
            return Result.fail("评分不正确");
        }

        // 4. 内容选填:去空、限 500 字,空串归一 null(库可空)
        String content = StrUtil.trim(comment.getContent());
        if (content != null && content.length() > 500) {
            return Result.fail("评价内容过长");
        }

        // 5. 一单一评:应用层查重(DB 唯一索引 uk_order_id 兜并发双写)
        Long dup = count(new QueryWrapper<ServiceComment>().eq("order_id", order.getOrderId()));
        if (dup != null && dup > 0) {
            return Result.fail("该订单已评价");
        }

        // 6. 服务端定值:item/provider 从订单行取,userId 取 token,时间戳自动填充
        comment.setCommentId(null);
        comment.setItemId(order.getItemId());
        comment.setProviderId(order.getProviderId());
        comment.setUserId(userId);
        comment.setContent(StrUtil.isBlank(content) ? null : content);
        comment.setCreateTime(null);
        comment.setUpdateTime(null);

        if (!save(comment)) {
            return Result.fail("评价提交失败，请稍后重试");
        }
        return Result.ok(getById(comment.getCommentId()));
    }

    @Override
    public Result listMyComments() {
        Long userId = Long.valueOf(UserContext.get().userId());
        List<ServiceComment> comments = list(new QueryWrapper<ServiceComment>()
                .eq("user_id", userId)
                .orderByDesc("create_time"));
        List<MyCommentVO> result = new ArrayList<>(comments.size());
        if (comments.isEmpty()) {
            return Result.ok(result, 0L);
        }

        // 关联订单一次批量查出,再按订单取服务名/商家名(防 N+1)
        Set<Long> orderIds = new HashSet<>();
        for (ServiceComment c : comments) {
            if (c.getOrderId() != null) orderIds.add(c.getOrderId());
        }
        Map<Long, ServiceOrder> orderMap = orderIds.isEmpty() ? Collections.emptyMap()
                : toMap(serviceOrderMapper.selectBatchIds(orderIds), ServiceOrder::getOrderId);

        Set<Long> itemIds = new HashSet<>();
        Set<Long> providerIds = new HashSet<>();
        for (ServiceOrder o : orderMap.values()) {
            if (o.getItemId() != null) itemIds.add(o.getItemId());
            if (o.getProviderId() != null) providerIds.add(o.getProviderId());
        }
        Map<Long, ServiceItem> itemMap = itemIds.isEmpty() ? Collections.emptyMap()
                : toMap(serviceItemMapper.selectBatchIds(itemIds), ServiceItem::getItemId);
        Map<Long, ServiceProvider> providerMap = providerIds.isEmpty() ? Collections.emptyMap()
                : toMap(serviceProviderMapper.selectBatchIds(providerIds), ServiceProvider::getProviderId);

        for (ServiceComment c : comments) {
            ServiceOrder order = c.getOrderId() == null ? null : orderMap.get(c.getOrderId());
            ServiceItem item = order == null || order.getItemId() == null ? null : itemMap.get(order.getItemId());
            ServiceProvider provider = order == null || order.getProviderId() == null ? null : providerMap.get(order.getProviderId());

            MyCommentVO vo = new MyCommentVO();
            vo.setCommentId(c.getCommentId());
            vo.setOrderId(c.getOrderId());
            vo.setOrderNo(order == null ? null : order.getOrderNo());
            vo.setItemName(item == null ? null : item.getItemName());
            vo.setProviderName(provider == null ? null : provider.getProviderName());
            vo.setScore(c.getScore());
            vo.setContent(c.getContent());
            vo.setCreateTime(c.getCreateTime());
            result.add(vo);
        }
        return Result.ok(result, (long) result.size());
    }

    @Override
    public Result getProviderScore() {
        //商家身份:token userId 即 provider_id(登录后状态必为 1,同商家自助查资料口径)
        Long providerId = Long.valueOf(UserContext.get().userId());
        //评分口径与家属端同源:service_comment 实时平均;不读 service_item.score 冗余列(死列,从未聚合写入)
        List<ServiceComment> comments = list(new QueryWrapper<ServiceComment>()
                .eq("provider_id", providerId));
        Map<String, Object> data = new HashMap<>(4);
        if (comments.isEmpty()) {
            data.put("score", null);
            data.put("reviewCount", 0);
        } else {
            double avg = comments.stream().mapToInt(ServiceComment::getScore).average().orElse(0);
            data.put("score", BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
            data.put("reviewCount", comments.size());
        }
        return Result.ok(data);
    }

    /** 批量查出实体列表转 id -> 实体 map */
    private <T> Map<Long, T> toMap(List<T> list, Function<T, Long> keyFn) {
        return list.stream().collect(Collectors.toMap(keyFn, Function.identity()));
    }
}
