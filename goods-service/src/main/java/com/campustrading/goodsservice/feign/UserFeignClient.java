package com.campustrading.goodsservice.feign;

import com.campustrading.goodsservice.entity.User;
import com.campustrading.goodsservice.fallback.UserFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// value: 指定要调用的服务名（在Nacos中注册的名字）
// fallback: 指定降级处理类，当 user-service 不可用时执行
@FeignClient(value = "user-service", fallback = UserFeignFallback.class)
public interface UserFeignClient {
    @GetMapping("/user/info/{id}")
    User getUserInfo(@PathVariable("id") Integer id);
}