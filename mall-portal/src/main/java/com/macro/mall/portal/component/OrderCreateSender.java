package com.macro.mall.portal.component;

import com.macro.mall.common.config.CircuitBreakerConfig;
import com.macro.mall.common.util.CircuitBreakerUtil;
import com.macro.mall.portal.domain.OrderCreateMessage;
import com.macro.mall.portal.domain.QueueEnum;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单创建消息的发送者（生产者）
 * 使用生产者-消费者模式解耦订单创建流程
 * Created by macro on 2024/01/01.
 */
@Component
public class OrderCreateSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCreateSender.class);
    
    @Autowired
    private AmqpTemplate amqpTemplate;
    
    @Autowired(required = false)
    private CircuitBreakerConfig circuitBreakerConfig;

    /**
     * 发送订单创建消息到队列
     * 
     * @param message 订单创建消息
     */
    public void sendMessage(OrderCreateMessage message) {
        CircuitBreaker rabbitmqCircuitBreaker = circuitBreakerConfig != null ? 
            circuitBreakerConfig.rabbitmqCircuitBreaker() : null;
        
        if (rabbitmqCircuitBreaker != null) {
            CircuitBreakerUtil.execute(
                rabbitmqCircuitBreaker,
                () -> {
                    // 发送消息到订单创建队列
                    amqpTemplate.convertAndSend(
                        QueueEnum.QUEUE_ORDER_CREATE.getExchange(), 
                        QueueEnum.QUEUE_ORDER_CREATE.getRouteKey(), 
                        message
                    );
                    LOGGER.info("订单创建消息已发送到队列，requestId:{}", message.getRequestId());
                },
                () -> {
                    // 降级策略：RabbitMQ不可用时，记录日志
                    LOGGER.error("RabbitMQ不可用，订单创建消息发送失败，requestId:{}", message.getRequestId());
                    LOGGER.warn("建议：将订单创建请求记录到数据库，由定时任务处理");
                    // TODO: 可以在这里添加数据库记录逻辑
                    // 例如：orderTaskService.savePendingOrderCreate(message);
                }
            );
        } else {
            // 没有断路器配置时，使用原有逻辑
            amqpTemplate.convertAndSend(
                QueueEnum.QUEUE_ORDER_CREATE.getExchange(), 
                QueueEnum.QUEUE_ORDER_CREATE.getRouteKey(), 
                message
            );
            LOGGER.info("订单创建消息已发送到队列，requestId:{}", message.getRequestId());
        }
    }
}


