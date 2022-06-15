package com.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.dao.SetmealDao;
import com.reggie.domain.Setmeal;
import com.reggie.domain.SetmealDish;
import com.reggie.dto.SetmealDto;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealDao, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐，同时对套餐表和中间表进行添加
     * @param setmealDto 封装了套餐和套餐菜品的数据
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        //对套餐表进行新增
        this.save(setmealDto);

        //遍历套餐菜品集合，给对应的套餐id赋值
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealDto.getId());
        }

        //对中间表进行新增
        setmealDishService.saveBatch(setmealDishes);
    }
}
