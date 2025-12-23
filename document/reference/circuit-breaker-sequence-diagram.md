# 断路器模式重构 - 时序图对比

## 一、搜索服务 - 重构前后对比

### 1.1 重构前：正常流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Controller as EsProductController
    participant Service as EsProductServiceImpl
    participant ES as ElasticsearchRestTemplate
    
    User->>Controller: 搜索商品
    Controller->>Service: search(keyword, ...)
    Service->>ES: search(query, EsProduct.class)
    ES-->>Service: SearchHits<EsProduct>
    Service-->>Controller: Page<EsProduct>
    Controller-->>User: 返回搜索结果
```

### 1.2 重构前：异常流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Controller as EsProductController
    participant Service as EsProductServiceImpl
    participant ES as ElasticsearchRestTemplate
    
    User->>Controller: 搜索商品
    Controller->>Service: search(keyword, ...)
    Service->>ES: search(query, EsProduct.class)
    ES-->>Service: 抛出异常（Elasticsearch 不可用）
    Service-->>Controller: 抛出异常
    Controller-->>User: 返回错误页面（500错误）
```

### 1.3 重构后：正常流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Controller as EsProductController
    participant Service as EsProductServiceImpl
    participant Util as CircuitBreakerUtil
    participant CB as CircuitBreaker
    participant ES as ElasticsearchRestTemplate
    
    User->>Controller: 搜索商品
    Controller->>Service: search(keyword, ...)
    Service->>Util: execute(circuitBreaker, supplier, fallback)
    Util->>CB: 检查状态（CLOSED）
    CB-->>Util: 允许执行
    Util->>ES: search(query, EsProduct.class)
    ES-->>Util: SearchHits<EsProduct>
    Util-->>Service: Page<EsProduct>
    Service-->>Controller: Page<EsProduct>
    Controller-->>User: 返回搜索结果
```

### 1.4 重构后：降级流程（断路器打开）

```mermaid
sequenceDiagram
    participant User as 用户
    participant Controller as EsProductController
    participant Service as EsProductServiceImpl
    participant Util as CircuitBreakerUtil
    participant CB as CircuitBreaker
    participant DAO as EsProductDao
    
    User->>Controller: 搜索商品
    Controller->>Service: search(keyword, ...)
    Service->>Util: execute(circuitBreaker, supplier, fallback)
    Util->>CB: 检查状态（OPEN）
    CB-->>Util: 拒绝执行（CallNotPermittedException）
    Util->>DAO: searchProducts(keyword, ...) [降级策略]
    DAO-->>Util: List<EsProduct>
    Util-->>Service: Page<EsProduct>
    Service-->>Controller: Page<EsProduct>
    Controller-->>User: 返回搜索结果（从数据库）
```

### 1.5 重构后：降级流程（执行失败）

```mermaid
sequenceDiagram
    participant User as 用户
    participant Controller as EsProductController
    participant Service as EsProductServiceImpl
    participant Util as CircuitBreakerUtil
    participant CB as CircuitBreaker
    participant ES as ElasticsearchRestTemplate
    participant DAO as EsProductDao
    
    User->>Controller: 搜索商品
    Controller->>Service: search(keyword, ...)
    Service->>Util: execute(circuitBreaker, supplier, fallback)
    Util->>CB: 检查状态（CLOSED）
    CB-->>Util: 允许执行
    Util->>ES: search(query, EsProduct.class)
    ES-->>Util: 抛出异常（Elasticsearch 不可用）
    Util->>CB: 记录失败
    CB->>CB: 更新失败计数
    Util->>DAO: searchProducts(keyword, ...) [降级策略]
    DAO-->>Util: List<EsProduct>
    Util-->>Service: Page<EsProduct>
    Service-->>Controller: Page<EsProduct>
    Controller-->>User: 返回搜索结果（从数据库）
    
    Note over CB: 如果失败率达到阈值，<br/>断路器状态变为 OPEN
