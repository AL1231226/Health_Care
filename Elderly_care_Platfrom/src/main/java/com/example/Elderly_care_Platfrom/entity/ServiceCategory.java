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
 * 服务分类字典表（首页五个服务模块）
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
@Getter
@Setter
@ToString
@TableName("service_category")
public class ServiceCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    @TableId(value = "category_id", type = IdType.AUTO)
    private Long categoryId;

    /**
     * 分类名称（助餐服务/助洁服务/助浴服务/助医服务/康复护理）
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 分类图标（图片 URL 或静态资源路径，暂未配图）
     */
    @TableField("icon")
    private String icon;

    /**
     * 排序（越小越靠前）
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 状态（0停用 1启用）
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
