package com.campustrading.userservice.mapper;

import com.campustrading.userservice.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("SELECT id, username, password, balance FROM user WHERE id = #{id}")
    User findById(Integer id);

    @Select("SELECT id, username, password, balance FROM user WHERE username = #{username}")
    User findByUsername(String username);
}
