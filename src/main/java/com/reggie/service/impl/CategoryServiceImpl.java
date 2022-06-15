package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.dao.CategoryDao;
import com.reggie.domain.Category;
import com.reggie.domain.Dish;
import com.reggie.domain.Setmeal;
import com.reggie.service.CategoryService;
import com.reggie.service.DishService;
import com.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 自定义分类删除方法
     * 先判断该分类是否关联菜品和套餐，然后再执行删除
     * @param id 前端传进来的id
     */
    @Override
    public void remove(Long id) {
        //创建dish lqw
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件
        dishLambdaQueryWrapper.eq(Dish::getCategoryId, id);

        //查询当前分类是否关联了菜品，如果已有则抛异常
        int dishCount = dishService.count(dishLambdaQueryWrapper);
        if (dishCount > 0) {
            //关联了菜品，抛出异常
            throw new CustomException("已关联菜品，不能删除");
        }

        //创建dish lqw
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //添加条件
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId, id);

        //查询当前分类是否关联了套餐，如果已有则抛异常
        int setmealCount =setmealService.count(setmealLambdaQueryWrapper);
        if (setmealCount > 0) {
            //关联了套餐，抛出异常
            throw new CustomException("已关联套餐，不能删除");
        }

        //正常删除
        super.removeById(id);
    }
}
