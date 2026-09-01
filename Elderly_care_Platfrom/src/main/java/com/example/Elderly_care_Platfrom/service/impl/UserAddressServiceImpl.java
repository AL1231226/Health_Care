package com.example.Elderly_care_Platfrom.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.UserAddress;
import com.example.Elderly_care_Platfrom.mapper.UserAddressMapper;
import com.example.Elderly_care_Platfrom.service.IUserAddressService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Elderly_care_Platfrom.utils.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.Elderly_care_Platfrom.utils.ValidationUtil.PHONE_REGEX;

/**
 * <p>
 * 用户地址簿表(隶属于家属 sys_user) 服务实现类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-31
 */
@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements IUserAddressService {

    @Override
    public Result addAddress(UserAddress userAddress) {
        // 归属强制取当前登录用户（token 解析出的 userId），不信任请求体传值，防止替他人加地址
        userAddress.setUserId(Long.parseLong(UserContext.get().userId()));
        // 参数校验
        Result validateResult = validate(userAddress);
        if (!validateResult.getSuccess()) {
            return validateResult;
        }
        boolean result = save(userAddress);
        if (!result) {
            return Result.fail("添加地址失败");
        }
        // 返回实体（自增 addrId 已回填），前端可拿到新地址 id
        return Result.ok(userAddress);
    }

    @Override
    public Result updateAddress(UserAddress userAddress) {
        if (userAddress.getAddrId() == null) {
            return Result.fail("参数不完整");
        }
        // 归属校验：地址必须存在且属于当前 token 用户
        UserAddress exist = getById(userAddress.getAddrId());
        if (exist == null) {
            return Result.fail("地址不存在");
        }
        String userId = UserContext.get().userId();
        if (!userId.equals(exist.getUserId().toString())) {
            return Result.fail("无权操作");
        }
        // 归属字段以库里为准，防止请求体篡改
        userAddress.setUserId(exist.getUserId());

        boolean result = updateById(userAddress);
        if (!result) {
            return Result.fail("修改地址失败");
        }
        return Result.ok("修改地址成功");
    }

    @Override
    public Result deleteAddress(Long id) {
        if(UserContext.get() == null  ){
            return Result.fail("请先登录");
        }
        UserAddress userAddress = getById(id);
        if (userAddress == null) {
            return Result.fail("地址不存在");
        }
        String userId = UserContext.get().userId();
        if ( !userId.equals(userAddress.getUserId().toString())) {
            return Result.fail("无权操作");
        }
        boolean result = removeById(id);
        if (!result) {
            return Result.fail("删除地址失败");
        }
        return Result.ok("删除地址成功");
    }

    @Override
    public Result getAddress(Long id) {
        UserAddress userAddress = getById(id);
        if (userAddress == null) {
            return Result.fail("未查询到地址");
        }
        String userId = UserContext.get().userId();
        if ( !userId.equals(userAddress.getUserId().toString())) {
            return Result.fail("无权操作");
        }
        return Result.ok(userAddress);
    }

    @Override
    public Result getAllAddresses() {
        // 按归属家属过滤，只查自己的地址
        QueryWrapper<UserAddress> queryWrapper = new QueryWrapper<>();
        String userId = UserContext.get().userId();
        queryWrapper.eq("user_id", userId);
        List<UserAddress> userAddresses = list(queryWrapper);
        return Result.ok(userAddresses, (long) userAddresses.size());
    }

    @Override
    @Transactional
    public Result setDefault(Long addrId) {
        UserAddress target = getById(addrId);
        String userId = UserContext.get().userId();
        if (target == null || !(target.getUserId().toString()).equals(userId)) {
            return Result.fail("地址不存在或无权操作");
        }
        // 先清掉该用户所有默认，再把目标设为默认（同一事务内保证一致）
        update(new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, (byte) 0));
        update(new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getAddrId, addrId)
                .set(UserAddress::getIsDefault, (byte) 1));
        return Result.ok("设置默认成功");
    }

    // 参数校验（与前端表单一致，后端兜底）
    private Result validate(UserAddress ua) {
        if (ua.getUserId() == null) {
            return Result.fail("用户信息缺失");
        }
        if (StrUtil.isBlank(ua.getPhone())) {
            return Result.fail("联系电话不能为空");
        }
        if (!ua.getPhone().matches(PHONE_REGEX)) {
            return Result.fail("联系电话格式不正确");
        }
        if (StrUtil.isBlank(ua.getProvince()) || StrUtil.isBlank(ua.getCity()) || StrUtil.isBlank(ua.getDistrict())) {
            return Result.fail("省市区不能为空");
        }
        if (StrUtil.isBlank(ua.getDetailAddr())) {
            return Result.fail("详细地址不能为空");
        }
        return Result.ok();
    }
}
