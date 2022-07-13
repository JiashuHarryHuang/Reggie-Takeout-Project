package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.Result;
import com.reggie.config.BaseContext;
import com.reggie.domain.OrderDetail;
import com.reggie.domain.Orders;
import com.reggie.dto.OrderDto;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 提交订单
     * @param orders 订单对象
     */
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody Orders orders) {
        log.info("用户下单：{}", orders.toString());
        orderService.submit(orders);
        return Result.success("下单成功");
    }

    /**
     * 分页查询订单
     * @param page 当前页
     * @param pageSize 每页显示条数
     * @param number 订单号
     * @param beginTime 下界
     * @param endTime 上界
     * @return 分页查询对象
     */
    @GetMapping("/page")
    public Result<Page<Orders>> getByPage(int page, int pageSize, Long number,
                                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date beginTime,
                                          @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        log.info("page = {}, pageSize = {}, number = {}, beginTime = {}, endTime = {}",
                page, pageSize, number, beginTime, endTime);

        Page<Orders> pageInfo = new Page<>(page, pageSize);

        //添加条件：SELECT FROM orders WHERE name like ? AND order_time BETWEEN ? and ?
        LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = new LambdaQueryWrapper<>();
        ordersLambdaQueryWrapper.like(number != null, Orders::getNumber, number);
        ordersLambdaQueryWrapper.between(beginTime != null && endTime != null,
                Orders::getOrderTime, beginTime, endTime);

        orderService.page(pageInfo, ordersLambdaQueryWrapper);
        return Result.success(pageInfo);
    }

    @GetMapping("/userPage")
    public Result<Page<OrderDto>> getByUserPage(int page, int pageSize) {
        log.info("page = {}, pageSize = {}", page, pageSize);

        //对order表进行分页查询：SELECT * FROM orders WHERE user_id = ?
        Page<Orders> ordersPage= new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> ordersQueryWrapper = new LambdaQueryWrapper<>();
        Long userId = BaseContext.getCurrentId();
        ordersQueryWrapper.eq(Orders::getUserId, userId);
        orderService.page(ordersPage, ordersQueryWrapper);

        //将查询结果复制到orderDtoPage
        Page<OrderDto> orderDtoPage = new Page<>();
        BeanUtils.copyProperties(ordersPage, orderDtoPage, "records");

        //对records进行处理
        List<Orders> orders = ordersPage.getRecords();
        List<OrderDto> orderDtos = orders.stream().map((order) -> {
            //复制对象
            OrderDto orderDto = new OrderDto();
            BeanUtils.copyProperties(order, orderDto);

            //条件查询：SELECT * FROM order_detail WHERE order_id = ?
            //获取orderDetails
            LambdaQueryWrapper<OrderDetail> orderDetailWrapper = new LambdaQueryWrapper<>();
            orderDetailWrapper.eq(OrderDetail::getOrderId, order.getId());
            List<OrderDetail> orderDetails = orderDetailService.list(orderDetailWrapper);

            //给orderDto赋值
            orderDto.setOrderDetails(orderDetails);

            return orderDto;
        }).toList();

        orderDtoPage.setRecords(orderDtos);
        return Result.success(orderDtoPage);
    }
}
