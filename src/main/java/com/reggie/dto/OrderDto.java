package com.reggie.dto;

import com.reggie.domain.OrderDetail;
import com.reggie.domain.Orders;
import lombok.Data;

import java.util.List;

@Data
public class OrderDto extends Orders {
    private List<OrderDetail> orderDetails;
}
