package com.spacemate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.spacemate.infrastructure.persistence.mapper")
public class SpaceMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpaceMateApplication.class, args);
    }
}



