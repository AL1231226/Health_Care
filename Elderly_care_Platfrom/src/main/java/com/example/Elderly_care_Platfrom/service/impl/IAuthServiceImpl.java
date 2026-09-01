package com.example.Elderly_care_Platfrom.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.Elderly_care_Platfrom.dao.LoginRequest;
import com.example.Elderly_care_Platfrom.dao.ProviderRequest;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.entity.Admin;
import com.example.Elderly_care_Platfrom.entity.ServiceCategory;
import com.example.Elderly_care_Platfrom.entity.ServiceProvider;
import com.example.Elderly_care_Platfrom.entity.SysUser;
import com.example.Elderly_care_Platfrom.mapper.AdminMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceCategoryMapper;
import com.example.Elderly_care_Platfrom.mapper.ServiceProviderMapper;
import com.example.Elderly_care_Platfrom.mapper.SysUserMapper;
import com.example.Elderly_care_Platfrom.service.IAuthService;
import com.example.Elderly_care_Platfrom.utils.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.example.Elderly_care_Platfrom.utils.ValidationUtil.PASSWORD_REGEX;
import static com.example.Elderly_care_Platfrom.utils.ValidationUtil.PHONE_REGEX;

@Service
public class IAuthServiceImpl implements IAuthService {
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private ServiceProviderMapper serviceProviderMapper;
    @Resource
    private ServiceCategoryMapper serviceCategoryMapper;
    @Resource
    private AdminMapper adminMapper;
    @Override
    public Result familyLogin(LoginRequest loginRequest) {
        //家属入口只允许用户角色（1用户 2管理员）；常量在前比较，role 为空也不会空指针
        if(!"1".equals(loginRequest.getRole())){
            return Result.fail("角色错误");
        }

        // 校验格式
        Result validateResult = validate(loginRequest);
        if(!validateResult.getSuccess()){
            return validateResult;
        }
        //查询用户
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", loginRequest.getPhone());
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);
        if(sysUser==null){
            return Result.fail("用户不存在");
        }
        //加密存储则无法使用
        if(!loginRequest.getPassword().equals(sysUser.getPassword())){
            return Result.fail("密码错误");
        }
        if(sysUser.getStatus()==0){
            return Result.fail("用户已禁用");
        }

