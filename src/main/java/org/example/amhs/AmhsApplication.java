package org.example.amhs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class AmhsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmhsApplication.class, args);
    }

}
