package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.dao.SetmealDao;
import com.reggie.domain.Dish;
import com.reggie.domain.DishFlavor;
import com.reggie.domain.Setmeal;
import com.reggie.domain.SetmealDish;
import com.reggie.dto.SetmealDto;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealDao, Setmeal> implements SetmealService {

    @Autowired
    private SetmealDishService setmealDishService;

    @Value("${reggie.path}")
    private String basePath;

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

    @Override
    public void deleteByIdsWithDish(Long[] ids) {
        //根据id删除图片
        this.removeFilesByIds(ids);

        //删除dish数据
        this.removeByIds(Arrays.stream(ids).toList());

        //根据每个id删除setmeal_dish的数据
        for (Long id : ids) {
            LambdaQueryWrapper<SetmealDish> setmealDishWrapper = new LambdaQueryWrapper<>();
            setmealDishWrapper.eq(SetmealDish::getSetmealId, id);
            setmealDishService.remove(setmealDishWrapper);
        }
    }

    /**
     * 根据id删除图片
     * @param ids id数组
     */
    private void removeFilesByIds(Long[] ids) {
        //根据id查询得到套餐集合
        LambdaQueryWrapper<Setmeal> setmealWrapper = new LambdaQueryWrapper<>();
        setmealWrapper.in(Setmeal::getId, ids);
        List<Setmeal> setmeals = this.list(setmealWrapper);

        //遍历菜品集合，根据每个套餐的图片名删除对应图片
        for (Setmeal setmeal : setmeals) {
            File picture = new File(basePath + setmeal.getImage());
            picture.delete();
        }
    }
}
