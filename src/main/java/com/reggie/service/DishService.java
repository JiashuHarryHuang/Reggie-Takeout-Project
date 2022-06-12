package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.domain.Dish;
import com.reggie.dto.DishDto;

public interface DishService extends IService<Dish> {
    void saveWithFlavor(DishDto dishDto);
}
