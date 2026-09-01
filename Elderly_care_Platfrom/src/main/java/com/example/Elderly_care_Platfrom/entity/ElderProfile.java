package com.example.Elderly_care_Platfrom.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 老人档案表(归属于家属 sys_user)
 * </p>
 *
 * @author 龙源lll
 * @since 2026-08-31
 */
@Getter
@Setter
@ToString
@TableName("elder_profile")
public class ElderProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 老人档案主键
     */
    @TableId(value = "elder_id", type = IdType.AUTO)
    private Long elderId;

    /**
     * 归属家属 (FK -> sys_user.id)
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 老人姓名
     */
    @TableField("elder_name")
    private String elderName;

    /**
     * 性别(1男 0女)
     */
    @TableField("gender")
    private Byte gender;

    /**
     * 出生日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @TableField("birth_date")
    private Date birthDate;

    /**
     * 身份证号(可空,唯一)
     */
    @TableField("id_card")
    private String idCard;

    /**
     * 老人联系电话(可空)
     */
    @TableField("phone")
    private String phone;

    /**
     * 健康状况/护理注意事项
     */
    @TableField("health_note")
    private String healthNote;

    /**
     * 常住地址 (FK -> user_address.addr_id, 可空)
     */
    @TableField("addr_id")
    private Long addrId;

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
