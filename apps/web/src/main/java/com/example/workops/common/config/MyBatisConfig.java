package com.example.workops.common.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** MyBatis Mapper interfaceを検出するためのアプリケーション共通設定。 */
@Configuration
@MapperScan(basePackages = "com.example.workops", annotationClass = Mapper.class)
public class MyBatisConfig {}
