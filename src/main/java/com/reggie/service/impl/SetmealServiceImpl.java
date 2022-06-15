package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.dao.SetmealDao;
import com.reggie.domain.DishFlavor;
import com.reggie.domain.Setmeal;
import com.reggie.domain.SetmealDish;
import com.reggie.dto.SetmealDto;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
import org.springframework.beans.BeanUtils;
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

    /**
     * 删除套餐
     * @param ids 前端的id数组
     */
    @Override
    @Transactional
    public void deleteByIdsWithDish(Long[] ids) {
        //select count(*) from setmeal where id in (1,2,3) and status = 1
        //查询套餐状态，确定是否可用删除
        //只有禁用状态可以删除
        LambdaQueryWrapper<Setmeal> setmealWrapper = new LambdaQueryWrapper();
        setmealWrapper.in(Setmeal::getId,ids);
        setmealWrapper.eq(Setmeal::getStatus,1);

        int count = this.count(setmealWrapper);
        if(count > 0){
            //如果不能删除，抛出一个业务异常
            throw new CustomException("套餐正在售卖中，不能删除");
        }

        //根据id删除图片
        this.removeFilesByIds(ids);

        //删除dish数据
        this.removeByIds(Arrays.stream(ids).toList());

        //根据每个id删除setmeal_dish的数据
        LambdaQueryWrapper<SetmealDish> setmealDishWrapper = new LambdaQueryWrapper<>();
        setmealDishWrapper.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(setmealDishWrapper);

    }

    /**
     * 根据id查询套餐
     * @param id 套餐id
     * @return 封装了菜品和套餐的对象
     */
    @Override
    public SetmealDto getByIdWithDish(Long id) {
        Setmeal setmeal = this.getById(id);
        SetmealDto setmealDto = new SetmealDto();

        //复制对象
        BeanUtils.copyProperties(setmeal, setmealDto);

        //条件查询: select from setmeal_dish where setmeal_id = {id}
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishes = setmealDishService.list(queryWrapper);

        //给dto赋值
        setmealDto.setSetmealDishes(setmealDishes);

        return setmealDto;
    }

    @Override
    public void updateWithDish(SetmealDto setmealDto) {
        //更新套餐数据
        this.updateById(setmealDto);

        //根据id删除中间表的数据：delete from setmeal_dish where setmeal_id = {setmealId}
        Long setmealId = setmealDto.getId();
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealId);
        setmealDishService.remove(queryWrapper);

        //给中间表重新添加数据
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealId);
        }

        setmealDishService.saveBatch(setmealDishes);
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
