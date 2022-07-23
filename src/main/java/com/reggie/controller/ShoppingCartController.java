package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.Result;
import com.reggie.config.BaseContext;
import com.reggie.domain.ShoppingCart;
import com.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 查询所有
     * @return 成功信息
     */
    @GetMapping("/list")
    public Result<List<ShoppingCart>> getByList() {
        log.info("查询购物车");

        //条件查询：SELECT * FROM shopping_cart WHERE user_id = ? ORDER BY create_time ASC
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);

        List<ShoppingCart> shoppingCarts = shoppingCartService.list(queryWrapper);

        return Result.success(shoppingCarts);
    }

    /**
     * 添加菜品到购物车
     * @param shoppingCart 购物车对象
     * @return 已经更新过/添加完的购物车对象
     */
    @PostMapping("/add")
    public Result<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart) {
        log.info("添加到购物车：{}", shoppingCart.toString());

        //给userId赋值
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        //添加条件user_id = ?
        LambdaQueryWrapper<ShoppingCart> shoppingCartWrapper = new LambdaQueryWrapper<>();
        shoppingCartWrapper.eq(ShoppingCart::getUserId, userId);

        Long dishId = shoppingCart.getDishId();
        if (dishId != null) {
            //说明传来的对象是菜品对象：dish_id = ?
            shoppingCartWrapper.eq(ShoppingCart::getDishId, dishId);
        } else {
            //说明传来的对象是套餐对象：setmeal_id = ?
            shoppingCartWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }

        //条件查询：SELECT * FROM shopping_cart WHERE user_id = ? AND dish_id/setmeal_id = ?
        ShoppingCart shoppingCartInTbl = shoppingCartService.getOne(shoppingCartWrapper);

        //判断前端的shoppingCart是否已存在表中
        if (shoppingCartInTbl != null) {
            //是，则进行更新操作
            Integer number = shoppingCartInTbl.getNumber();
            shoppingCartInTbl.setNumber(number + 1);
            shoppingCartService.updateById(shoppingCartInTbl);
        } else {
            //否，则进行添加操作
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            shoppingCartInTbl = shoppingCart;
        }

        return Result.success(shoppingCartInTbl);
    }

    /**
     * 从购物车移除菜品
     * @param shoppingCart 购物车对象
     * @return 删除成功信息
     */
    @PostMapping("/sub")
    public Result<String> subtract(@RequestBody ShoppingCart shoppingCart, HttpSession session) {
        log.info("从购物车移除菜品：{}", shoppingCart.toString());

        //判断需要删除的是菜品还是套餐
        Long dishId = shoppingCart.getDishId();
        Long userId = (Long) session.getAttribute("user");
        LambdaQueryWrapper<ShoppingCart> shoppingCartWrapper = new LambdaQueryWrapper<>();
        shoppingCartWrapper.eq(ShoppingCart::getUserId, userId);
        if (dishId != null) {
            //添加条件：SELECT FROM shopping_cart WHERE dish_id = ?
            shoppingCartWrapper.eq(ShoppingCart::getDishId, dishId);
        } else {
            //添加条件：SELECT FROM shopping_cart WHERE setmeal_id = ?
            Long setmealId = shoppingCart.getSetmealId();
            shoppingCartWrapper.eq(ShoppingCart::getSetmealId, setmealId);
        }

        ShoppingCart shoppingCartInTbl = shoppingCartService.getOne(shoppingCartWrapper);
        Integer number = shoppingCartInTbl.getNumber();
        //判断是需要减少数量还是删除
        if (number > 1) {
            shoppingCartInTbl.setNumber(number - 1);
            shoppingCartService.updateById(shoppingCartInTbl);
        } else {
            shoppingCartService.removeById(shoppingCartInTbl.getId());
        }

        return Result.success("删除成功");
    }

    /**
     * 清空购物车
     * @return 成功信息
     */
    @DeleteMapping("/clean")
    public Result<String> clean() {
        log.info("清空购物车");
        //添加条件：DELETE FROM shopping_cart WHERE user_id = ?
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        shoppingCartService.remove(queryWrapper);

        return Result.success("清空成功");
    }

}
