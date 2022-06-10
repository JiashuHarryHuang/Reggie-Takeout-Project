package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 异常处理类
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class}) //声明通知增强的类
@ResponseBody //表明返回的是JSON数据
@Slf4j
public class GlobalExceptionHandler {

    /**
     * sql异常处理方法
     * @param exception sql约束异常
     * @return 带有异常信息的Result对象
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<String> sqlExceptionHandler(SQLIntegrityConstraintViolationException exception) {
        log.error(exception.getMessage());

        //判断异常是否有关键信息
        if (exception.getMessage().contains("Duplicate entry")) {
            String[] message = exception.getMessage().split(" ");
            String errMsg = message[2] + "已存在";
            return Result.error(errMsg);
        }

        return Result.error("sql未知异常");
    }
}
