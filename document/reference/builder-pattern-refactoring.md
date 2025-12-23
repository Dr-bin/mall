# Builder模式重构说明文档

## 一、重构必要性（原因）

### 1.1 原始代码存在的问题

在重构前的订单创建逻辑中，`OmsPortalOrderServiceImpl.generateOrder()` 方法存在以下问题：

#### 1.1.1 代码可读性差
- **问题描述**：在 `generateOrder()` 方法中（170-225行），使用了大量的 `setter` 方法逐个设置订单属性，代码冗长且难以阅读。
- **示例代码**：
```java
OmsOrder order = new OmsOrder();
order.setDiscountAmount(new BigDecimal(0));
order.setTotalAmount(calcTotalAmount(orderItemList));
order.setFreightAmount(new BigDecimal(0));
order.setPromotionAmount(calcPromotionAmount(orderItemList));
order.setPromotionInfo(getOrderPromotionInfo(orderItemList));
// ... 还有20多个setter调用
```

#### 1.1.2 参数设置顺序混乱
- **问题描述**：由于使用多个独立的 `setter` 方法，属性设置的顺序没有逻辑性，容易导致相关属性分散设置，增加理解成本。
- **影响**：例如，优惠券相关的 `couponId` 和 `couponAmount` 被分散在不同位置设置，不利于代码维护。

#### 1.1.3 对象状态不一致风险
- **问题描述**：在对象创建过程中，可能因为遗漏某些 `setter` 调用而导致对象处于不完整状态。
- **风险**：如果某个必填字段忘记设置，只有在运行时才能发现，增加了调试难度。

#### 1.1.4 代码维护困难
- **问题描述**：当需要添加新的订单属性时，需要在多个地方添加 `setter` 调用，容易遗漏。
- **影响**：随着业务复杂度增加，代码变得越来越难以维护。

### 1.2 Builder模式的优势

Builder模式可以很好地解决上述问题：

1. **链式调用**：通过方法链式调用，代码更加流畅和易读
2. **逻辑分组**：可以将相关属性的设置组织在一起，提高代码可读性
3. **可选参数处理**：可以优雅地处理可选参数（如优惠券、积分等）
4. **对象构建过程清晰**：整个对象构建过程一目了然

## 二、重构过程

### 2.1 在 OmsOrder 类中添加 Builder 模式

#### 2.1.1 添加静态工厂方法
在 `OmsOrder` 类中添加静态方法 `builder()`，用于创建 Builder 实例：

```java
public static Builder builder() {
    return new Builder();
}
```

#### 2.1.2 创建 Builder 内部类
在 `OmsOrder` 类中创建静态内部类 `Builder`，为每个属性提供对应的设置方法：

```java
public static class Builder {
    private OmsOrder order;

    public Builder() {
        order = new OmsOrder();
    }

    public Builder memberId(Long memberId) {
        order.setMemberId(memberId);
        return this;
    }

    public Builder totalAmount(BigDecimal totalAmount) {
        order.setTotalAmount(totalAmount);
        return this;
    }

    // ... 为所有属性提供对应的builder方法

    public OmsOrder build() {
        return order;
    }
}
```

**关键设计点**：
- 每个 `builder` 方法都返回 `Builder` 实例本身，支持链式调用
- `build()` 方法返回构建完成的 `OmsOrder` 对象
- Builder 内部维护一个 `OmsOrder` 实例，通过 `setter` 方法设置属性

### 2.2 重构 Service 层的订单创建逻辑

#### 2.2.1 重构前代码（before/OmsPortalOrderServiceImpl.java）

```java
// 创建订单对象
OmsOrder order = new OmsOrder();
order.setDiscountAmount(new BigDecimal(0));
order.setTotalAmount(calcTotalAmount(orderItemList));
order.setFreightAmount(new BigDecimal(0));
order.setPromotionAmount(calcPromotionAmount(orderItemList));
order.setPromotionInfo(getOrderPromotionInfo(orderItemList));
if (orderParam.getCouponId() == null) {
    order.setCouponAmount(new BigDecimal(0));
} else {
    order.setCouponId(orderParam.getCouponId());
    order.setCouponAmount(calcCouponAmount(orderItemList));
}
if (orderParam.getUseIntegration() == null) {
    order.setIntegration(0);
    order.setIntegrationAmount(new BigDecimal(0));
} else {
    order.setIntegration(orderParam.getUseIntegration());
    order.setIntegrationAmount(calcIntegrationAmount(orderItemList));
}
order.setPayAmount(calcPayAmount(order));
order.setMemberId(currentMember.getId());
order.setCreateTime(new Date());
order.setMemberUsername(currentMember.getUsername());
order.setPayType(orderParam.getPayType());
order.setSourceType(1);
order.setStatus(0);
order.setOrderType(0);
// ... 还有更多setter调用
```

#### 2.2.2 重构后代码（mall-portal/.../OmsPortalOrderServiceImpl.java）

