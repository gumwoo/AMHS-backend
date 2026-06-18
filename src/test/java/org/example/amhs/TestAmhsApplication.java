package org.example.amhs;

import org.springframework.boot.SpringApplication;

public class TestAmhsApplication {

    public static void main(String[] args) {
        SpringApplication.from(AmhsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
