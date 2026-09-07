package com.example.Elderly_care_Platfrom.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceComment;

/**
 * <p>
 * 服务评价表 服务类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
public interface IServiceCommentService extends IService<ServiceComment> {

    /**
     * 家属端:发表评价(仅本人已完成(2)订单可评,一单一评;itemId/providerId/userId 服务端定值,不信任请求体)
     */
    Result createComment(ServiceComment comment);

    /**
     * 家属端:我的全部评价(最新在前,关联订单号/服务名/商家名一次批量查出防 N+1)
     */
    Result listMyComments();

    /**
     * 商家端:本人店铺评分聚合(service_comment 实时平均 1 位小数;score 无评价为 null,reviewCount 评价条数)
     */
    Result getProviderScore();

    /**
     * 商家端:本人店铺全部评价列表(最新在前,关联订单号/被评服务名/家属昵称一次批量查出防 N+1)
     */
    Result listMyProviderComments();
}
