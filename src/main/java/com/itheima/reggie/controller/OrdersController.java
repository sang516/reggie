package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrdersService;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrdersController {
    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("submit")
    public R<String> submit(@RequestBody Orders orders){
        ordersService.submit(orders);
        return R.success("订单已提交");
    }

    @GetMapping("/userPage")
    public R<Page> get(int page,int pageSize){
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        Page<OrdersDto> pageDto = new Page<>();
        ordersService.page(pageInfo);
        BeanUtils.copyProperties(pageInfo,pageDto,"records");
        List<Orders> records = pageInfo.getRecords();
        List<OrdersDto> list = records.stream().map((item)->{
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item,ordersDto);
            Long orderId = item.getId();
            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderDetail::getOrderId,orderId);
            List<OrderDetail> orderDetails = orderDetailService.list(wrapper);
            ordersDto.setOrderDetails(orderDetails);
            Orders order = ordersService.getById(orderId);
            if (order != null) {
                ordersDto.setPhone(order.getPhone());
                ordersDto.setAddress(order.getAddress());
                ordersDto.setConsignee(order.getConsignee());
                ordersDto.setUserName(order.getUserName());
            }
            return ordersDto;
        }).collect(Collectors.toList());
        //将DishDt列表赋值给pageDishDto
        pageDto.setRecords(list);
        log.info(pageDto.toString());
        return R.success(pageDto);
    }

    /**
     * 再来一单
     * @param orders
     * @return
     */
    @PostMapping("/again")
    public R<ShoppingCart> again(@RequestBody Orders orders){
        //根据order_id从orderDetail表中查询数据
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId,orders.getId());
        OrderDetail orderDetail = orderDetailService.getOne(wrapper);
        //根据查询到的订单数据设置shoppingCart的属性
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setName(orderDetail.getName());
        shoppingCart.setImage(orderDetail.getImage());
        shoppingCart.setDishFlavor(orderDetail.getDishFlavor());
        shoppingCart.setNumber(orderDetail.getNumber());
        shoppingCart.setAmount(orderDetail.getAmount());
        //获取并设置user_id
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        //获取菜品id
        Long dishId = orderDetail.getDishId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,userId);
        //设置菜品或套装id
        if (dishId != null){
            shoppingCart.setDishId(dishId);
            queryWrapper.eq(ShoppingCart::getDishId,dishId);
        }else {
            shoppingCart.setSetmealId(orderDetail.getSetmealId());
            queryWrapper.eq(ShoppingCart::getSetmealId,orderDetail.getSetmealId());
        }
        //查询购物车表中数据，判断是否存在购物车信息
        ShoppingCart shoppingCart2 = shoppingCartService.getOne(queryWrapper);
        if (shoppingCart2 != null){
            Integer number = shoppingCart2.getNumber();
            shoppingCart2.setNumber(number + orderDetail.getNumber());
            shoppingCartService.updateById(shoppingCart2);//存在，更新购物车信息
        }else {//不存在，插入数据
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            shoppingCart2 = shoppingCart;
        }
        return R.success(shoppingCart2);
    }

    /**
     * 订单查询
     * @param page
     * @param pageSize
     * @param number
     * @param beginTime
     * @param endTime
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String number,String beginTime,String endTime){
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        if (beginTime != null && endTime != null) {
            DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime beginDateTime = LocalDateTime.parse(beginTime, sdf);
            LocalDateTime endDateTime = LocalDateTime.parse(endTime, sdf);
            queryWrapper.between(Orders::getOrderTime,beginDateTime,endDateTime);
        }
        queryWrapper.eq(number != null,Orders::getId,number);
        Page<Orders> ordersPage = new Page<>(page,pageSize);
        ordersService.page(ordersPage,queryWrapper);
        return R.success(ordersPage);
    }

    /**
     * 更新订单信息
     * @param order
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody Orders order){
        LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Orders::getId,order.getId());
        updateWrapper.set(Orders::getStatus,order.getStatus());
        ordersService.update(updateWrapper);
        return R.success("订单更新成功");
    }
}
