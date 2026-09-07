package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.annotation.RequireRole;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceComment;
import com.example.Elderly_care_Platfrom.service.IServiceCommentService;
import com.example.Elderly_care_Platfrom.utils.RoleType;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 服务评价表(用户对商家/服务项目的评价,商家评分由本表 score 聚合) 前端控制器
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
@RestController
@RequestMapping("/service-comment")
@RequireRole(RoleType.USER)
public class ServiceCommentController {

    @Resource
    private IServiceCommentService serviceCommentService;

    /** 家属端:发表评价(仅本人已完成订单,一单一评;item/provider 从订单行取,userId 取 token) */
    @PostMapping("/create")
    public Result create(@RequestBody ServiceComment serviceComment) {
        return serviceCommentService.createComment(serviceComment);
    }

    /** 家属端:我的全部评价(最新在前,关联订单号/服务名/商家名) */
    @GetMapping("/my")
    public Result my() {
        return serviceCommentService.listMyComments();
    }

    /** 商家端:本人店铺评分聚合(service_comment 实时平均;方法级 PROVIDER 覆盖类级 USER,家属/管理员打此端点拒「无权限」) */
    @RequireRole(RoleType.PROVIDER)
    @GetMapping("/provider/score")
    public Result providerScore() {
        return serviceCommentService.getProviderScore();
    }

    /** 商家端:本人店铺全部评价列表(最新在前,含订单号/服务名/家属昵称;方法级 PROVIDER,越权同拒「无权限」) */
    @RequireRole(RoleType.PROVIDER)
    @GetMapping("/provider/list")
    public Result providerComments() {
        return serviceCommentService.listMyProviderComments();
    }
}
