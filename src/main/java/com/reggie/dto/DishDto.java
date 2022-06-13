package com.reggie.dto;

import com.reggie.domain.Dish;
import com.reggie.domain.DishFlavor;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Dish DTO 包含了菜品已有的数据以及前端可能需要用到的其他数据
 */
@Data
public class DishDto extends Dish {

    /**
     * 当前菜品可指定的口味
     */
    private List<DishFlavor> flavors = new ArrayList<>();

    /**
     * 分类名称，用于页面展示
     */
    private String categoryName;

    private Integer copies;
}
