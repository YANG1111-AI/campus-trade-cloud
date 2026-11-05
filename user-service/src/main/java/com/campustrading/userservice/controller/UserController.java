package com.campustrading.userservice.controller;

import com.campustrading.userservice.entity.User;
import com.campustrading.userservice.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RefreshScope
public class UserController {

    private final UserMapper userMapper;

    /**
     * 由 Nacos 配置中心下发的公告语，修改配置后无需重启即可生效。
     * 配置缺失时使用本地默认值。
     */
    @Value("${campus.welcome:欢迎来到校园二手交易平台}")
    private String welcomeMsg;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 供 goods-service 通过 Feign 调用的用户查询接口。 */
    @GetMapping("/info/{id}")
    public User getUserInfo(@PathVariable Integer id) {
        return userMapper.findById(id);
    }

    /** 登录示例：仅按用户名查询，未做任何密码校验或鉴权。 */
    @GetMapping("/login")
    public String login(@RequestParam String username) {
        User user = userMapper.findByUsername(username);
        if (user != null) {
            return "Login Success: " + user.getUsername();
        }
        return "Login Failed";
    }

    /** 注册示例桩接口：只回显参数，不写库。真实注册需要密码加密与参数校验。 */
    @GetMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password) {
        return "注册接口为示例桩实现，未写入数据库：" + username;
    }

    /** 返回配置中心读取到的公告语，用于演示动态刷新。 */
    @GetMapping("/welcome")
    public String getWelcome() {
        return welcomeMsg;
    }
}