```

## 二、订单服务 - 重构前后对比

### 2.1 重构前：正常流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Controller as OmsPortalOrderController
    participant Service as OmsPortalOrderServiceImpl
    participant Stock as 库存服务
    participant Coupon as 优惠券服务
    participant Redis as RedisService
    participant MQ as CancelOrderSender
    
    User->>Controller: 提交订单
    Controller->>Service: generateOrder(orderParam)
    Service->>Stock: hasStock(cartItems)
    Stock-->>Service: true
    Service->>Coupon: getUseCoupon(cartItems, couponId)
    Coupon-->>Service: SmsCouponHistoryDetail
    Service->>Stock: lockStock(cartItems)
    Stock-->>Service: 成功
    Service->>Redis: incr(key, 1) [生成订单号]
    Redis-->>Service: 订单号
    Service->>Service: 保存订单
    Service->>MQ: sendMessage(orderId, delayTimes)
    MQ-->>Service: 成功
    Service-->>Controller: 订单信息
    Controller-->>User: 订单创建成功
```

### 2.2 重构前：异常流程（库存检查失败）

```mermaid
sequenceDiagram
    participant User as 用户
    participant Controller as OmsPortalOrderController
    participant Service as OmsPortalOrderServiceImpl
    participant Stock as 库存服务
    
    User->>Controller: 提交订单
    Controller->>Service: generateOrder(orderParam)
    Service->>Stock: hasStock(cartItems)
    Stock-->>Service: 抛出异常（库存服务不可用）
    Service-->>Controller: 抛出异常
    Controller-->>User: 返回错误（订单创建失败）
```

### 2.3 重构后：正常流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Controller as OmsPortalOrderController
    participant Service as OmsPortalOrderServiceImpl
    participant Util as CircuitBreakerUtil
    participant CB as CircuitBreaker
    participant Stock as 库存服务
    participant Coupon as 优惠券服务
    participant Redis as RedisService
    participant MQ as CancelOrderSender
    
    User->>Controller: 提交订单
    Controller->>Service: generateOrder(orderParam)
    Service->>Util: execute(cb, hasStock, fallback)
    Util->>CB: 检查状态
    CB-->>Util: 允许执行
    Util->>Stock: hasStock(cartItems)
    Stock-->>Util: true
    Util-->>Service: true
    Service->>Util: execute(cb, getUseCoupon, fallback)
    Util->>Coupon: getUseCoupon(...)
    Coupon-->>Util: SmsCouponHistoryDetail
    Util-->>Service: SmsCouponHistoryDetail
    Service->>Util: execute(cb, lockStock, fallback)
    Util->>Stock: lockStock(cartItems)
    Stock-->>Util: 成功
    Util-->>Service: 成功
    Service->>Util: execute(redisCB, generateOrderSn, fallback)
    Util->>Redis: incr(key, 1)
    Redis-->>Util: 订单号
    Util-->>Service: 订单号
    Service->>Service: 保存订单
    Service->>Util: execute(mqCB, sendMessage, fallback)
    Util->>MQ: sendMessage(orderId, delayTimes)
    MQ-->>Util: 成功
    Util-->>Service: 成功
    Service-->>Controller: 订单信息
    Controller-->>User: 订单创建成功
```

### 2.4 重构后：部分降级流程（优惠券服务故障）

```mermaid
sequenceDiagram
    participant User as 用户
    participant Controller as OmsPortalOrderController
    participant Service as OmsPortalOrderServiceImpl
    participant Util as CircuitBreakerUtil
    participant CB as CircuitBreaker
    participant Stock as 库存服务
    participant Coupon as 优惠券服务
    participant Redis as RedisService
    participant MQ as CancelOrderSender
    
    User->>Controller: 提交订单（使用优惠券）
    Controller->>Service: generateOrder(orderParam)
    Service->>Util: execute(cb, hasStock, fallback)
    Util->>Stock: hasStock(cartItems)
    Stock-->>Util: true
    Util-->>Service: true
    Service->>Util: execute(cb, getUseCoupon, fallback)
    Util->>CB: 检查状态（OPEN）
    CB-->>Util: 拒绝执行
    Util-->>Service: null [降级策略：不使用优惠券]
    Service->>Service: 设置优惠券金额为0
    Service->>Util: execute(cb, lockStock, fallback)
    Util->>Stock: lockStock(cartItems)
    Stock-->>Util: 成功
    Util-->>Service: 成功
    Service->>Util: execute(redisCB, generateOrderSn, fallback)
    Util->>Redis: incr(key, 1)
    Redis-->>Util: 订单号
    Util-->>Service: 订单号
    Service->>Service: 保存订单
    Service->>Util: execute(mqCB, sendMessage, fallback)
    Util->>MQ: sendMessage(orderId, delayTimes)
    MQ-->>Util: 成功
    Util-->>Service: 成功
    Service-->>Controller: 订单信息（未使用优惠券）
    Controller-->>User: 订单创建成功（未使用优惠券）
    
    Note over Service: 虽然优惠券服务故障，<br/>但订单仍可创建
