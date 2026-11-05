package com.campustrading.userservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

public class User {

    private Integer id;
    private String username;

    /**
     * 密码只在数据库层使用，不随 REST/Feign 响应返回。
     * 本示例未实现密码校验，生产环境必须使用 Spring Security 等做加密比对。
     */
    @JsonIgnore
    private String password;

    private BigDecimal balance;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
