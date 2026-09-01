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
 * 服务项目表（商家提供的具体服务，购物车加购单位）
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-01
 */
@Getter
@Setter
@ToString
@TableName("service_item")
public class ServiceItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 服务项目ID
     */
    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;

    /**
     * 所属商家 (FK -> service_provider.provider_id)
     */
    @TableField("provider_id")
    private Long providerId;

    /**
     * 所属分类 (FK -> service_category.category_id)
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 服务名称（如：三菜一汤午餐）
     */
    @TableField("item_name")
    private String itemName;

    /**
     * 封面图路径
     */
    @TableField("cover")
    private String cover;

    /**
     * 单价（元）
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 计价单位（次/份/小时/2小时）
     */
    @TableField("unit")
    private String unit;

    /**
     * 服务时长（如 60分钟）
     */
    @TableField("duration")
    private String duration;

    /**
     * 服务详情描述
     */
    @TableField("detail")
    private String detail;

    /**
     * 平均评分（冗余，评价后聚合回填）
     */
    @TableField("score")
    private BigDecimal score;

    /**
     * 状态（0下架 1上架）
     */
    @TableField("status")
    private Byte status;

    /**
     * 销量（冗余）
     */
    @TableField("sales")
    private Integer sales;

    /**
     * 排序（越小越靠前）
     */
    @TableField("sort")
    private Integer sort;

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
