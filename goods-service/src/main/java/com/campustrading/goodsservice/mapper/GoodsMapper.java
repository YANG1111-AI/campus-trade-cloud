package com.campustrading.goodsservice.mapper;

import com.campustrading.goodsservice.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GoodsMapper {

    @Select("SELECT id, title, price, seller_id, stock FROM goods")
    List<Goods> findAll();

    @Select("SELECT id, title, price, seller_id, stock FROM goods WHERE id = #{id}")
    Goods findById(Integer id);
}
