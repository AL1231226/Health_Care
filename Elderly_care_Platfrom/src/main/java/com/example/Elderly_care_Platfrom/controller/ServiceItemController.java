package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.annotation.RequireRole;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ServiceItem;
import com.example.Elderly_care_Platfrom.service.IServiceItemService;
import com.example.Elderly_care_Platfrom.utils.RoleType;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 服务项目表（商家提供的具体服务，购物车加购单位） 前端控制器
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
@RestController
@RequestMapping("/service-item")
@RequireRole(RoleType.PROVIDER)
public class ServiceItemController {
    @Resource
    private IServiceItemService serviceItemService;

    /** 当前商家的服务列表：status 可选 0下架/1上架 */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) Integer status) {
        return serviceItemService.listItems(status);
    }

    /** 新增服务（归属取 token 商家） */
    @PostMapping("/add")
    public Result add(@RequestBody ServiceItem serviceItem) {
        return serviceItemService.addItem(serviceItem);
    }

    /** 删除服务（仅限本人） */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        return serviceItemService.deleteItem(id);
    }

    /** 修改服务（仅限本人） */
    @PutMapping("/update")
    public Result update(@RequestBody ServiceItem serviceItem) {
        return serviceItemService.updateItem(serviceItem);
    }

    /** 服务详情（仅限本人） */
    @GetMapping("/get/{id}")
    public Result get(@PathVariable Long id) {
        return serviceItemService.getItem(id);
    }

    /** 上下架切换：status=0 或 1 */
    @PutMapping("/status/{id}")
    public Result toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        return serviceItemService.toggleItemStatus(id, status);
    }

    /** 管理员端：全平台服务项目列表（status 可选 0下架/1上架；含商家名/分类名，方法级 ADMIN 覆盖类级 PROVIDER） */
    @RequireRole(RoleType.ADMIN)
    @GetMapping("/admin/list")
    public Result adminList(@RequestParam(required = false) Integer status) {
        return serviceItemService.listAdminItems(status);
    }

    /** 管理员端：任意服务上下架监督（status=0 或 1，无归属校验；方法级 ADMIN 覆盖类级 PROVIDER） */
    @RequireRole(RoleType.ADMIN)
    @PutMapping("/admin/status/{itemId}")
    public Result adminToggleStatus(@PathVariable Long itemId, @RequestParam Integer status) {
        return serviceItemService.toggleItemStatusByAdmin(itemId, status);
    }
}
