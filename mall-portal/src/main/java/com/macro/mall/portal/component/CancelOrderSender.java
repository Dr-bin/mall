package com.macro.mall.portal.component;

import com.macro.mall.common.config.CircuitBreakerConfig;
import com.macro.mall.common.util.CircuitBreakerUtil;
import com.macro.mall.portal.domain.QueueEnum;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 取消订单消息的发送者（带断路器保护）
 * Created by macro on 2018/9/14.
 */
@Component
public class CancelOrderSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelOrderSender.class);
    @Autowired
    private AmqpTemplate amqpTemplate;
    @Autowired(required = false)
    private CircuitBreakerConfig circuitBreakerConfig;

    public void sendMessage(Long orderId, final long delayTimes){
        CircuitBreaker rabbitmqCircuitBreaker = circuitBreakerConfig != null ? 
            circuitBreakerConfig.rabbitmqCircuitBreaker() : null;
        
        if (rabbitmqCircuitBreaker != null) {
            CircuitBreakerUtil.execute(
                rabbitmqCircuitBreaker,
                () -> {
                    //给延迟队列发送消息
                    amqpTemplate.convertAndSend(QueueEnum.QUEUE_TTL_ORDER_CANCEL.getExchange(), 
                        QueueEnum.QUEUE_TTL_ORDER_CANCEL.getRouteKey(), orderId, new MessagePostProcessor() {
                            @Override
                            public Message postProcessMessage(Message message) throws AmqpException {
                                //给消息设置延迟毫秒值
                                message.getMessageProperties().setExpiration(String.valueOf(delayTimes));
                                return message;
                            }
                        });
                    LOGGER.info("send orderId:{}", orderId);
                },
                () -> {
                    // 降级策略：RabbitMQ不可用时，记录日志
                    // 实际项目中可以在这里将任务记录到数据库，由定时任务轮询处理
                    LOGGER.warn("RabbitMQ不可用，订单取消消息发送失败，orderId:{}，delayTimes:{}ms", orderId, delayTimes);
                    LOGGER.warn("建议：将订单ID:{}记录到待处理任务表，由定时任务处理", orderId);
                    // TODO: 可以在这里添加数据库记录逻辑
                    // 例如：orderTaskService.savePendingCancelOrder(orderId, delayTimes);
                }
            );
        } else {
            // 没有断路器配置时，使用原有逻辑
            amqpTemplate.convertAndSend(QueueEnum.QUEUE_TTL_ORDER_CANCEL.getExchange(), 
                QueueEnum.QUEUE_TTL_ORDER_CANCEL.getRouteKey(), orderId, new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        message.getMessageProperties().setExpiration(String.valueOf(delayTimes));
                        return message;
                    }
                });
            LOGGER.info("send orderId:{}", orderId);
        }
    }
}
