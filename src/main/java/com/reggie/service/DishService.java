package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.domain.Dish;
import com.reggie.dto.DishDto;

public interface DishService extends IService<Dish> {
    /**
     * 新增菜品
     * 同时对两个表进行新增操作
     * @param dishDto 封装了flavors和dish数据的DTO
     */
    void saveWithFlavor(DishDto dishDto);

    /**
     * 根据id查询菜品以及口味
     * @param id 菜品id
     * @return 封装了菜品和口味的DTO对象
     */
    DishDto getByIdWithFlavor(Long id);

    /**
     * 修改菜品以及口味
     * @param dishDto 封装了flavors和dish数据的DTO
     */
    void updateWithFlavor(DishDto dishDto);
}
