package com.shopping.onlineshop.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;


@Configuration
@EnableAsync
public class Config {

    @Bean
    public RestTemplate restTemplate (){
        return  new RestTemplate() ;
    }

    @Bean
    public Executor taskExecutor () {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor() ;
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ECOM-");
        executor.initialize();
        return executor ;
    }

    @Bean("asyncTaskExecutor")
    public Executor asynchExecutor () {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor() ;
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("TASK-");
        executor.initialize();
        return executor ;
    }

}
