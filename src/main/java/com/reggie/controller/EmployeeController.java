package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.Result;
import com.reggie.domain.Employee;
import com.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

@RestController //@ResponseBody + @Controller
@RequestMapping("/employee")
@Slf4j
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    /**
     * 用户登陆操作
     * @param request 用于将用户信息存入session
     * @param employee 封装了前端发送的用户名和密码
     * @return 装有该员工信息的Result对象
     */
    @PostMapping("/login")
    //Use request to store data into session
    public Result<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        //1. 将密码加密
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2. 根据用户名查询数据库，如果为null则登录失败
        LambdaQueryWrapper<Employee> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Employee::getUsername, employee.getUsername());

        //查询单个字段
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

    /**
     * 员工登出操作
     * @param request 用于删除session里的数据
     * @return 装有退出成功信息的Result对象
     */
    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        //1. 释放session数据
        request.getSession().removeAttribute("employee");
        //2. 返回结果
        return Result.success("退出成功");
    }

    /**
     * 新增员工操作
     * @param request 用于从session获取当前员工的id
     * @param employee 前端传过来的新员工对象
     * @return 装有新增成功信息的Result对象
     */
    @PostMapping
    public Result<String> save(HttpServletRequest request, @RequestBody Employee employee) {
//        log.info("新增员工：{}", employee.toString());

        //设置员工初始密码并进行加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));


        //初始化创建及更新时间，已在MyMetaObjectHandler处理
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
//
//        //当前登录用户的id，也就是负责添加员工的人
//        Long currId = (Long) request.getSession().getAttribute("employee");
//        employee.setCreateUser(currId);
//        employee.setUpdateUser(currId);

        //调用service方法
        if(employeeService.save(employee)) {
            return Result.success("新增成功");
        } else {
            return Result.error("新增失败");
        }
    }

    /**
     * 分页查询
     * @param page 当前页
     * @param pageSize 总页数
     * @param name 查询字段
     * @return 装有分页数据的Result对象
     */
    @GetMapping("/page")
    public Result<Page<Employee>> selectByPage(int page, int pageSize, String name) {
        log.info("page = {}, pageSize = {}, name = {}", page, pageSize, name);

        //1. 创建Page分页对象
        Page<Employee> pageInfo = new Page<>(page, pageSize);

        //2. 添加条件
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);

        //3. 排序
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        //4. 执行分页查询
        employeeService.page(pageInfo, queryWrapper);

        return Result.success(pageInfo);
    }

    /**
     * 员工信息更新
     * @param request 用于获取session数据
     * @param employee 前端传来的员工数据
     * @return 成功信息Result
     */
    @PutMapping
    public Result<String> update(HttpServletRequest request, @RequestBody Employee employee) {
        log.info(employee.toString());

        //更新“更新时间”和“负责更新的员工”，已在MyMetaObjectHandler处理
//        employee.setUpdateTime(LocalDateTime.now());
//        Long empId = (Long) request.getSession().getAttribute("employee");
//        employee.setUpdateUser(empId);
//        long id = Thread.currentThread().getId();
//        log.info("当前线程: {}", id);

        //调用IService方法
        employeeService.updateById(employee);

        return Result.success("员工信息修改成功");
    }

    /**
     * 根据id查询
     * @param id 前端传来的id数据
     * @return 带有员工的Result对象
     */
    @GetMapping("/{id}")
    public Result<Employee> getById(@PathVariable Long id) {
        log.info("根据id查询");
        Employee employee = employeeService.getById(id);
        if (employee != null) {
            return Result.success(employee);
        } else {
            return Result.error("该员工不存在");
        }
    }
}
