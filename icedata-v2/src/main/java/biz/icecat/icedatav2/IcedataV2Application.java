package biz.icecat.icedatav2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class IcedataV2Application {
    public static void main(String[] args) {
        SpringApplication.run(IcedataV2Application.class, args);
    }
}
