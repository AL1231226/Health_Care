package com.example.Elderly_care_Platfrom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * <p>
 * 管理员表
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-28
 */
@Getter
@Setter
@ToString
@TableName("admin")
public class Admin implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 管理员账号（唯一）
     */
    @TableField("username")
    private String username;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 角色(1用户 2管理员)
     */
    @TableField("role")
    private Byte role;

    /**
     * 账号状态(1正常 0禁用)
     */
    @TableField("status")
    private Byte status;
}
