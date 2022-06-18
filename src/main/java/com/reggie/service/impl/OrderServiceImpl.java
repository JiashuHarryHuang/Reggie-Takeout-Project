package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.config.BaseContext;
import com.reggie.dao.OrderDao;
import com.reggie.domain.*;
import com.reggie.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderDao, Orders> implements OrderService {
    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 提交订单
     * @param orders 订单对象
     */
    @Override
    @Transactional
    public void submit(Orders orders) {
        //查询用户数据
        Long userId = BaseContext.getCurrentId();
        User user = userService.getById(userId);

        //查询购物车数据
        LambdaQueryWrapper<ShoppingCart> shoppingCartWrapper = new LambdaQueryWrapper<>();
        shoppingCartWrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(shoppingCartWrapper);

        //确认数据
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new CustomException("购物车不能为空");
        }

        //查询地址数据
        Long addressId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressId);

        //确认数据
        if (addressBook == null) {
            throw new CustomException("用户地址信息有误，不能下单");
        }

        //生成订单号
        Long orderNumber = IdWorker.getId();

        //原子类，用来保证在高并发情况下的线程安全
        AtomicInteger amount = new AtomicInteger(0);

        //计算总金额，并将购物车对象转成OrderDetail对象
        List<OrderDetail> orderDetailList = shoppingCartList.stream().map(shoppingCart -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderNumber);
            orderDetail.setNumber(shoppingCart.getNumber());
            orderDetail.setDishFlavor(shoppingCart.getDishFlavor());
            orderDetail.setDishId(shoppingCart.getDishId());
            orderDetail.setSetmealId(shoppingCart.getSetmealId());
            orderDetail.setName(shoppingCart.getName());
            orderDetail.setImage(shoppingCart.getImage());
            orderDetail.setAmount(shoppingCart.getAmount());
            
            //计算当前菜品金额: amount * number
            amount.addAndGet(shoppingCart.getAmount()
                    .multiply(new BigDecimal(shoppingCart.getNumber()))
                    .intValue());
            return orderDetail;
        }).toList();

        //填充orders的属性
        orders.setId(orderNumber);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        //2代表待派送
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));//总金额
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderNumber));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));

        //存入orders表
        this.save(orders);

        //存入order_detail表
        orderDetailService.saveBatch(orderDetailList);

        //清空购物车
        shoppingCartService.remove(shoppingCartWrapper);
    }
}
