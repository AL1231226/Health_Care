package com.example.Elderly_care_Platfrom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Elderly_care_Platfrom.dao.CommentWithUserVO;
import com.example.Elderly_care_Platfrom.dao.HotProviderVO;
import com.example.Elderly_care_Platfrom.dao.HotResultVO;
import com.example.Elderly_care_Platfrom.dao.ProviderDetailVO;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.dao.SearchItemVO;
import com.example.Elderly_care_Platfrom.dao.SearchResultVO;
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
import java.util.Comparator;
import java.util.HashMap;
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

        List<ProviderDetailVO> voList = new ArrayList<>(providers.size());
        for (ServiceProvider provider : providers) {
            List<ServiceComment> providerComments = commentsByProvider.getOrDefault(provider.getProviderId(), Collections.emptyList());
            ProviderDetailVO vo = new ProviderDetailVO();
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

    @Override
    public Result search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.fail("请输入搜索关键词");
        }
        String kw = keyword.trim();
        if (kw.length() > 30) {
            return Result.fail("搜索关键词过长，最多 30 字");
        }

        // 分类名命中（仅启用中的分类），「搜助餐」也能带出该分类下的商家
        QueryWrapper<ServiceCategory> categoryWrapper = new QueryWrapper<>();
        categoryWrapper.eq("status", 1).like("category_name", kw);
        List<Long> hitCategoryIds = list(categoryWrapper).stream()
                .map(ServiceCategory::getCategoryId)
                .collect(Collectors.toList());

        // 商家命中：正常(1)商家，名称/简介包含关键词或主营分类命中；入驻时间倒序
        QueryWrapper<ServiceProvider> providerWrapper = new QueryWrapper<>();
        providerWrapper.eq("status", 1)
                .and(w -> {
                    w.like("provider_name", kw).or().like("intro", kw);
                    if (!hitCategoryIds.isEmpty()) {
                        w.or().in("category_id", hitCategoryIds);
                    }
                })
                .orderByDesc("create_time");
        List<ServiceProvider> providers = serviceProviderService.list(providerWrapper);
        // 名称前缀命中优先（稳定排序，其余保持入驻时间倒序）
        providers.sort(Comparator.comparing((ServiceProvider p) ->
                !(p.getProviderName() != null && p.getProviderName().startsWith(kw))));

        // 服务命中：上架(1)服务名称包含关键词；创建时间倒序，名称前缀命中优先
        QueryWrapper<ServiceItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.eq("status", 1).like("item_name", kw).orderByDesc("create_time");
        List<ServiceItem> hitItems = serviceItemService.list(itemWrapper);
        hitItems.sort(Comparator.comparing((ServiceItem i) ->
                !(i.getItemName() != null && i.getItemName().startsWith(kw))));

        // 归属商家一次批量查出：停用/注销商家的服务不对外展示（与公开商家详情口径一致）
        List<Long> ownerIds = hitItems.stream()
                .map(ServiceItem::getProviderId).distinct().collect(Collectors.toList());
        Map<Long, ServiceProvider> ownerMap = ownerIds.isEmpty() ? Collections.emptyMap()
                : serviceProviderService.listByIds(ownerIds).stream()
                        .collect(Collectors.toMap(ServiceProvider::getProviderId, p -> p));
        List<SearchItemVO> items = new ArrayList<>();
        for (ServiceItem item : hitItems) {
            ServiceProvider owner = ownerMap.get(item.getProviderId());
            if (owner == null || owner.getStatus() == null || owner.getStatus() != 1) {
                continue;
            }
            SearchItemVO vo = new SearchItemVO();
            vo.setItemId(item.getItemId());
            vo.setItemName(item.getItemName());
            vo.setPrice(item.getPrice());
            vo.setUnit(item.getUnit());
            vo.setDuration(item.getDuration());
            vo.setScore(item.getScore());
            vo.setSales(item.getSales());
            vo.setProviderId(owner.getProviderId());
            vo.setProviderName(owner.getProviderName());
            items.add(vo);
        }

        SearchResultVO resultVO = new SearchResultVO();
        resultVO.setItems(items);
        if (providers.isEmpty()) {
            resultVO.setProviders(Collections.emptyList());
            return Result.ok(resultVO);
        }

        // 命中商家装配列表卡片（同 listCategoryProviders 形态，另填主营分类名供卡片标签展示）
        List<Long> providerIds = providers.stream()
                .map(ServiceProvider::getProviderId).collect(Collectors.toList());
        QueryWrapper<ServiceItem> providerItemWrapper = new QueryWrapper<>();
        providerItemWrapper.in("provider_id", providerIds)
                .eq("status", 1)
                .orderByAsc("sort").orderByDesc("create_time");
        Map<Long, List<ServiceItem>> itemsByProvider = serviceItemService.list(providerItemWrapper).stream()
                .collect(Collectors.groupingBy(ServiceItem::getProviderId));

        QueryWrapper<ServiceComment> commentWrapper = new QueryWrapper<>();
        commentWrapper.in("provider_id", providerIds);
        Map<Long, List<ServiceComment>> commentsByProvider = serviceCommentService.list(commentWrapper).stream()
                .collect(Collectors.groupingBy(ServiceComment::getProviderId));

        List<Long> categoryIds = providers.stream().map(ServiceProvider::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(ServiceCategory::getCategoryId, ServiceCategory::getCategoryName));

        List<ProviderDetailVO> providerVOs = new ArrayList<>(providers.size());
        for (ServiceProvider provider : providers) {
            List<ServiceComment> providerComments = commentsByProvider.getOrDefault(provider.getProviderId(), Collections.emptyList());
            ProviderDetailVO vo = new ProviderDetailVO();
            vo.setProviderId(provider.getProviderId());
            vo.setProviderName(provider.getProviderName());
            vo.setLogo(provider.getLogo());
            vo.setIntro(provider.getIntro());
            vo.setCategoryName(categoryNameMap.get(provider.getCategoryId()));
            vo.setDistrict(provider.getDistrict());
            vo.setAddress(provider.getAddress());
            vo.setReviewCount(providerComments.size());
            if (!providerComments.isEmpty()) {
                double avg = providerComments.stream().mapToInt(ServiceComment::getScore).average().orElse(0);
                vo.setScore(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
            }
            vo.setItems(itemsByProvider.getOrDefault(provider.getProviderId(), Collections.emptyList()));
            providerVOs.add(vo);
        }
        resultVO.setProviders(providerVOs);
        return Result.ok(resultVO);
    }

    /** 校验服务详情可见（存在、上架 status=1 且归属商家正常 status=1，口径同公开列表），返回 Result.fail 或 Result.ok(服务实体) */
    private Result checkItemVisible(Long itemId) {
        if (itemId == null) {
            return Result.fail("参数不完整");
        }
        ServiceItem item = serviceItemService.getById(itemId);
        // 不存在/已下架(0)与归属商家停用(2)同等对待，不泄露服务状态
        if (item == null || item.getStatus() == null || item.getStatus() != 1) {
            return Result.fail("服务不存在");
        }
        ServiceProvider owner = serviceProviderService.getById(item.getProviderId());
        if (owner == null || owner.getStatus() == null || owner.getStatus() != 1) {
            return Result.fail("服务不存在");
        }
        return Result.ok(item);
    }

    @Override
    public Result getItemDetail(Long itemId) {
        Result check = checkItemVisible(itemId);
        if (!check.getSuccess()) {
            return check;
        }
        ServiceItem item = (ServiceItem) check.getData();
        ServiceProvider owner = serviceProviderService.getById(item.getProviderId());
        SearchItemVO vo = new SearchItemVO();
        vo.setItemId(item.getItemId());
        vo.setItemName(item.getItemName());
        vo.setPrice(item.getPrice());
        vo.setUnit(item.getUnit());
        vo.setDuration(item.getDuration());
        vo.setDetail(item.getDetail());
        vo.setScore(item.getScore());
        vo.setSales(item.getSales());
        vo.setProviderId(owner.getProviderId());
        vo.setProviderName(owner.getProviderName());
        return Result.ok(vo);
    }

    @Override
    public Result listItemComments(Long itemId) {
        Result check = checkItemVisible(itemId);
        if (!check.getSuccess()) {
            return check;
        }
        ServiceItem item = (ServiceItem) check.getData();

        // 该服务全部评价，最新在前
        QueryWrapper<ServiceComment> commentWrapper = new QueryWrapper<>();
        commentWrapper.eq("item_id", itemId).orderByDesc("create_time");
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

        List<CommentWithUserVO> voList = new ArrayList<>(comments.size());
        for (ServiceComment comment : comments) {
            CommentWithUserVO vo = new CommentWithUserVO();
            vo.setCommentId(comment.getCommentId());
            vo.setUserId(comment.getUserId());
            SysUser user = userMap.get(comment.getUserId());
            vo.setUserName(user != null ? user.getUserName() : "匿名用户");
            vo.setItemId(item.getItemId());
            // 列表语境即该服务，被评服务名直接回填当前服务
            vo.setItemName(item.getItemName());
            vo.setScore(comment.getScore());
            vo.setContent(comment.getContent());
            vo.setCreateTime(comment.getCreateTime());
            voList.add(vo);
        }
        return Result.ok(voList, (long) voList.size());
    }

    @Override
    public Result listHot() {
        // ===== 热门服务：上架(1)服务按销量降序取前 8（同销量按 id 升序，保证排序稳定）=====
        QueryWrapper<ServiceItem> itemWrapper = new QueryWrapper<>();
        itemWrapper.eq("status", 1).orderByDesc("sales").orderByAsc("item_id");
        List<ServiceItem> rankedItems = serviceItemService.list(itemWrapper);
        List<ServiceItem> topItems = rankedItems.size() > 8 ? rankedItems.subList(0, 8) : rankedItems;

        // ===== 热门商家：正常(1)商家按名下上架服务销量总和降序取前 4（同销量保持自然顺序稳定）=====
        QueryWrapper<ServiceProvider> providerWrapper = new QueryWrapper<>();
        providerWrapper.eq("status", 1);
        List<ServiceProvider> providers = serviceProviderService.list(providerWrapper);
        // 名下上架服务销量之和一次算好（sales 为接活列，直读已完成订单数）
        Map<Long, Integer> providerSales = new HashMap<>();
        if (!providers.isEmpty()) {
            QueryWrapper<ServiceItem> salesWrapper = new QueryWrapper<>();
            salesWrapper.in("provider_id", providers.stream()
                            .map(ServiceProvider::getProviderId).collect(Collectors.toList()))
                    .eq("status", 1);
            for (ServiceItem item : serviceItemService.list(salesWrapper)) {
                providerSales.merge(item.getProviderId(),
                        item.getSales() == null ? 0 : item.getSales(), Integer::sum);
            }
        }
        providers.sort(Comparator.comparing((ServiceProvider p) ->
                -providerSales.getOrDefault(p.getProviderId(), 0)));
        List<ServiceProvider> topProviders = providers.size() > 4 ? providers.subList(0, 4) : providers;

        // 热门服务联所属商家名（一次批量，避免逐条 N+1）
        List<Long> itemOwnerIds = topItems.stream()
                .map(ServiceItem::getProviderId).distinct().collect(Collectors.toList());
        Map<Long, ServiceProvider> ownerMap = itemOwnerIds.isEmpty() ? Collections.emptyMap()
                : serviceProviderService.listByIds(itemOwnerIds).stream()
                        .collect(Collectors.toMap(ServiceProvider::getProviderId, p -> p));

        List<SearchItemVO> items = new ArrayList<>(topItems.size());
        for (ServiceItem item : topItems) {
            ServiceProvider owner = ownerMap.get(item.getProviderId());
            SearchItemVO vo = new SearchItemVO();
            vo.setItemId(item.getItemId());
            vo.setItemName(item.getItemName());
            vo.setPrice(item.getPrice());
            vo.setUnit(item.getUnit());
            vo.setDuration(item.getDuration());
            vo.setDetail(item.getDetail());
            vo.setScore(item.getScore());
            vo.setSales(item.getSales());
            vo.setProviderId(item.getProviderId());
            vo.setProviderName(owner != null ? owner.getProviderName() : null);
            items.add(vo);
        }

        // 热门商家卡片：评分/评价数实时聚合 + 主营分类名
        List<Long> topProviderIds = topProviders.stream()
                .map(ServiceProvider::getProviderId).collect(Collectors.toList());
        QueryWrapper<ServiceComment> commentWrapper = new QueryWrapper<>();
        commentWrapper.in("provider_id", topProviderIds);
        Map<Long, List<ServiceComment>> commentsByProvider = serviceCommentService.list(commentWrapper).stream()
                .collect(Collectors.groupingBy(ServiceComment::getProviderId));
        List<Long> categoryIds = topProviders.stream().map(ServiceProvider::getCategoryId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(ServiceCategory::getCategoryId, ServiceCategory::getCategoryName));

        List<HotProviderVO> providerVOs = new ArrayList<>(topProviders.size());
        for (ServiceProvider provider : topProviders) {
            List<ServiceComment> providerComments =
                    commentsByProvider.getOrDefault(provider.getProviderId(), Collections.emptyList());
            HotProviderVO vo = new HotProviderVO();
            vo.setProviderId(provider.getProviderId());
            vo.setProviderName(provider.getProviderName());
            vo.setLogo(provider.getLogo());
            vo.setIntro(provider.getIntro());
            vo.setCategoryName(categoryNameMap.get(provider.getCategoryId()));
            vo.setDistrict(provider.getDistrict());
            vo.setAddress(provider.getAddress());
            vo.setReviewCount(providerComments.size());
            if (!providerComments.isEmpty()) {
                double avg = providerComments.stream().mapToInt(ServiceComment::getScore).average().orElse(0);
                vo.setScore(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
            }
            vo.setTotalSales(providerSales.getOrDefault(provider.getProviderId(), 0));
            providerVOs.add(vo);
        }
        return Result.ok(new HotResultVO(items, providerVOs));
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
