package com.example.Elderly_care_Platfrom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Elderly_care_Platfrom.dao.CommentWithUserVO;
import com.example.Elderly_care_Platfrom.dao.ProviderDetailVO;
import com.example.Elderly_care_Platfrom.dao.ProviderWithItemsVO;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceCategory;
import com.example.Elderly_care_Platfrom.entity.ServiceComment;
import com.example.Elderly_care_Platfrom.entity.ServiceItem;
import com.example.Elderly_care_Platfrom.entity.ServiceProvider;
import com.example.Elderly_care_Platfrom.entity.SysUser;
import com.example.Elderly_care_Platfrom.mapper.ServiceCategoryMapper;
import com.example.Elderly_care_Platfrom.service.IServiceCategoryService;
import com.example.Elderly_care_Platfrom.service.IServiceCommentService;
import com.example.Elderly_care_Platfrom.service.IServiceItemService;
import com.example.Elderly_care_Platfrom.service.IServiceProviderService;
import com.example.Elderly_care_Platfrom.service.ISysUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务分类字典表 服务实现类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
@Service
public class ServiceCategoryServiceImpl extends ServiceImpl<ServiceCategoryMapper, ServiceCategory> implements IServiceCategoryService {

    @Resource
    private IServiceProviderService serviceProviderService;

    @Resource
    private IServiceItemService serviceItemService;

    @Resource
    private IServiceCommentService serviceCommentService;

    @Resource
    private ISysUserService sysUserService;

