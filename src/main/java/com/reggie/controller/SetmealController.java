package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.Result;
import com.reggie.domain.Category;
import com.reggie.domain.Setmeal;
import com.reggie.domain.SetmealDish;
import com.reggie.dto.DishDto;
import com.reggie.dto.SetmealDto;
import com.reggie.service.CategoryService;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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

    @Autowired
    private CacheManager cacheManager;

    /**
     * 新增套餐操作
     * @param setmealDto 封装了套餐和套餐菜品的数据
     * @return 成功信息
     */
    @PostMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    public Result<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("新增套餐{}", setmealDto.getName());
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
        log.info("page = {}, pageSize = {}, name = {}", page, pageSize, name);

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

    /**
     * 根据id删除
     * @param ids id数组
     * @return 成功信息
     */
    @DeleteMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    public Result<String> deleteByIds(Long[] ids) {
        log.info("根据id删除菜品：{}", Arrays.toString(ids));
        setmealService.deleteByIdsWithDish(ids);
        return Result.success("删除成功");
    }

    /**
     * 根据id查询套餐
     * @param id 套餐id
     * @return 封装了菜品和套餐的对象
     */
    @GetMapping("/{id}")
    public Result<SetmealDto> getById(@PathVariable Long id) {
        log.info("查询id为{}的菜品", id);
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        return Result.success(setmealDto);
    }

    /**
     * 修改套餐
     * @param setmealDto 用户提交的DTO对象
     */
    @PutMapping
    @CacheEvict(value = "setmealCache", allEntries = true)
    public Result<String> update(@RequestBody SetmealDto setmealDto) {
        log.info("更新菜品：{}", setmealDto.getName());
        setmealService.updateWithDish(setmealDto);
        return Result.success("修改成功");
    }

    /**
     * 启用/禁用套餐
     * @param ids id数组
     * @param status 操作完成后的状态
     * @return 成功信息
     */
    @PostMapping("/status/{status}")
    @CacheEvict(value = "setmealCache", allEntries = true)
    public Result<String> changeStatus(@PathVariable int status, Long[] ids) {
        log.info("根据id启用/禁用菜品: {}", Arrays.toString(ids));

        //把ids和status封装到一个Setmeal集合
        List<Setmeal> setmeals = Arrays.stream(ids).map((id) -> {
            Setmeal setmeal = new Setmeal();
            setmeal.setId(id);
            setmeal.setStatus(status);
            return setmeal;
        }).toList();

        //集合更新
        setmealService.updateBatchById(setmeals);

        return Result.success("修改成功");
    }

    /**
     * 根据分类id查询套餐
     * @param setmeal 封装了分类id和状态数据
     * @return 符合条件的套餐集合
     */
    @GetMapping("/list")
    @Cacheable(value = "setmealCache", key = "#setmeal.categoryId + '_' + #setmeal.status")
    public Result<List<Setmeal>> getSetmealDish(Setmeal setmeal) {
        log.info("根据分类id为{}查询菜品", setmeal.getCategoryId());

        //添加条件: SELECT * FROM setmeal WHERE category_id = ? AND status = ? ORDER BY update_time DESC
        LambdaQueryWrapper<Setmeal> setmealWrapper = new LambdaQueryWrapper<>();
        setmealWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        setmealWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        setmealWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> setmeals = setmealService.list(setmealWrapper);
        return Result.success(setmeals);
    }
}
