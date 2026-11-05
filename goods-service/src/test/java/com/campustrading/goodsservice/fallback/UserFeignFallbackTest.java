package com.campustrading.goodsservice.fallback;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserFeignFallbackTest {

    private final UserFeignFallback fallback = new UserFeignFallback();

    @Test
    void returnsNullInsteadOfThrowing() {
        assertThat(fallback.getUserInfo(1)).isNull();
    }
}
