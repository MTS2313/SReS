package br.com.sres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Modulithic
@EnableConfigurationProperties(SresProperties.class)
@EnableScheduling
public class SresApplication {
    public static void main(String[] args) {
        SpringApplication.run(SresApplication.class, args);
    }
}
