package com.jpmc.midascore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.jpmc.midascore"})
public class MidasCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(MidasCoreApplication.class, args);
    }

}