```java
// 使用Builder模式创建订单对象
OmsOrder.Builder orderBuilder = OmsOrder.builder()
        .discountAmount(new BigDecimal(0))
        .totalAmount(calcTotalAmount(orderItemList))
        .freightAmount(new BigDecimal(0))
        .promotionAmount(calcPromotionAmount(orderItemList))
        .promotionInfo(getOrderPromotionInfo(orderItemList))
        .memberId(currentMember.getId())
        .createTime(new Date())
        .memberUsername(currentMember.getUsername())
        .payType(message.getPayType())
        .sourceType(1)
        .status(0)
        .orderType(0)
        .confirmStatus(0)
        .deleteStatus(0);

// 设置优惠券信息（条件设置）
if (message.getCouponId() == null) {
    orderBuilder.couponAmount(new BigDecimal(0));
} else {
    orderBuilder.couponId(message.getCouponId())
            .couponAmount(calcCouponAmount(orderItemList));
}

// 设置积分信息（条件设置）
if (message.getUseIntegration() == null) {
    orderBuilder.integration(0)
            .integrationAmount(new BigDecimal(0));
} else {
    orderBuilder.useIntegration(message.getUseIntegration())
            .integrationAmount(calcIntegrationAmount(orderItemList));
}

// 收货人信息（逻辑分组）
UmsMemberReceiveAddress address = memberReceiveAddressService.getItem(message.getMemberReceiveAddressId());
orderBuilder.receiverName(address.getName())
        .receiverPhone(address.getPhoneNumber())
        .receiverPostCode(address.getPostCode())
        .receiverProvince(address.getProvince())
        .receiverCity(address.getCity())
        .receiverRegion(address.getRegion())
        .receiverDetailAddress(address.getDetailAddress());

// 构建订单对象
OmsOrder order = orderBuilder.build();
```

### 2.3 重构对比

| 对比项 | 重构前 | 重构后 |
|--------|--------|--------|
| 代码行数 | 约55行 | 约45行 |
| 可读性 | 低（大量重复的setter调用） | 高（链式调用，逻辑清晰） |
| 属性分组 | 无逻辑分组 | 按业务逻辑分组（基础信息、优惠券、积分、收货地址） |
| 可选参数处理 | 使用if-else分散处理 | 使用条件判断后链式调用，更清晰 |
| 维护性 | 低（添加新属性需要分散添加setter） | 高（添加新属性只需在Builder中添加方法） |

## 三、重构结果（带来的好处）

### 3.1 代码可读性提升

**重构前**：代码中充斥着大量的 `order.setXxx()` 调用，难以快速理解订单对象的构建逻辑。

**重构后**：通过链式调用，代码结构更加清晰，可以一目了然地看到订单对象的构建过程。相关属性的设置被组织在一起，例如：
- 基础订单信息（金额、状态等）
- 优惠券信息（条件设置）
- 积分信息（条件设置）
- 收货地址信息（逻辑分组）

### 3.2 代码维护性提升

1. **添加新属性更容易**：只需在 `Builder` 类中添加对应的 `builder` 方法，不需要修改调用代码的结构。
2. **属性分组更清晰**：相关属性的设置可以组织在一起，便于理解和维护。
3. **减少错误**：链式调用的方式使得代码结构更加规范，减少了遗漏设置属性的可能性。

### 3.3 代码简洁性提升

虽然代码行数没有显著减少，但代码的**表达力**更强：
- 链式调用使得代码更加流畅
- 条件设置更加优雅（如优惠券、积分的条件设置）
- 逻辑分组使得代码结构更加清晰

### 3.4 符合设计模式最佳实践

Builder模式是创建复杂对象的经典设计模式，特别适用于：
- 对象有很多属性
- 有些属性是可选的
- 需要保证对象创建过程的一致性

本次重构完全符合这些场景，使代码更加符合设计模式的最佳实践。

### 3.5 实际效果对比

#### 重构前的问题示例：
```java
// 问题1：属性设置分散，难以理解
order.setTotalAmount(...);
order.setMemberId(...);
order.setTotalAmount(...); // 可能重复设置
order.setCouponId(...);
// ... 中间有很多其他设置
order.setCouponAmount(...); // 优惠券相关属性被分散
```

#### 重构后的优势：
```java
// 优势1：逻辑分组，一目了然
OmsOrder.Builder orderBuilder = OmsOrder.builder()
        .totalAmount(...)      // 金额相关
        .promotionAmount(...)   // 金额相关
        .memberId(...)         // 用户相关
        .memberUsername(...);  // 用户相关

// 优势2：条件设置更清晰
if (couponId != null) {
    orderBuilder.couponId(couponId)      // 优惠券相关
            .couponAmount(...);          // 优惠券相关
}

// 优势3：相关属性集中设置
orderBuilder.receiverName(...)          // 收货地址相关
        .receiverPhone(...)              // 收货地址相关
        .receiverProvince(...)           // 收货地址相关
        .receiverCity(...);              // 收货地址相关
```

## 四、总结

通过引入Builder模式，订单创建逻辑的代码质量得到了显著提升：

1. **可读性**：链式调用使得代码更加流畅，逻辑分组使得结构更加清晰
2. **维护性**：添加新属性更容易，属性分组更清晰，减少错误
3. **简洁性**：代码表达力更强，条件设置更加优雅
4. **规范性**：符合设计模式最佳实践，提高代码质量

这次重构是一个典型的**代码质量提升**案例，通过应用合适的设计模式，使代码更加优雅、易读、易维护。


