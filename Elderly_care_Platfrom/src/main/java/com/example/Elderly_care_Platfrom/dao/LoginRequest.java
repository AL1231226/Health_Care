package com.example.Elderly_care_Platfrom.dao;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    private String phone;
    private String password;
    /**
     * 家属注册传 userName（昵称）；管理员登录传 username（账号）。
     * @JsonAlias 让 username 与 userName 都能绑定到本字段，原名字保留不受影响
     */
    @JsonAlias("username")
    private String userName;
    private String role;
}
