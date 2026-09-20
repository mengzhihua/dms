package com.dms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.dms.**.mapper")
public class DmsApplication {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(DmsApplication.class, args);
    }
}
