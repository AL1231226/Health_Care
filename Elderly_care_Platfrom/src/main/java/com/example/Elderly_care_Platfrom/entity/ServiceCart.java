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
 * 购物车表（家属跨商家加购；一行一服务项目，唯一(user_id,item_id)；结算按商家×项目拆单）
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-04
 */
@Getter
@Setter
@ToString
@TableName("service_cart")
public class ServiceCart implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 购物车行ID
     */
    @TableId(value = "cart_id", type = IdType.AUTO)
    private Long cartId;

    /**
     * 归属家属 (FK -> sys_user.id)
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 服务项目 (FK -> service_item.item_id，项目删除 ON DELETE CASCADE 自动清行)
     */
    @TableField("item_id")
    private Long itemId;

    /**
     * 份数(1~99，重复加购累加，上限 99 拒)
     */
    @TableField("quantity")
    private Integer quantity;

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
