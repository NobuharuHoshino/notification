
package com.toyota.tsc.notificationhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import jp.toyota.res.common.utils.ALJToken;

@SpringBootApplication
public class NotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }

    @Bean
    public ALJToken getALJToken() {
        return new ALJToken();
    }

}
