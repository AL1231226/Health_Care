package com.example.Elderly_care_Platfrom.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceItem;
import com.example.Elderly_care_Platfrom.mapper.ServiceItemMapper;
import com.example.Elderly_care_Platfrom.service.IServiceItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Elderly_care_Platfrom.utils.UserContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 服务项目表（商家提供的具体服务，购物车加购单位） 服务实现类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
@Service
public class ServiceItemServiceImpl extends ServiceImpl<ServiceItemMapper, ServiceItem> implements IServiceItemService {

    /** 当前登录商家的 providerId（商家登录 token 的 userId 即 provider_id） */
    private Long currentProviderId() {
        return Long.parseLong(UserContext.get().userId());
    }

    // 参数校验（与前端表单一致，后端兜底）
    private Result validate(ServiceItem item) {
        if (StrUtil.isBlank(item.getItemName())) {
            return Result.fail("服务名称不能为空");
        }
        if (item.getCategoryId() == null) {
            return Result.fail("请选择服务分类");
        }
        if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail("价格不正确");
        }
        if (StrUtil.isBlank(item.getUnit())) {
            return Result.fail("计价单位不能为空");
        }
        return Result.ok();
    }

    @Override
    public Result listItems(Integer status) {
        // 只查当前商家的服务（token 解析 providerId，不信任任何请求参数）
        QueryWrapper<ServiceItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("provider_id", currentProviderId())
                .eq(status != null, "status", status)
                .orderByAsc("sort")
                .orderByDesc("create_time");
        List<ServiceItem> serviceItems = list(queryWrapper);
        return Result.ok(serviceItems, (long) serviceItems.size());
    }

    @Override
    public Result addItem(ServiceItem serviceItem) {
        Result valid = validate(serviceItem);
        if (!valid.getSuccess()) {
            return valid;
        }
        // 归属强制取当前登录商家（token），不信任请求体传 providerId
        serviceItem.setProviderId(currentProviderId());
        // 聚合字段禁止商家写入：评分/销量由评价、订单回填
        serviceItem.setScore(null);
        serviceItem.setSales(0);
        // 默认上架，sort=0 排最前
        if (serviceItem.getStatus() == null) {
            serviceItem.setStatus((byte) 1);
        }
        if (serviceItem.getSort() == null) {
            serviceItem.setSort(0);
        }
        boolean result = save(serviceItem);
        if (!result) {
            return Result.fail("添加服务失败");
        }
        // 返回实体（自增 itemId 已回填）
        return Result.ok(serviceItem);
    }

    @Override
    public Result deleteItem(Long id) {
        ServiceItem serviceItem = getById(id);
        if (serviceItem == null) {
            return Result.fail("服务不存在");
        }
        if (!currentProviderId().equals(serviceItem.getProviderId())) {
            return Result.fail("无权操作");
        }
        boolean result = removeById(id);
        if (!result) {
            return Result.fail("删除服务失败");
        }
        return Result.ok("删除成功");
    }

    @Override
    public Result updateItem(ServiceItem serviceItem) {
        if (serviceItem.getItemId() == null) {
            return Result.fail("参数不完整");
        }
        // 归属校验：服务必须存在且属于当前商家
        ServiceItem exist = getById(serviceItem.getItemId());
        if (exist == null) {
            return Result.fail("服务不存在");
        }
        if (!currentProviderId().equals(exist.getProviderId())) {
            return Result.fail("无权操作");
        }
        Result valid = validate(serviceItem);
        if (!valid.getSuccess()) {
            return valid;
        }
        // 归属以库为准；评分/销量为聚合字段，置 null 跳过更新（updateById 默认跳过 null），防请求体篡改
        serviceItem.setProviderId(exist.getProviderId());
        serviceItem.setScore(null);
        serviceItem.setSales(null);
        boolean result = updateById(serviceItem);
        if (!result) {
            return Result.fail("修改服务失败");
        }
        return Result.ok("修改成功");
    }

    @Override
    public Result getItem(Long id) {
        ServiceItem serviceItem = getById(id);
        if (serviceItem == null) {
            return Result.fail("服务不存在");
        }
        if (!currentProviderId().equals(serviceItem.getProviderId())) {
            return Result.fail("无权操作");
        }
        return Result.ok(serviceItem);
    }

    @Override
    public Result toggleItemStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.fail("状态参数不正确");
        }
        ServiceItem serviceItem = getById(id);
        if (serviceItem == null) {
            return Result.fail("服务不存在");
        }
        if (!currentProviderId().equals(serviceItem.getProviderId())) {
            return Result.fail("无权操作");
        }
        boolean result = update(new LambdaUpdateWrapper<ServiceItem>()
                .eq(ServiceItem::getItemId, id)
                .set(ServiceItem::getStatus, status.byteValue()));
        if (!result) {
            return Result.fail("操作失败");
        }
        return Result.ok(status == 1 ? "已上架" : "已下架");
    }
}
