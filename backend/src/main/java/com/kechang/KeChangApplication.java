package com.kechang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 课畅（KeChang）— 智能化在线教学支持服务平台 SpringBoot 主启动类
 */
@SpringBootApplication
@MapperScan("com.kechang.mapper")
@EnableScheduling
public class KeChangApplication {

    public static void main(String[] args) {
        SpringApplication.run(KeChangApplication.class, args);
        System.out.println("========================================");
        System.out.println("  课畅 KeChang 后端服务启动成功！");
        System.out.println("  API文档: http://localhost:8080/swagger-ui.html");
        System.out.println("========================================");
    }
}
