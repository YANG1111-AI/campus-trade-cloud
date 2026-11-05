package com.campustrading.goodsservice.fallback;

import com.campustrading.goodsservice.entity.User;
import com.campustrading.goodsservice.feign.UserFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * user-service 不可用时的 Feign 降级实现，需要 feign.sentinel.enabled=true 才会被触发。
 */
@Component
public class UserFeignFallback implements UserFeignClient {

    private static final Logger log = LoggerFactory.getLogger(UserFeignFallback.class);

    @Override
    public User getUserInfo(Integer id) {
        log.warn("user-service 调用失败，已触发降级, userId={}", id);
        return null;
    }
}
