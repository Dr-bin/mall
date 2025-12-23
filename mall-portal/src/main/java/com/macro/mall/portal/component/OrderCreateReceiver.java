package com.macro.mall.portal.component;

import com.macro.mall.portal.domain.OrderCreateMessage;
import com.macro.mall.portal.domain.QueueEnum;
import com.macro.mall.portal.service.OmsPortalOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单创建消息的接收者（消费者）
 * 处理订单创建的实际业务逻辑
 * Created by macro on 2024/01/01.
 */
@Component
@RabbitListener(queues = "mall.order.create")
public class OrderCreateReceiver {
    // 注意：@RabbitListener的queues参数必须使用字符串常量，不能使用枚举
    // 这里使用"mall.order.create"与QueueEnum.QUEUE_ORDER_CREATE.getName()保持一致
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCreateReceiver.class);
    
    @Autowired
    private OmsPortalOrderService portalOrderService;

    /**
     * 处理订单创建消息
     * 
     * @param message 订单创建消息
     */
    @RabbitHandler
    public void handle(OrderCreateMessage message) {
        try {
            LOGGER.info("开始处理订单创建消息，requestId:{}", message.getRequestId());
            // 调用订单处理服务
            portalOrderService.processOrderCreate(message);
            LOGGER.info("订单创建消息处理完成，requestId:{}", message.getRequestId());
        } catch (Exception e) {
            LOGGER.error("订单创建消息处理失败，requestId:{}，错误信息:{}", message.getRequestId(), e.getMessage(), e);
            // 这里可以添加重试逻辑或死信队列处理
            throw e; // 抛出异常以便RabbitMQ进行重试
        }
    }
}