    @Override
    public Result listEnabledCategories() {
        // 只返回启用中的分类，按 sort 升序（前端首页按此顺序展示）
        QueryWrapper<ServiceCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1).orderByAsc("sort");
        List<ServiceCategory> categories = list(queryWrapper);
        return Result.ok(categories, (long) categories.size());
    }

    @Override
    public Result listCategoryProviders(Long categoryId) {
        if (categoryId == null) {
            return Result.fail("参数不完整");
        }
        if (getById(categoryId) == null) {
            return Result.fail("分类不存在");
        }
        // 该分类下正常(1)的商家，按入驻时间倒序
        QueryWrapper<ServiceProvider> providerWrapper = new QueryWrapper<>();
        providerWrapper.eq("category_id", categoryId)
                .eq("status", 1)
                .orderByDesc("create_time");
        List<ServiceProvider> providers = serviceProviderService.list(providerWrapper);
        if (providers.isEmpty()) {
            return Result.ok(Collections.emptyList(), 0L);
        }
        // 该分类下上架(1)的服务项目一次性查出，按商家分组（避免逐商家 N+1 查询）
        QueryWrapper<ServiceItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.eq("category_id", categoryId)
                .eq("status", 1)
                .orderByAsc("sort").orderByDesc("create_time");
        List<ServiceItem> items = serviceItemService.list(itemWrapper);
        Map<Long, List<ServiceItem>> itemsByProvider = items.stream()
                .collect(Collectors.groupingBy(ServiceItem::getProviderId));

        // 商家评价一次性查出，按商家分组：评分 = 该商家所有评价 score 的均分，条数 = 评价数
        List<Long> providerIds = providers.stream().map(ServiceProvider::getProviderId).collect(Collectors.toList());
        QueryWrapper<ServiceComment> commentWrapper = new QueryWrapper<>();
        commentWrapper.in("provider_id", providerIds);
        List<ServiceComment> comments = serviceCommentService.list(commentWrapper);
        Map<Long, List<ServiceComment>> commentsByProvider = comments.stream()
                .collect(Collectors.groupingBy(ServiceComment::getProviderId));

        List<ProviderWithItemsVO> voList = new ArrayList<>(providers.size());
        for (ServiceProvider provider : providers) {
            List<ServiceComment> providerComments = commentsByProvider.getOrDefault(provider.getProviderId(), Collections.emptyList());
            ProviderWithItemsVO vo = new ProviderWithItemsVO();
            vo.setProviderId(provider.getProviderId());
            vo.setProviderName(provider.getProviderName());
            vo.setLogo(provider.getLogo());
            vo.setIntro(provider.getIntro());
            vo.setDistrict(provider.getDistrict());
            vo.setAddress(provider.getAddress());
            vo.setReviewCount(providerComments.size());
            if (!providerComments.isEmpty()) {
                double avg = providerComments.stream().mapToInt(ServiceComment::getScore).average().orElse(0);
                vo.setScore(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
            }
            vo.setItems(itemsByProvider.getOrDefault(provider.getProviderId(), Collections.emptyList()));
            voList.add(vo);
        }
        return Result.ok(voList, (long) voList.size());
    }

    /** 校验商家详情可见（存在且 status=1 正常营业），返回 Result.fail 或 Result.ok(商家实体) */
    private Result checkProviderVisible(Long providerId) {
        if (providerId == null) {
            return Result.fail("参数不完整");
        }
        ServiceProvider provider = serviceProviderService.getById(providerId);
        // 待审核(0)/停用(2)与不存在同等对待，不泄露商家状态
        if (provider == null || provider.getStatus() == null || provider.getStatus() != 1) {
            return Result.fail("商家不存在");
        }
        return Result.ok(provider);
    }

    @Override
    public Result getProviderDetail(Long providerId) {
        Result check = checkProviderVisible(providerId);
        if (!check.getSuccess()) {
            return check;
        }
        ServiceProvider provider = (ServiceProvider) check.getData();

        // 主营分类名（字典查不到保持 null，前端兜底展示）
        String categoryName = null;
        ServiceCategory category = getById(provider.getCategoryId());
        if (category != null) {
            categoryName = category.getCategoryName();
        }

        // 名下全部上架(1)服务：不限于当前分类，sort 升序 + 创建时间倒序
        QueryWrapper<ServiceItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.eq("provider_id", providerId)
                .eq("status", 1)
                .orderByAsc("sort").orderByDesc("create_time");
        List<ServiceItem> items = serviceItemService.list(itemWrapper);

        // 该商家全部评价：评分均分（BigDecimal 1 位小数）+ 条数
        QueryWrapper<ServiceComment> commentWrapper = new QueryWrapper<>();
        commentWrapper.eq("provider_id", providerId);
        List<ServiceComment> comments = serviceCommentService.list(commentWrapper);

        ProviderDetailVO vo = new ProviderDetailVO();
        vo.setProviderId(provider.getProviderId());
        vo.setProviderName(provider.getProviderName());
        vo.setLogo(provider.getLogo());
        vo.setIntro(provider.getIntro());
        vo.setCategoryName(categoryName);
        vo.setLegalPerson(provider.getLegalPerson());
        // 电话完整返回（用户详情页联系商家用，不做脱敏）
        vo.setPhone(provider.getPhone());
        vo.setProvince(provider.getProvince());
        vo.setCity(provider.getCity());
        vo.setDistrict(provider.getDistrict());
        vo.setAddress(provider.getAddress());
        vo.setReviewCount(comments.size());
        if (!comments.isEmpty()) {
            double avg = comments.stream().mapToInt(ServiceComment::getScore).average().orElse(0);
            vo.setScore(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        }
        vo.setItems(items);
        return Result.ok(vo);
    }

    @Override
    public Result listProviderComments(Long providerId) {
        Result check = checkProviderVisible(providerId);
        if (!check.getSuccess()) {
            return check;
        }

        // 该商家全部评价，最新在前
        QueryWrapper<ServiceComment> commentWrapper = new QueryWrapper<>();
        commentWrapper.eq("provider_id", providerId).orderByDesc("create_time");
        List<ServiceComment> comments = serviceCommentService.list(commentWrapper);
        if (comments.isEmpty()) {
            return Result.ok(Collections.emptyList(), 0L);
        }

        // 评价人昵称一次性查出（避免逐条 N+1）
        List<Long> userIds = comments.stream()
                .map(ServiceComment::getUserId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u));

        // 被评服务名一次性查出
        List<Long> itemIds = comments.stream()
                .map(ServiceComment::getItemId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, ServiceItem> itemMap = itemIds.isEmpty() ? Collections.emptyMap()
                : serviceItemService.listByIds(itemIds).stream()
                        .collect(Collectors.toMap(ServiceItem::getItemId, i -> i));

        List<CommentWithUserVO> voList = new ArrayList<>(comments.size());
        for (ServiceComment comment : comments) {
            CommentWithUserVO vo = new CommentWithUserVO();
            vo.setCommentId(comment.getCommentId());
            vo.setUserId(comment.getUserId());
            SysUser user = userMap.get(comment.getUserId());
            vo.setUserName(user != null ? user.getUserName() : "匿名用户");
            vo.setItemId(comment.getItemId());
            ServiceItem item = itemMap.get(comment.getItemId());
            vo.setItemName(item != null ? item.getItemName() : null);
            vo.setScore(comment.getScore());
            vo.setContent(comment.getContent());
            vo.setCreateTime(comment.getCreateTime());
            voList.add(vo);
        }
        return Result.ok(voList, (long) voList.size());
    }
}
