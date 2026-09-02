package com.example.Elderly_care_Platfrom.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * <p>
 * 用户端「商家详情-评价列表」展示 VO：评价内容 + 评价人昵称 + 所评服务名
 * </p>
 * <p>
 * service_comment 只有 user_id / item_id 外键，需联 sys_user / service_item 才能展示
 * 评价人昵称与被评服务名称，故用 VO 承载组合结构返回前端
 * </p>
 *
 * @author 龙源lll
 * @since 2026-09-02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentWithUserVO {
    /**
     * 评价ID
     */
    private Long commentId;

    /**
     * 评价人ID
     */
    private Long userId;

    /**
     * 评价人昵称（用户已注销等查不到时兜底「匿名用户」）
     */
    private String userName;

    /**
     * 被评服务项目ID
     */
    private Long itemId;

    /**
     * 被评服务名称（可能为空，兼容无 item 的旧评价）
     */
    private String itemName;

    /**
     * 评分 1-5
     */
    private Integer score;

    /**
     * 评价内容
     */
    private String content;

    /**
     * 评价时间
     */
    private Date createTime;
}
