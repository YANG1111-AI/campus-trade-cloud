package com.campustrading.userservice.controller;

import com.campustrading.userservice.entity.User;
import com.campustrading.userservice.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserController userController;

    @Test
    void loginSucceedsForExistingUsername() {
        User user = new User();
        user.setId(1);
        user.setUsername("seller_01");
        when(userMapper.findByUsername("seller_01")).thenReturn(user);

        assertThat(userController.login("seller_01")).isEqualTo("Login Success: seller_01");
    }

    @Test
    void loginFailsForUnknownUsername() {
        when(userMapper.findByUsername("nobody")).thenReturn(null);

        assertThat(userController.login("nobody")).isEqualTo("Login Failed");
    }

    /** 密码列允许存在于实体中，但绝不能出现在对外 JSON 里。 */
    @Test
    void userInfoJsonDoesNotExposePassword() throws Exception {
        User user = new User();
        user.setId(1);
        user.setUsername("seller_01");
        user.setPassword("demo-password");
        when(userMapper.findById(1)).thenReturn(user);

        String json = new ObjectMapper().writeValueAsString(userController.getUserInfo(1));

        assertThat(json).doesNotContain("demo-password").doesNotContain("password");
    }
}
