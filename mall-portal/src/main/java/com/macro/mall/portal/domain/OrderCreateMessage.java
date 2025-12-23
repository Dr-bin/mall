package com.macro.mall.portal.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单创建消息DTO
 * 用于生产者-消费者模式传递订单创建信息
 * Created by macro on 2024/01/01.
 */
@Data
public class OrderCreateMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 订单创建请求ID（用于追踪订单处理状态）
     */
    private String requestId;

    /**
     * 用户ID
     */
    private Long memberId;

    /**
     * 收货地址ID
     */
    private Long memberReceiveAddressId;

    /**
     * 优惠券ID
     */
    private Long couponId;

    /**
     * 使用的积分数
     */
    private Integer useIntegration;

    /**
     * 支付方式
     */
    private Integer payType;

    /**
     * 被选中的购物车商品ID列表
     */
    private List<Long> cartIds;

    /**
     * 创建时间戳
     */
    private Long createTime;
}



