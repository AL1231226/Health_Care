package com.example.Elderly_care_Platfrom.controller;

import com.example.Elderly_care_Platfrom.dao.LoginRequest;
import com.example.Elderly_care_Platfrom.dao.ProviderRequest;
import com.example.Elderly_care_Platfrom.dao.Result;
import com.example.Elderly_care_Platfrom.service.IAuthService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/auth")
@RestController
public class AuthController {
    @Resource
    private IAuthService authService;
    @RequestMapping("/family/login")
    public Result familyLogin(@RequestBody LoginRequest loginRequest) {
        return authService.familyLogin(loginRequest);
    }
    @RequestMapping("/family/register")
    public Result familyRegister(@RequestBody LoginRequest loginRequest) {
        return authService.familyRegister(loginRequest);
    }
    @RequestMapping("/provider/login")
    public Result providerLogin(@RequestBody LoginRequest loginRequest) {
        return authService.providerLogin(loginRequest);
    }
    @RequestMapping("/provider/register")
    public Result providerRegister(@RequestBody ProviderRequest registerRequest) {
        return authService.providerRegister(registerRequest);
    }
    @RequestMapping("/admin/login")
    public Result adminLogin(@RequestBody LoginRequest loginRequest) {
        return authService.adminLogin(loginRequest);
    }
}
