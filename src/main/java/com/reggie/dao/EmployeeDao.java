package com.reggie.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.domain.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeDao extends BaseMapper<Employee> {
}
