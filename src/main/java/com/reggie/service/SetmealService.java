package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.domain.Setmeal;
import com.reggie.dto.SetmealDto;

public interface SetmealService extends IService<Setmeal> {
    /**
     * 新增套餐，同时对套餐表和中间表进行添加
     * @param setmealDto 封装了套餐和套餐菜品的数据
     */
    void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐
     * @param ids 前端的id数组
     */
    void deleteByIdsWithDish(Long[] ids);

    /**
     * 根据id查询套餐
     * @param id 套餐id
     * @return 封装了菜品和套餐的对象
     */
    SetmealDto getByIdWithDish(Long id);

    void updateWithDish(SetmealDto setmealDto);
}
