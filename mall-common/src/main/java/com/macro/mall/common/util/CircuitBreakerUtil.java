package com.macro.mall.common.util;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * 断路器工具类
 * Created for Circuit Breaker Pattern implementation
 */
public class CircuitBreakerUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CircuitBreakerUtil.class);

    /**
     * 执行受断路器保护的操作
     *
     * @param circuitBreaker 断路器实例
     * @param supplier 要执行的操作
     * @param fallback 降级操作
     * @param <T> 返回类型
     * @return 执行结果
     */
    public static <T> T execute(CircuitBreaker circuitBreaker, Supplier<T> supplier, Supplier<T> fallback) {
        try {
            return circuitBreaker.executeSupplier(supplier);
        } catch (CallNotPermittedException e) {
            LOGGER.warn("断路器已打开，执行降级策略: {}", e.getMessage());
            return fallback.get();
        } catch (Exception e) {
            LOGGER.error("执行操作时发生异常，执行降级策略: {}", e.getMessage(), e);
            return fallback.get();
        }
    }

    /**
     * 执行受断路器保护的操作（无返回值）
     *
     * @param circuitBreaker 断路器实例
     * @param runnable 要执行的操作
     * @param fallback 降级操作
     */
    public static void execute(CircuitBreaker circuitBreaker, Runnable runnable, Runnable fallback) {
        try {
            circuitBreaker.executeRunnable(runnable);
        } catch (CallNotPermittedException e) {
            LOGGER.warn("断路器已打开，执行降级策略: {}", e.getMessage());
            fallback.run();
        } catch (Exception e) {
            LOGGER.error("执行操作时发生异常，执行降级策略: {}", e.getMessage(), e);
            fallback.run();
        }
    }

    /**
     * 执行受断路器保护的操作（无降级策略）
     *
     * @param circuitBreaker 断路器实例
     * @param supplier 要执行的操作
     * @param <T> 返回类型
     * @return 执行结果
     * @throws Exception 如果操作失败
     */
    public static <T> T execute(CircuitBreaker circuitBreaker, Supplier<T> supplier) throws Exception {
        try {
            return circuitBreaker.executeSupplier(supplier);
        } catch (CallNotPermittedException e) {
            LOGGER.warn("断路器已打开: {}", e.getMessage());
            throw e;
        }
    }
}



