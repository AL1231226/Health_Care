package com.example.Elderly_care_Platfrom.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.ElderProfile;
import com.example.Elderly_care_Platfrom.entity.ServiceOrder;
import com.example.Elderly_care_Platfrom.mapper.ElderProfileMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceOrderMapper;
import com.example.Elderly_care_Platfrom.service.IElderProfileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Elderly_care_Platfrom.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.Elderly_care_Platfrom.utils.ValidationUtil.ID_CARD_REGEX;
import static com.example.Elderly_care_Platfrom.utils.ValidationUtil.PHONE_REGEX;

/**
 * <p>
 * 老人档案表(归属于家属 sys_user) 服务实现类
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-31
 */
@Service
public class ElderProfileServiceImpl extends ServiceImpl<ElderProfileMapper, ElderProfile> implements IElderProfileService {

    @Resource
    private ServiceOrderMapper serviceOrderMapper;

    @Override
    public Result getAllElders() {
        // 按归属家属过滤，只查自己的老人档案
        QueryWrapper<ElderProfile> queryWrapper = new QueryWrapper<>();
        String userId = UserContext.get().userId();
        queryWrapper.eq("user_id", userId);
        List<ElderProfile> elders = list(queryWrapper);
        return Result.ok(elders, (long) elders.size());
    }

    @Override
    public Result addElder(ElderProfile elderProfile) {
        // 归属强制取当前登录用户（token 解析出的 userId），不信任请求体传值
        elderProfile.setUserId(Long.parseLong(UserContext.get().userId()));
        // 身份证选填：空串归一化为 null，否则 '' 会撞唯一约束 uk_elder_id_card（Duplicate entry ''）
        if (StrUtil.isBlank(elderProfile.getIdCard())) {
            elderProfile.setIdCard(null);
        }
        // 参数校验
        Result validateResult = validate(elderProfile);
        if (!validateResult.getSuccess()) {
            return validateResult;
        }
        // 身份证选填，非空才校验唯一（库里有唯一约束，先查一遍避免 500）
        if (StrUtil.isNotBlank(elderProfile.getIdCard())) {
            Result dupResult = checkIdCardUnique(elderProfile);
            if (!dupResult.getSuccess()) {
                return dupResult;
            }
        }
        boolean result = save(elderProfile);
        if (!result) {
            return Result.fail("添加档案失败");
        }
        // 返回实体（自增 elderId 已回填），前端可拿到新档案 id
        return Result.ok(elderProfile);
    }

    @Override
    public Result updateElder(ElderProfile elderProfile) {
        if (elderProfile.getElderId() == null) {
            return Result.fail("参数不完整");
        }
        // 归属校验：档案必须存在且属于当前 token 用户，不信任请求体传 userId
        ElderProfile exist = getById(elderProfile.getElderId());
        if (exist == null) {
            return Result.fail("档案不存在");
        }
        String userId = UserContext.get().userId();
        if (!userId.equals(exist.getUserId().toString())) {
            return Result.fail("无权操作");
        }
        // 归属字段以库里为准，防止请求体篡改
        elderProfile.setUserId(exist.getUserId());
        // 身份证选填：空串归一化为 null，避免 '' 撞唯一约束；同时标记本次是否要清空库里已有身份证
        boolean clearIdCard = StrUtil.isBlank(elderProfile.getIdCard());
        if (clearIdCard) {
            elderProfile.setIdCard(null);
        }
        // 身份证选填，非空才校验唯一（排除自己）
        if (StrUtil.isNotBlank(elderProfile.getIdCard())) {
            Result dupResult = checkIdCardUnique(elderProfile);
            if (!dupResult.getSuccess()) {
                return dupResult;
            }
        }

        boolean result = updateById(elderProfile);
        // updateById 默认跳过 null 字段，清空身份证需显式 set null
        if (result && clearIdCard) {
            result = update(new LambdaUpdateWrapper<ElderProfile>()
                    .eq(ElderProfile::getElderId, elderProfile.getElderId())
                    .set(ElderProfile::getIdCard, null));
        }
        if (!result) {
            return Result.fail("修改档案失败");
        }
        return Result.ok("修改档案成功");
    }

    @Override
    public Result deleteElder(Long elderId) {
        ElderProfile elderProfile = getById(elderId);
        if (elderProfile == null) {
            return Result.fail("档案不存在");
        }
        String userId = UserContext.get().userId();
        if (!userId.equals(elderProfile.getUserId().toString())) {
            return Result.fail("无权操作");
        }
        // 保护：该老人有进行中订单(0待接单/1服务中)时禁止删除，
        // 避免删档后商家端接单/服务中的订单失去老人健康信息；已完成/已取消可删（详情有「档案已删」兜底）
        Long activeCount = serviceOrderMapper.selectCount(new QueryWrapper<ServiceOrder>()
                .eq("elder_id", elderId)
                .in("order_status", 0, 1));
        if (activeCount != null && activeCount > 0) {
            return Result.fail("该老人存在进行中的订单，请先取消订单或等服务完成后再删除");
        }
        boolean result = removeById(elderId);
        if (!result) {
            return Result.fail("删除档案失败");
        }
        return Result.ok("删除档案成功");
    }

    // 参数校验（与前端表单一致，后端兜底）
    private Result validate(ElderProfile ep) {
        if (StrUtil.isBlank(ep.getElderName())) {
            return Result.fail("老人姓名不能为空");
        }
        if (ep.getGender() == null) {
            return Result.fail("性别不能为空");
        }
        if (ep.getBirthDate() == null) {
            return Result.fail("出生日期不能为空");
        }
        if (StrUtil.isNotBlank(ep.getIdCard()) && !ep.getIdCard().matches(ID_CARD_REGEX)) {
            return Result.fail("身份证号格式不正确");
        }
        if (StrUtil.isNotBlank(ep.getPhone()) && !ep.getPhone().matches(PHONE_REGEX)) {
            return Result.fail("老人联系电话格式不正确");
        }
        return Result.ok();
    }

    // 身份证号唯一校验：新增查全部，修改排除自己（表唯一约束的兜底，避免唯一冲突抛 500）
    // 调用方保证身份证非空才进入
    private Result checkIdCardUnique(ElderProfile ep) {
        QueryWrapper<ElderProfile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id_card", ep.getIdCard())
                .ne(ep.getElderId() != null, "elder_id", ep.getElderId());
        if (count(queryWrapper) > 0) {
            return Result.fail("该身份证号已存在");
        }
        return Result.ok();
    }
}
