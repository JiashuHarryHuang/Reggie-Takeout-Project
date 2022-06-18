package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.Result;
import com.reggie.domain.User;
import com.reggie.service.UserService;
import com.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 发送验证码（目前用控制台打印模拟）
     * @param user 封装了手机号码的数据
     * @param session 会话
     * @return 成功信息
     */
    @PostMapping("/sendMsg")
    public Result<String> sendMsg(@RequestBody User user, HttpSession session) {
        log.info("发送验证码");

        //获取手机号
        String phone = user.getPhone();

        if(StringUtils.isNotEmpty(phone)){
            //生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();

            //调用阿里云提供的短信服务API完成发送短信
            //SMSUtils.sendMessage("瑞吉外卖","",phone,code);

            //用打印日志模拟发送验证码
            log.info("code={}",code);

            //需要将生成的验证码保存到Session
            session.setAttribute(phone,code);

            return Result.success("手机验证码短信发送成功");
        }
        return Result.error("短信发送失败");
    }

    /**
     * 用户登录操作
     * @param user 封装了用户手机号和验证码
     * @param session 会话
     * @return 成功信息
     */
    @RequestMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> user, HttpSession session) {
        log.info("用户登录");
        //获取用户输入的验证码
        String enteredCode = user.get("code");

        //获取用户输入的手机号
        String phone = user.get("phone");

        //获取session中的验证码
        String code = (String) session.getAttribute(phone);

        //比对
        if (code != null && code.equals(enteredCode)) {
            //检查账号是否已存在
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getPhone, phone);
            User currUser = userService.getOne(userWrapper);

            //如果未存在，则新建账户
            if (currUser == null) {
                currUser = new User();
                currUser.setPhone(phone);
                currUser.setStatus(1);
                userService.save(currUser);
            }

            //将id存入session
            session.setAttribute("user", currUser.getId());
            return Result.success("登陆成功");
        }
        return Result.error("登录失败");
    }

    /**
     * 登出操作
     * @param session 会话
     * @return 成功信息
     */
    @PostMapping("/loginout")
    public Result<String> logout(HttpSession session) {
        log.info("用户已登出");
        session.removeAttribute("user");
        return Result.success("登出成功");
    }
}
