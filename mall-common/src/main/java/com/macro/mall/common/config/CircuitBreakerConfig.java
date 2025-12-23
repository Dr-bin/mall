package com.macro.mall.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 断路器配置类
 * Created for Circuit Breaker Pattern implementation
 */
@Configuration
public class CircuitBreakerConfig {

    /**
     * 配置 Elasticsearch 断路器
     */
    @Bean("elasticsearchCircuitBreaker")
    public CircuitBreaker elasticsearchCircuitBreaker() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                // 失败率阈值，超过50%失败率时打开断路器
                .failureRateThreshold(50)
                // 等待时间，断路器打开后等待60秒后进入半开状态
                .waitDurationInOpenState(Duration.ofSeconds(60))
                // 滑动窗口大小
                .slidingWindowSize(10)
                // 最小调用次数，至少调用5次后才计算失败率
                .minimumNumberOfCalls(5)
                // 半开状态下允许的调用次数
                .permittedNumberOfCallsInHalfOpenState(3)
                // 慢调用率阈值，超过50%的调用超过2秒认为是慢调用
                .slowCallRateThreshold(50)
                // 慢调用时间阈值
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                // 记录异常
                .recordExceptions(Exception.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        return registry.circuitBreaker("elasticsearch", config);
    }

    /**
     * 配置 Redis 断路器
     */
    @Bean("redisCircuitBreaker")
    public CircuitBreaker redisCircuitBreaker() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofSeconds(1))
                .recordExceptions(Exception.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        return registry.circuitBreaker("redis", config);
    }

    /**
     * 配置 RabbitMQ 断路器
     */
    @Bean("rabbitmqCircuitBreaker")
    public CircuitBreaker rabbitmqCircuitBreaker() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .recordExceptions(Exception.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        return registry.circuitBreaker("rabbitmq", config);
    }

    /**
     * 配置订单服务断路器
     */
    @Bean("orderServiceCircuitBreaker")
    public CircuitBreaker orderServiceCircuitBreaker() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .recordExceptions(Exception.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        return registry.circuitBreaker("orderService", config);
    }
}


