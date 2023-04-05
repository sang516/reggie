package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {
    //新增菜品，同时添加菜品对应的口味数据，需要操作两张表：dish dish_flavor
    void saveWithFlavor(DishDto dishDto);
    //根据id查询菜品信息包括对应的口味数据
    DishDto getByIdWithFlavor(Long id);

    //更新菜品信息及口味
    void updateWithFlavor(DishDto dishDto);
    //批量删除菜品
    void deleteWithFlavor(List<Long> ids);
}
