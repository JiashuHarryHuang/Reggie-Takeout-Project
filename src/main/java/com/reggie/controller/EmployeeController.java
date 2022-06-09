package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.Result;
import com.reggie.domain.Employee;
import com.reggie.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    @PostMapping("/login")
    //Use request to store data into session
    public Result<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        //1. 将密码加密
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2. 根据用户名查询数据库，如果为null则登录失败
        LambdaQueryWrapper<Employee> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(lqw);
        if (emp == null){
            return Result.error("登录失败");
        }
        //3. 根据密码查询数据库，如果为null则登录失败
        if (!emp.getPassword().equals(password)) {
            return Result.error("登录失败");
        }

        //4. 查看员工是否被禁用，如果为0则登录失败
        if (emp.getStatus() == 0) {
            return Result.error("账号已禁用");
        }

        //5. 将id存入session，登录成功
        HttpSession session = request.getSession();
        session.setAttribute("employee", emp.getId());
        return Result.success(emp);
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        //1. 释放session数据
        request.getSession().removeAttribute("employee");
        //2. 返回结果
        return Result.success("退出成功");
    }
}
