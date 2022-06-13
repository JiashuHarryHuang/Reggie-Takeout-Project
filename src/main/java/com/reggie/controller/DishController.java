package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.Result;
import com.reggie.domain.Category;
import com.reggie.domain.Dish;
import com.reggie.dto.DishDto;
import com.reggie.service.CategoryService;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品
     * @param dishDto 包含菜品与口味的数据
     * @return 成功信息
     */
    @PostMapping
    public Result<String> save(@RequestBody DishDto dishDto) {
        dishService.saveWithFlavor(dishDto);
        return Result.success("新增成功");
    }

    /**
     * 分页查询
     * @param page 当前页
     * @param pageSize 每页条数
     * @param name 输入框信息
     * @return 带有DishDto的Result对象
     */
    @GetMapping("/page")
    public Result<Page<DishDto>> getByPage(int page, int pageSize, String name) {

        //创建page对象
        Page<Dish> dishPage = new Page<>(page, pageSize);

        //因为前端要求返回对象带有categoryName属性，所以需要在DishDto加上这个属性
        //返回的Result对象也需要是DishDto类型的
        Page<DishDto> dishDtoPage = new Page<>();

        //在菜品表进行条件查询：Select * from dish where name like {name} order by update_time asc
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(name != null, Dish::getName, name);
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行查询
        dishService.page(dishPage, lambdaQueryWrapper);

        //将dishPage的数据复制到dishDtoPage上，但records属性不能复制，后面需要进行处理
        BeanUtils.copyProperties(dishPage, dishDtoPage, "records");

        //提取全部菜品数据并封装成集合
        List<Dish> dishes = dishPage.getRecords();

        //用来装处理完的dishDto
        List<DishDto> dishDtoList = new ArrayList<>();

        //遍历菜品
        for (Dish dish : dishes) {
            //将每个菜品数据复制到dishDto上
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(dish, dishDto);

            //根据菜品id查找到分类名，再给dishDto赋值
            Long categoryId = dish.getCategoryId();
            Category category = categoryService.getById(categoryId);
            dishDto.setCategoryName(category.getName());
            dishDtoList.add(dishDto);
        }

        //给dishDtoPage赋值
        dishDtoPage.setRecords(dishDtoList);

        return Result.success(dishDtoPage);
    }

    @GetMapping("/{id}")
    public Result<DishDto> getById(@PathVariable Long id) {
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return Result.success(dishDto);
    }

    @PutMapping
    public Result<String> update(@RequestBody DishDto dishDto) {
        dishService.updateWithFlavor(dishDto);
        return Result.success("更新成功");
    }
}
