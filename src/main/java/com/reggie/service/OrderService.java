package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.domain.Orders;

public interface OrderService extends IService<Orders> {
    /**
     * 提交订单
     * @param orders 订单对象
     */
    void submit(Orders orders);
}
