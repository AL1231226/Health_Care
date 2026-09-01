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
 * 用户地址簿表(隶属于家属 sys_user)
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-31
 */
@Getter
@Setter
@ToString
@TableName("user_address")
public class UserAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 地址主键
     */
    @TableId(value = "addr_id", type = IdType.AUTO)
    private Long addrId;

    /**
     * 归属家属 (FK -> sys_user.id)
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 省
     */
    @TableField("province")
    private String province;

    /**
     * 市
     */
    @TableField("city")
    private String city;

    /**
     * 区/县
     */
    @TableField("district")
    private String district;

    /**
     * 详细地址(街道、楼栋、门牌号等)
     */
    @TableField("detail_addr")
    private String detailAddr;

    /**
     * 是否默认地址(0否 1是)
     */
    @TableField("is_default")
    private Byte isDefault;

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

    /**
     * 联系地址电话
     */
    @TableField("phone")
    private String phone;
}