```

### 2.5 重构后：降级流程（Redis 故障）

```mermaid
sequenceDiagram
    participant User as 用户
    participant Controller as OmsPortalOrderController
    participant Service as OmsPortalOrderServiceImpl
    participant Util as CircuitBreakerUtil
    participant RedisCB as RedisCircuitBreaker
    participant Redis as RedisService
    
    User->>Controller: 提交订单
    Controller->>Service: generateOrder(orderParam)
    Service->>Util: execute(redisCB, generateOrderSn, fallback)
    Util->>RedisCB: 检查状态（OPEN）
    RedisCB-->>Util: 拒绝执行
    Util->>Service: generateOrderSnFallback(order) [降级策略]
    Service->>Service: 使用时间戳+随机数生成订单号
    Service-->>Controller: 订单信息（使用备用订单号）
    Controller-->>User: 订单创建成功
    
    Note over Service: Redis 不可用时，<br/>使用备用方案生成订单号
```

## 三、缓存服务 - 重构前后对比

### 3.1 重构前：正常流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Service as UmsMemberServiceImpl
    participant Cache as UmsMemberCacheServiceImpl
    participant Redis as RedisService
    
    User->>Service: 登录（getByUsername）
    Service->>Cache: getMember(username)
    Cache->>Redis: get(key)
    Redis-->>Cache: UmsMember
    Cache-->>Service: UmsMember
    Service-->>User: 登录成功
```

### 3.2 重构前：异常流程（Redis 故障）

```mermaid
sequenceDiagram
    participant User as 用户
    participant Service as UmsMemberServiceImpl
    participant Cache as UmsMemberCacheServiceImpl
    participant Redis as RedisService
    
    User->>Service: 登录（getByUsername）
    Service->>Cache: getMember(username)
    Cache->>Redis: get(key)
    Redis-->>Cache: 抛出异常（Redis 不可用）
    Cache-->>Service: null
    Service->>Service: 从数据库查询
    Service-->>User: 登录成功（但响应较慢）
    
    Note over Service: 虽然能登录，但需要从数据库查询，<br/>响应时间增加
```

### 3.3 重构后：正常流程

```mermaid
sequenceDiagram
    participant User as 用户
    participant Service as UmsMemberServiceImpl
    participant Cache as UmsMemberCacheServiceImpl
    participant Util as CircuitBreakerUtil
    participant CB as CircuitBreaker
    participant Redis as RedisService
    
    User->>Service: 登录（getByUsername）
    Service->>Cache: getMember(username)
    Cache->>Redis: get(key) [带断路器保护]
    Redis-->>Cache: UmsMember
    Cache-->>Service: UmsMember
    Service-->>User: 登录成功
```

### 3.4 重构后：降级流程（Redis 故障）

```mermaid
sequenceDiagram
    participant User as 用户
    participant Service as UmsMemberServiceImpl
    participant Cache as UmsMemberCacheServiceImpl
    participant Util as CircuitBreakerUtil
    participant CB as CircuitBreaker
    participant Redis as RedisService
    participant DB as UmsMemberMapper
    
    User->>Service: 登录（getByUsername）
    Service->>Cache: getMember(username)
    Cache->>Redis: get(key) [带断路器保护]
    Redis-->>Cache: 抛出异常（Redis 不可用）
    Cache->>CB: 记录失败
    CB->>CB: 更新失败计数
    Cache->>DB: selectByExample(example) [降级策略]
    DB-->>Cache: UmsMember
    Cache->>Cache: 尝试重新设置缓存
    Cache-->>Service: UmsMember
    Service-->>User: 登录成功（从数据库查询）
    
    Note over Cache: Redis 不可用时，<br/>自动降级到数据库查询
```

## 四、消息队列服务 - 重构前后对比

### 4.1 重构前：正常流程

