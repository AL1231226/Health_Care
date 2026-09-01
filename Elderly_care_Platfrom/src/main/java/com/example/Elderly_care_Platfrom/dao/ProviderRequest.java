package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商家注册请求体（providerName / categoryId 为商家专属字段）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProviderRequest {
    private String phone;
    private String password;
    /** 商家名称 */
    private String providerName;
    /** 主营服务分类 ID (FK -> service_category) */
    private Long categoryId;
}
