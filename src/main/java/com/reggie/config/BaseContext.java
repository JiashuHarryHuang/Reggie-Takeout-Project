package com.reggie.config;

/**
 * ThreadLocal工具类，用于保存和获取用户id
 */
public class BaseContext {
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 在ThreadLocal存储id
     * @param id 需要存储的id
     */
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    /**
     * 从ThreadLocal获取id
     * @return id
     */
    public static Long getCurrentId() {
        return threadLocal.get();
    }
}
