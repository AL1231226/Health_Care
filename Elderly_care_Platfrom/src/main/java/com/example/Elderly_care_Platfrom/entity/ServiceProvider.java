package com.example.Elderly_care_Platfrom.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 商家表（服务商账号，归属 service_category 分类）
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
@Getter
@Setter
@ToString
@TableName("service_provider")
public class ServiceProvider implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商家ID
     */
    @TableId(value = "provider_id", type = IdType.AUTO)
    private Long providerId;

    /**
     * 主营服务分类 (FK -> service_category.category_id)
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 商家名称
     */
    @TableField("provider_name")
    private String providerName;

    /**
     * 商家 logo 图片路径
     */
    @TableField("logo")
    private String logo;

    /**
     * 商家简介
     */
    @TableField("intro")
    private String intro;

    /**
     * 手机号（登录账号）
     */
    @TableField("phone")
    private String phone;

    /**
     * 登录密码（加密存储）
     */
    @TableField("password")
    private String password;

    /**
     * 角色(3商家)
     */
    @TableField("role")
    private Byte role;

    /**
     * 负责人姓名
     */
    @TableField("legal_person")
    private String legalPerson;

    /**
     * 省（编码，与 user_address 一致）
     */
    @TableField("province")
    private String province;

    /**
     * 市（编码）
     */
    @TableField("city")
    private String city;

    /**
     * 区（编码）
     */
    @TableField("district")
    private String district;

    /**
     * 经度
     */
    @TableField("longitude")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @TableField("latitude")
    private BigDecimal latitude;

    /**
     * 详细经营地址
     */
    @TableField("address")
    private String address;

    /**
     * 状态（0待审核 1正常 2停用）
     */
    @TableField("status")
    private Byte status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
