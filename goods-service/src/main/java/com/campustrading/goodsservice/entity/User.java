package com.campustrading.goodsservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

/**
 * user-service 返回值的镜像 DTO，仅用于跨服务传输，因此不接收 password 字段。
 */
public class User {

    private Integer id;
    private String username;

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
