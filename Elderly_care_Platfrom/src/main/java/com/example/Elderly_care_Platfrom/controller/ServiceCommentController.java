package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceComment;
import com.example.Elderly_care_Platfrom.service.IServiceCommentService;
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
}
