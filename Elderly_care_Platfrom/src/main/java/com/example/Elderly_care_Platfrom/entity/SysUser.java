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
import java.util.Date;

/**
 * <p>
 * 家属表
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-28
 */
@Getter
@Setter
@ToString
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;



    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 手机号（登录账号，唯一）
     */
    @TableField("phone")
    private String phone;

    /**
     * 登录密码（BCrypt加密存储）
     */
    @TableField("password")
    private String password;

    /**
     * 昵称
     */
    @TableField("user_name")
    private String userName;

    /**
     * 账号状态(1正常 0禁用)
     */
    @TableField("status")
    private Byte status;

    /**
     * 角色(1用户 2管理员)
     */
    @TableField("role")
    private Byte role;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
