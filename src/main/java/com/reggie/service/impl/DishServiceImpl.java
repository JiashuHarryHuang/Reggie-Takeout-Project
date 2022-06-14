package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.dao.DishDao;
import com.reggie.domain.Dish;
import com.reggie.domain.DishFlavor;
import com.reggie.dto.DishDto;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
public class DishServiceImpl extends ServiceImpl<DishDao, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Value("${reggie.path}")
    private String basePath;

    /**
     * 新增菜品
     * 同时对两个表进行新增操作
     * @param dishDto 封装了flavors和dish数据的DTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //将菜品数据存放到菜品表
        this.save(dishDto);
        //save完之后该菜品的id已经被自动赋值

        //提取菜品id
        Long dishId = dishDto.getId();

        //给所有flavor的id赋值
        List<DishFlavor> flavors = dishDto.getFlavors();
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dishId);
        }

        //将口味数据存放到dish_flavor表
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据id查询菜品以及口味
     * @param id 菜品id
     * @return 封装了菜品和口味的DTO对象
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品数据
        Dish dish = this.getById(id);
        DishDto dishDto = new DishDto();

        //将菜品数据复制到DishDto
        BeanUtils.copyProperties(dish, dishDto);

        //查询口味数据
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, id);
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);

        //给DishDto的口味属性赋值
        dishDto.setFlavors(flavors);

        return dishDto;
    }

    /**
     * 修改菜品以及口味
     * @param dishDto 封装了flavors和dish数据的DTO
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新菜品数据
        this.updateById(dishDto);

        //获取菜品id
        Long dishId = dishDto.getId();

        //删除口味表的对应数据: delete from flavor where dish_id = {dish_id}
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishId);
        dishFlavorService.remove(queryWrapper);

        //提取dishDto的flavors，给每个口味的dishId赋值
        List<DishFlavor> flavors = dishDto.getFlavors();
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dishId);
        }

        //将处理好的flavors存到数据库
        dishFlavorService.saveBatch(flavors);

    }

    /**
     * 删除菜品
     * @param ids 前端的id数组
     */
    @Override
    @Transactional
    public void deleteByIdsWithFlavor(Long[] ids) {
        //根据id删除图片
        this.removeFilesByIds(ids);

        //删除dish数据
        this.removeByIds(Arrays.stream(ids).toList());

        //根据每个id删除flavor数据
        for (Long id : ids) {
            LambdaQueryWrapper<DishFlavor> flavorWrapper = new LambdaQueryWrapper<>();
            flavorWrapper.eq(DishFlavor::getDishId, id);
            dishFlavorService.remove(flavorWrapper);
        }
    }

    /**
     * 根据id删除图片
     * @param ids id数组
     */
    private void removeFilesByIds(Long[] ids) {
        //根据id查询得到菜品集合
        LambdaQueryWrapper<Dish> dishWrapper = new LambdaQueryWrapper<>();
        dishWrapper.in(Dish::getId, ids);
        List<Dish> dishes = this.list(dishWrapper);

        //遍历菜品集合，根据每个菜品的图片名删除对应图片
        for (Dish dish : dishes) {
            File picture = new File(basePath + dish.getImage());
            picture.delete();
        }
    }
}
