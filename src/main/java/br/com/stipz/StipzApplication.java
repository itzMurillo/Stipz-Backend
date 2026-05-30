package br.com.stipz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StipzApplication {

    public static void main(String[] args) {
        SpringApplication.run(StipzApplication.class, args);
    }

}
