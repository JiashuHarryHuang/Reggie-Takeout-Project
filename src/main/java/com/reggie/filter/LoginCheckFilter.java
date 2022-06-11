package com.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.reggie.common.Result;
import com.reggie.config.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    //用于做路径比较的工具类，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        //1. 判断请求是否需处理，如果否则直接放行
        String requestURI = request.getRequestURI();
//        log.info("拦截到请求：{}", requestURI);

        //不需要处理的路径
        String[] uris = {
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**"
        };

        boolean check = checkURI(uris, requestURI);

        //对不需要处理的路径放行
        if (check) {
//            log.info("本次请求{}不需要处理", requestURI);
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        //2. 判断是否已登录，如果否则跳转登陆界面，是则放行
        Long empId = (Long) request.getSession().getAttribute("employee");
        if (empId != null) {
//            log.info("用户已登录，id为{}", request.getSession().getAttribute("employee"));
            BaseContext.setCurrentId(empId);
            filterChain.doFilter(servletRequest, servletResponse);
//            long id = Thread.currentThread().getId();
//            log.info("当前线程: {}", id);
        } else {
//            log.info("用户未登录");
            response.getWriter().write(JSON.toJSONString(Result.error("NOTLOGIN")));
        }
    }

    private boolean checkURI(String[] uris, String requestURI) {
        for (String uri : uris) {
            if (PATH_MATCHER.match(uri, requestURI)){
                return true;
            }
        }
        return false;
    }
}
