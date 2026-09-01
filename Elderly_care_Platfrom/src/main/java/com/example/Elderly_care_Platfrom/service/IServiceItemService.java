package com.example.Elderly_care_Platfrom.service;

import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceItem;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务项目表（商家提供的具体服务，购物车加购单位） 服务类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
public interface IServiceItemService extends IService<ServiceItem> {

    /** 当前商家的服务列表：status 可选 0下架/1上架，不传查全部 */
    Result listItems(Integer status);

    /** 新增服务（归属取 token 商家，默认上架） */
    Result addItem(ServiceItem serviceItem);

    /** 删除服务（仅限本人） */
    Result deleteItem(Long id);

    /** 修改服务（仅限本人，评分/销量以库为准不可篡改） */
    Result updateItem(ServiceItem serviceItem);

    /** 服务详情（仅限本人） */
    Result getItem(Long id);

    /** 上下架切换：status=0 或 1 */
    Result toggleItemStatus(Long id, Integer status);
}
