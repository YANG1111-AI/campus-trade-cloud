package com.campustrading.goodsservice.controller;

import com.campustrading.goodsservice.entity.Goods;
import com.campustrading.goodsservice.entity.User;
import com.campustrading.goodsservice.feign.UserFeignClient;
import com.campustrading.goodsservice.mapper.GoodsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/goods")
public class GoodsController {

    private static final Logger log = LoggerFactory.getLogger(GoodsController.class);

    /** 下单消息发往的 Stream binding 名称，对应配置中的 destination: order-topic。 */
    static final String ORDER_BINDING = "orderOutput-out-0";

    private final GoodsMapper goodsMapper;
    private final UserFeignClient userFeignClient;
    private final StreamBridge streamBridge;

    public GoodsController(GoodsMapper goodsMapper, UserFeignClient userFeignClient, StreamBridge streamBridge) {
        this.goodsMapper = goodsMapper;
        this.userFeignClient = userFeignClient;
        this.streamBridge = streamBridge;
    }

    @GetMapping("/list")
    public List<Goods> list() {
        return goodsMapper.findAll();
    }

    @GetMapping("/detail/{id}")
    public Goods detail(@PathVariable Integer id) {
        return goodsMapper.findById(id);
    }

    /**
     * 下单示例：Feign 远程调用 + 发送异步消息。
     * 该接口会改变服务状态，本应使用 POST；为保持前端示例一致仍保留 GET，详见 README「已知限制」。
     */
    @GetMapping("/order/{goodsId}/{userId}")
    public String order(@PathVariable Integer goodsId, @PathVariable Integer userId) {
        User user = userFeignClient.getUserInfo(userId);
        // user-service 不可用时 Sentinel 触发降级，返回 null
        if (user == null) {
            log.warn("下单失败：用户不存在或 user-service 已降级, goodsId={}, userId={}", goodsId, userId);
            return "下单失败：用户不存在或服务降级！";
        }

        String msg = buildOrderMessage(user, goodsId);
        streamBridge.send(ORDER_BINDING, msg);
        log.info("下单消息已投递, binding={}, msg={}", ORDER_BINDING, msg);

        return "下单成功！消息已发送至 RabbitMQ";
    }

    static String buildOrderMessage(User user, Integer goodsId) {
        return "下单成功：用户 " + user.getUsername() + " 购买了商品 ID:" + goodsId;
    }
}