```mermaid
sequenceDiagram
    participant Service as OmsPortalOrderServiceImpl
    participant Sender as CancelOrderSender
    participant MQ as AmqpTemplate
    
    Service->>Sender: sendMessage(orderId, delayTimes)
    Sender->>MQ: convertAndSend(exchange, routingKey, orderId)
    MQ-->>Sender: 成功
    Sender-->>Service: 成功
```

### 4.2 重构前：异常流程（RabbitMQ 故障）

```mermaid
sequenceDiagram
    participant Service as OmsPortalOrderServiceImpl
    participant Sender as CancelOrderSender
    participant MQ as AmqpTemplate
    
    Service->>Sender: sendMessage(orderId, delayTimes)
    Sender->>MQ: convertAndSend(exchange, routingKey, orderId)
    MQ-->>Sender: 抛出异常（RabbitMQ 不可用）
    Sender-->>Service: 抛出异常
    Service-->>Service: 订单创建成功，但取消消息发送失败
    
    Note over Service: 订单创建成功，但超时后无法自动取消，<br/>需要人工处理
```

### 4.3 重构后：正常流程

```mermaid
sequenceDiagram
    participant Service as OmsPortalOrderServiceImpl
    participant Sender as CancelOrderSender
    participant Util as CircuitBreakerUtil
    participant CB as CircuitBreaker
    participant MQ as AmqpTemplate
    
    Service->>Sender: sendMessage(orderId, delayTimes)
    Sender->>Util: execute(cb, sendMessage, fallback)
    Util->>CB: 检查状态
    CB-->>Util: 允许执行
    Util->>MQ: convertAndSend(exchange, routingKey, orderId)
    MQ-->>Util: 成功
    Util-->>Sender: 成功
    Sender-->>Service: 成功
```

### 4.4 重构后：降级流程（RabbitMQ 故障）

```mermaid
sequenceDiagram
    participant Service as OmsPortalOrderServiceImpl
    participant Sender as CancelOrderSender
    participant Util as CircuitBreakerUtil
    participant CB as CircuitBreaker
    participant MQ as AmqpTemplate
    participant Logger as Logger
    
    Service->>Sender: sendMessage(orderId, delayTimes)
    Sender->>Util: execute(cb, sendMessage, fallback)
    Util->>CB: 检查状态（OPEN）
    CB-->>Util: 拒绝执行
    Util->>Logger: warn("RabbitMQ不可用，记录日志") [降级策略]
    Logger-->>Util: 日志已记录
    Util-->>Sender: 成功（降级策略执行成功）
    Sender-->>Service: 成功
    Service-->>Service: 订单创建成功
    
    Note over Util: RabbitMQ 不可用时，<br/>记录日志，建议扩展为数据库记录
```

## 五、断路器状态转换

### 5.1 断路器状态转换流程

```mermaid
stateDiagram-v2
    [*] --> CLOSED: 初始化
    
    CLOSED --> OPEN: 失败率 >= 阈值<br/>或慢调用率 >= 阈值
    CLOSED --> CLOSED: 正常执行
    
    OPEN --> HALF_OPEN: 等待时间到期
    
    HALF_OPEN --> CLOSED: 测试调用成功
    HALF_OPEN --> OPEN: 测试调用失败
    
    OPEN --> OPEN: 拒绝执行，直接降级
    HALF_OPEN --> HALF_OPEN: 测试中
    CLOSED --> CLOSED: 正常执行
    
    CLOSED --> [*]: 服务停止
    OPEN --> [*]: 服务停止
    HALF_OPEN --> [*]: 服务停止
```

### 5.2 断路器状态说明

- **CLOSED（关闭）**：正常状态，允许执行
- **OPEN（打开）**：故障状态，拒绝执行，直接降级
- **HALF_OPEN（半开）**：测试状态，允许少量调用测试服务是否恢复

## 六、总结

### 重构前特点
- 直接调用外部服务
- 异常时抛出异常，服务不可用
- 无容错机制

### 重构后特点
- 通过断路器间接调用外部服务
- 异常时执行降级策略，服务部分可用
- 完整的容错和降级机制
- 自动状态转换和恢复

### 关键改进
1. **可用性提升**：从完全不可用到部分可用
2. **用户体验改善**：从错误页面到功能降级但可用
3. **自动恢复**：断路器自动检测服务恢复
4. **统一处理**：通过工具类统一处理断路器逻辑



