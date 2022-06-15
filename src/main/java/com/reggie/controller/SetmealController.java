package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.Result;
import com.reggie.domain.Category;
import com.reggie.domain.Setmeal;
import com.reggie.dto.SetmealDto;
import com.reggie.service.CategoryService;
import com.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增套餐操作
     * @param setmealDto 封装了套餐和套餐菜品的数据
     * @return 成功信息
     */
    @PostMapping
    public Result<String> save(@RequestBody SetmealDto setmealDto) {
        setmealService.saveWithDish(setmealDto);
        return Result.success("新增成功");
    }

    /**
     * 分页查询
     * @param page 当前页
     * @param pageSize 每页显示条数
     * @param name 查询字段
     * @return 封装了套餐和菜品的page对象
     */
    @GetMapping("/page")
    public Result<Page<SetmealDto>> getByPage(int page, int pageSize, String name) {
        //创建page对象
        Page<Setmeal> setmealPage = new Page<>(page, pageSize);

        //添加条件：select from setmeal where name = {name} order by update_time desc
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null, Setmeal::getName, name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        //执行查询
        setmealService.page(setmealPage, queryWrapper);

        //创建封装了Setmeal DTO的page对象，并将setmealPage的数据除了records都复制过去
        Page<SetmealDto> setmealDtoPage = new Page<>();
        BeanUtils.copyProperties(setmealPage, setmealDtoPage, "records");

        //处理records
        //提取setmealPage里所有的套餐对象并进行遍历，结果封装成一个集合
        List<Setmeal> setmeals = setmealPage.getRecords();
        List<SetmealDto> setmealDtos = setmeals.stream().map((setmeal) -> {
            //对象拷贝
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(setmeal, setmealDto);

            //根据id查询套餐分类
            Long categoryId = setmeal.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                //用拿到的套餐分类给setmealDto赋值
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).toList();

        //用处理好的records给setmealDtoPage赋值
        setmealDtoPage.setRecords(setmealDtos);

        return Result.success(setmealDtoPage);
    }

    @DeleteMapping
    public Result<String> deleteByIds(Long[] ids) {
        setmealService.deleteByIdsWithDish(ids);
        return Result.success("删除成功");
    }
}
