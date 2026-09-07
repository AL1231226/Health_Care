package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.annotation.RequireRole;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.UserAddress;
import com.example.Elderly_care_Platfrom.service.IUserAddressService;
import com.example.Elderly_care_Platfrom.utils.RoleType;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户地址簿表(隶属于家属 sys_user) 前端控制器
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-31
 */
@RestController
@RequestMapping("/address")
@RequireRole(RoleType.USER)
public class UserAddressController {
    @Resource
    private IUserAddressService userAddressService;

    @PostMapping("/add")
    public Result addAddress(@RequestBody UserAddress userAddress) {
        return userAddressService.addAddress(userAddress);
    }

    @PutMapping("/update")
    public Result updateAddress(@RequestBody UserAddress userAddress) {
        return userAddressService.updateAddress(userAddress);
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteAddress(@PathVariable Long id) {
        return userAddressService.deleteAddress(id);
    }

    @GetMapping("/get/{id}")
    public Result getAddressById(@PathVariable Long id) {
        return userAddressService.getAddress(id);
    }

    @GetMapping("/gets")
    public Result getAllAddresses() {
        return userAddressService.getAllAddresses();
    }

    // 设为默认地址（事务：先清掉该用户其他默认，再把目标设为默认）
    @PutMapping("/set-default/{id}")
    public Result setDefault(@PathVariable Long id) {
        return userAddressService.setDefault(id);
    }

}
