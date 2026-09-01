package com.example.Elderly_care_Platfrom.service;

import com.example.Elderly_care_Platfrom.dao.LoginRequest;
import com.example.Elderly_care_Platfrom.dao.ProviderRequest;
import com.example.Elderly_care_Platfrom.dao.Result;

public interface IAuthService {
    Result familyLogin(LoginRequest loginRequest);

    Result familyRegister(LoginRequest loginRequest);

    /** 商家登录（role=3，service_provider 表） */
    Result providerLogin(LoginRequest loginRequest);

    /** 商家入驻（status=0 待审核） */
    Result providerRegister(ProviderRequest registerRequest);

    /** 管理员登录（role=2，admin 表，账号为 username） */
    Result adminLogin(LoginRequest loginRequest);
}
