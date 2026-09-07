package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * <p>
 * 用户端首页「热门推荐」结果 VO（GET /service-category/hot）：
 * 销量 Top8 服务 + 销量 Top4 商家一组接口返回
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-07
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotResultVO {
    /**
     * 热门服务（上架服务按销量降序取 8，同销量按 id 升序稳定）
     */
    private List<SearchItemVO> items;

    /**
     * 热门商家（正常商家按名下上架服务销量总和降序取 4）
     */
    private List<HotProviderVO> providers;
}
