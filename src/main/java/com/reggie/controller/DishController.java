package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.Result;
import com.reggie.domain.Category;
import com.reggie.domain.Dish;
import com.reggie.domain.DishFlavor;
import com.reggie.dto.DishDto;
import com.reggie.service.CategoryService;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto 包含菜品与口味的数据
     * @return 成功信息
     */
    @PostMapping
    @CacheEvict(value = "dishCache", allEntries = true)
    public Result<String> save(@RequestBody DishDto dishDto) {
        log.info("新增菜品: {}", dishDto);
        dishService.saveWithFlavor(dishDto);

        //清理所有Redis缓存
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

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
        log.info("分页查询：当前页为{}", page);

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
        List<DishDto> dishDtoList = dishes.stream().map((dish) -> {
            //将每个菜品数据复制到dishDto上
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(dish, dishDto);

            //根据菜品id查找到分类名，再给dishDto赋值
            Long categoryId = dish.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                dishDto.setCategoryName(category.getName());
            }
            return dishDto;
        }).toList();

        //给dishDtoPage赋值
        dishDtoPage.setRecords(dishDtoList);

        return Result.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品以及口味
     * @param id 菜品id
     * @return 封装了菜品和口味的DTO对象
     */
    @GetMapping("/{id}")
    public Result<DishDto> getById(@PathVariable Long id) {
        log.info("查询id为{}的菜品", id);
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return Result.success(dishDto);
    }

    /**
     * 修改菜品以及口味
     * @param dishDto 封装了flavors和dish数据的DTO
     */
    @PutMapping
    @CacheEvict(value = "dishCache", key = "#dishDto.categoryId + '_1'")
    public Result<String> update(@RequestBody DishDto dishDto) {
        log.info("更新菜品：{}", dishDto.getName());
        dishService.updateWithFlavor(dishDto);

        //清理所有Redis缓存
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        //精确清理缓存
        //String key = "dish_" + dishDto.getCategoryId() + "_1";
        //redisTemplate.delete(key);

        return Result.success("更新成功");
    }

    /**
     * 删除菜品
     * @param ids 前端的id数组
     */
    @DeleteMapping
    @CacheEvict(value = "dishCache", allEntries = true)
    public Result<String> deleteByIds(Long[] ids) {
        log.info("根据id删除菜品：{}", Arrays.toString(ids));
        dishService.deleteByIdsWithFlavor(ids);

        //清理所有Redis缓存
        //Set keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);

        return Result.success("删除成功");
    }

    /**
     * 启用/禁用菜品
     * @param ids id数组
     * @param status 操作完成后的状态
     * @return 成功信息
     */
    @PostMapping("/status/{status}")
    @CacheEvict(value = "dishCache", allEntries = true)
    public Result<String> changeStatus(Long[] ids, @PathVariable int status) {
        log.info("根据id启用/禁用菜品: {}", Arrays.toString(ids));

        List<Dish> dishes = Arrays.stream(ids).map((id) -> {
            //把每个id存入一个dish对象
            Dish dish = new Dish();
            dish.setId(id);

            //更新status
            dish.setStatus(status);
            return dish;
        }).toList();

        //调用批量更新方法
        dishService.updateBatchById(dishes);

        //清理所有Redis缓存
        //Set keys = redisTemplate.keys("dish_*");
        //isTemplate.delete(keys);

        return Result.success("状态更新成功");
    }

    /**
     * 根据分类id查询数据
     * @param dish 封装了分类id
     * @return 菜品DTO集合
     */
    @GetMapping("/list")
    @Cacheable(value = "dishCache", key = "#dish.categoryId + '_' + #dish.status")
    public Result<List<DishDto>> getByList(Dish dish) {
        log.info("根据分类id为{}查询菜品", dish.getCategoryId());

        List<DishDto> dishDtoList;

        //构造key
        //String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();

        //从Redis 获取缓存数据
        //dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        //如果存在，则直接返回
        //if (dishDtoList != null) {
            //return Result.success(dishDtoList);
        //}

        //如果不存在，则查询数据库

        //设置条件查询: select * from dish where category_id = {category_id}
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dish::getCategoryId, dish.getCategoryId());
        List<Dish> dishes = dishService.list(queryWrapper);

        //遍历菜品集合，对每个菜品进行查询然后封装到DishDto对象里
        dishDtoList = dishes.stream().map((eachDish) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(eachDish, dishDto);

            //给categoryName属性赋值
            Long categoryId = eachDish.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            //添加条件：SELECT * FROM dish_flavor WHERE dish_id = ?
            Long dishId = eachDish.getId();
            LambdaQueryWrapper<DishFlavor> dishFlavorWrapper = new LambdaQueryWrapper<>();
            dishFlavorWrapper.eq(DishFlavor::getDishId, dishId);
            List<DishFlavor> dishFlavors = dishFlavorService.list(dishFlavorWrapper);

            //给flavors属性赋值
            dishDto.setFlavors(dishFlavors);
            return dishDto;
        }).toList();

        //缓存到Redis
        //redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);

        return Result.success(dishDtoList);
    }
}