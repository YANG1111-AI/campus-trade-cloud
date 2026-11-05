package com.campustrading.goodsservice.controller;

import com.campustrading.goodsservice.entity.Goods;
import com.campustrading.goodsservice.entity.User;
import com.campustrading.goodsservice.feign.UserFeignClient;
import com.campustrading.goodsservice.mapper.GoodsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * StreamBridge 是 final 类，在旧版本 Byte Buddy（Spring Boot 2.6 版本线）上无法被打桩，
 * 因此这里只覆盖不依赖消息发送的分支，消息内容通过 buildOrderMessage 单独断言。
 */
@ExtendWith(MockitoExtension.class)
class GoodsControllerTest {

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private UserFeignClient userFeignClient;

    private GoodsController goodsController;

    @BeforeEach
    void setUp() {
        goodsController = new GoodsController(goodsMapper, userFeignClient, null);
    }

    private Goods sampleGoods() {
        Goods goods = new Goods();
        goods.setId(1);
        goods.setTitle("二手 Java 微服务教材");
        goods.setPrice(new BigDecimal("35.00"));
        goods.setSellerId(1);
        goods.setStock(5);
        return goods;
    }

    @Test
    void listReturnsAllGoods() {
        List<Goods> goods = Collections.singletonList(sampleGoods());
        when(goodsMapper.findAll()).thenReturn(goods);

        assertThat(goodsController.list()).isEqualTo(goods);
    }

    @Test
    void detailReturnsSingleGoods() {
        Goods goods = sampleGoods();
        when(goodsMapper.findById(1)).thenReturn(goods);

        assertThat(goodsController.detail(1)).isEqualTo(goods);
    }

    @Test
    void orderFailsWithoutTouchingMqWhenFeignFallsBack() {
        when(userFeignClient.getUserInfo(2)).thenReturn(null);

        String result = goodsController.order(1, 2);

        assertThat(result).isEqualTo("下单失败：用户不存在或服务降级！");
    }

    @Test
    void orderMessageCarriesUsernameAndGoodsId() {
        User user = new User();
        user.setId(2);
        user.setUsername("buyer_01");

        assertThat(GoodsController.buildOrderMessage(user, 7))
                .isEqualTo("下单成功：用户 buyer_01 购买了商品 ID:7");
    }

    @Test
    void bindingNameMatchesStreamDestinationConfig() {
        assertThat(GoodsController.ORDER_BINDING).isEqualTo("orderOutput-out-0");
    }
}
