package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.Result;
import com.reggie.domain.Category;
import com.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@Slf4j
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分类
     * @param category 前端传来的分类数据
     * @return 成功/失败信息
     */
    @PostMapping
    public Result<String> save(@RequestBody Category category) {
        log.info("新增分类：{}", category);
        categoryService.save(category);
        return Result.success("新增成功");
    }

    /**
     * 分页查询
     * @param page 当前页
     * @param pageSize 每页展示条数
     * @return 带有page对象的Result
     */
    @GetMapping("/page")
    public Result<Page<Category>> getByPage(int page, int pageSize) {
        log.info("当前页：{}", page);
        log.info("每页条数：{}", pageSize);
        Page<Category> pageInfo = new Page<>(page, pageSize);

        //添加排序条件
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Category::getSort);

        //调用方法
        categoryService.page(pageInfo, queryWrapper);

        return Result.success(pageInfo);
    }

    /**
     * 根据id删除分类
     * @param id 分类id
     * @return 成功/失败信息
     */
    @DeleteMapping
    public Result<String> deleteById(@RequestParam("ids") Long id) {
        log.info("删除id为{}的分类", id);
        categoryService.remove(id);
        return Result.success("删除成功");
    }

    /**
     * 根据id修改分类信息
     * @param category 传过来的更新分类数据
     * @return 成功/失败信息
     */
    @PutMapping
    public Result<String> update(@RequestBody Category category) {
        log.info("更新分类：{}", category.toString());
        categoryService.updateById(category);
        return Result.success("修改成功");
    }

    /**
     * 根据条件查询分类数据
     * @param category 前端发来的type属性被封装到了Category对象里
     * @return 查询到的category的集合
     */
    @GetMapping("/list")
    public Result<List<Category>> getByType(Category category) {
        log.info("查询类别为{}的分类", category.getType());
        //条件构造器
        //SELECT * FROM dish_flavor WHERE type = {type} ORDER BY sort ASC, update_time DESC
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(category.getType() != null, Category::getType, category.getType());
        lambdaQueryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);

        //调用方法
        List<Category> categories = categoryService.list(lambdaQueryWrapper);
        return Result.success(categories);
    }
}
