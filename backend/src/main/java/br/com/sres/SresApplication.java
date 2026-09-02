package br.com.sres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@Modulithic
@EnableConfigurationProperties(SresProperties.class)
public class SresApplication {
    public static void main(String[] args) {
        SpringApplication.run(SresApplication.class, args);
    }
}