        //密码不返回给前端（响应体里不泄露密码字段）
        sysUser.setPassword(null);
        //生成token，与用户信息一起返回给前端
        String token = JwtUtil.generateToken(String.valueOf(sysUser.getId()), String.valueOf(sysUser.getRole()),sysUser.getUserName());
        return  Result.ok(Map.of("user", sysUser, "token", token));

    }

    @Override
    public Result familyRegister(LoginRequest loginRequest) {
        // 校验格式
        Result validateResult = validate(loginRequest);
        if(!validateResult.getSuccess()){
            return validateResult;
        }
        //查询用户
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", loginRequest.getPhone());
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);
        //插入用户
        if(sysUser==null){
            sysUser = new SysUser();
            sysUser.setPhone(loginRequest.getPhone());
            sysUser.setPassword(loginRequest.getPassword());
            sysUser.setUserName(loginRequest.getUserName());
            //家属自助注册固定为用户角色（1用户 2管理员），不信任前端传值
            sysUser.setRole((byte) 1);
            int result = sysUserMapper.insert(sysUser);
            if(!(result>0)){
                return Result.fail("注册失败");
            }
            //密码不返回给前端（响应体里不泄露密码字段）
            sysUser.setPassword(null);
            //生成token，与用户信息一起返回（前端注册后暂不使用，登录时使用）
            String token = JwtUtil.generateToken(String.valueOf(sysUser.getId()), String.valueOf(sysUser.getRole()),sysUser.getUserName());
            return Result.ok(Map.of("user", sysUser, "token", token));
        }

        
        return Result.fail("用户已存在");
    }

    @Override
    public Result providerLogin(LoginRequest loginRequest) {
        //商家入口只允许商家角色（3商家），常量在前比较，role 为空也不会空指针
        if(!"3".equals(loginRequest.getRole())){
            return Result.fail("角色错误");
        }

        // 校验格式（手机号/密码）
        Result validateResult = validate(loginRequest);
        if(!validateResult.getSuccess()){
            return validateResult;
        }
        //查询商家
        QueryWrapper<ServiceProvider> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", loginRequest.getPhone());
        ServiceProvider provider = serviceProviderMapper.selectOne(queryWrapper);
        if(provider==null){
            return Result.fail("商家不存在");
        }
        if(!loginRequest.getPassword().equals(provider.getPassword())){
            return Result.fail("密码错误");
        }
        //状态机：0待审核 1正常 2停用
        if(provider.getStatus()==0){
            return Result.fail("商家资料审核中，暂不可登录");
        }
        if(provider.getStatus()==2){
            return Result.fail("商家已停用");
        }

        //密码不返回给前端（响应体里不泄露密码字段）
        provider.setPassword(null);
        //生成token，与商家信息一起返回给前端
        String token = JwtUtil.generateToken(String.valueOf(provider.getProviderId()), String.valueOf(provider.getRole()), provider.getProviderName());
        return Result.ok(Map.of("user", provider, "token", token));
    }

    @Override
    public Result providerRegister(ProviderRequest registerRequest) {
        // 复用手机号/密码格式校验
        LoginRequest loginRequest = new LoginRequest(registerRequest.getPhone(), registerRequest.getPassword(), null, "3");
        Result validateResult = validate(loginRequest);
        if(!validateResult.getSuccess()){
            return validateResult;
        }
        if(StrUtil.isBlank(registerRequest.getProviderName())){
            return Result.fail("商家名称不能为空");
        }
        if(registerRequest.getCategoryId()==null){
            return Result.fail("请选择主营服务分类");
        }
        //主营分类必须真实存在
        ServiceCategory category = serviceCategoryMapper.selectById(registerRequest.getCategoryId());
        if(category==null){
            return Result.fail("主营服务分类不存在");
        }
        //手机号唯一（登录账号）
        QueryWrapper<ServiceProvider> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", registerRequest.getPhone());
        if(serviceProviderMapper.selectCount(queryWrapper) > 0){
            return Result.fail("该手机号已注册");
        }
        ServiceProvider provider = new ServiceProvider();
        provider.setPhone(registerRequest.getPhone());
        provider.setPassword(registerRequest.getPassword());
        provider.setProviderName(registerRequest.getProviderName());
        provider.setCategoryId(registerRequest.getCategoryId());
        //商家自助入驻固定为商家角色（3商家），不信任前端传值
        provider.setRole((byte) 3);
        //新入驻商家默认待审核（0待审核 1正常 2停用），审核通过前不可登录
        provider.setStatus((byte) 0);
        int result = serviceProviderMapper.insert(provider);
        if(!(result>0)){
            return Result.fail("注册失败");
        }
        //密码不返回给前端（响应体里不泄露密码字段）
        provider.setPassword(null);
        //生成token，与商家信息一起返回（前端注册后暂不使用，审核通过后登录时使用）
        String token = JwtUtil.generateToken(String.valueOf(provider.getProviderId()), String.valueOf(provider.getRole()), provider.getProviderName());
        return Result.ok(Map.of("user", provider, "token", token));
    }

    @Override
    public Result adminLogin(LoginRequest loginRequest) {
        //管理员入口只允许管理员角色（1用户 2管理员），常量在前比较，role 为空也不会空指针
        if(!"2".equals(loginRequest.getRole())){
            return Result.fail("角色错误");
        }
        //管理员账号是 username（非手机号），单独校验
        if(StrUtil.isBlank(loginRequest.getUserName())){
            return Result.fail("管理员账号不能为空");
        }
        if(StrUtil.isBlank(loginRequest.getPassword())){
            return Result.fail("密码不能为空");
        }
        //查询管理员
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", loginRequest.getUserName());
        Admin admin = adminMapper.selectOne(queryWrapper);
        if(admin==null){
            return Result.fail("管理员账号不存在");
        }
        if(!loginRequest.getPassword().equals(admin.getPassword())){
            return Result.fail("密码错误");
        }
        if(admin.getStatus()==0){
            return Result.fail("账号已禁用");
        }

        //密码不返回给前端（响应体里不泄露密码字段）
        admin.setPassword(null);
        //生成token，与管理员信息一起返回给前端
        String token = JwtUtil.generateToken(String.valueOf(admin.getId()), String.valueOf(admin.getRole()), admin.getUsername());
        return Result.ok(Map.of("user", admin, "token", token));
    }

    public Result validate(LoginRequest loginRequest) {
        //校验登录逻辑
        String phone = loginRequest.getPhone();
        String password = loginRequest.getPassword();
        if(StrUtil.isBlank(phone) || StrUtil.isBlank(password)){
            return Result.fail("用户名或密码不能为空");
        }
        if(!password.matches( PASSWORD_REGEX)){
            return Result.fail("密码格式不正确");
        }
        if(!phone.matches(PHONE_REGEX)){
            return Result.fail("手机号格式不正确");
        }
        return Result.ok();

    }
    
}
