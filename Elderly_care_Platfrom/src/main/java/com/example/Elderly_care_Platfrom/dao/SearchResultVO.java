package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <p>
 * 用户端全局搜索结果 VO（GET /service-category/search）：一个 keyword 返回两组结果
 * </p>
 * <p>
 * items = 命中服务名（仅上架且归属商家正常，名称前缀命中优先）；
 * providers = 命中商家（正常商家名称/简介/主营分类任一包含关键词，名称前缀命中优先，
 * 卡片与分类下商家列表同构：基础信息 + 名下全部上架服务 + 评分/评价数聚合 + 主营分类名）
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultVO {
    /**
     * 命中服务（SearchItemVO 列表，均属正常商家，可点击跳商家详情）
     */
    private List<SearchItemVO> items;

    /**
     * 命中商家（ProviderDetailVO 列表，分类浏览页同款卡片形态）
     */
    private List<ProviderDetailVO> providers;
}
