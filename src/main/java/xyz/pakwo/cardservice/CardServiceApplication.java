package xyz.pakwo.cardservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import xyz.pakwo.cardservice.config.CardServiceProperties;

@SpringBootApplication
@EnableConfigurationProperties(CardServiceProperties.class)
public class CardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardServiceApplication.class, args);
    }

}
