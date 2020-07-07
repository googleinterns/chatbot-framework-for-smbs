package com.chatbot.services.asyncservices;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// config class for the executor of async threads

@Configuration
@EnableAsync
public class AsyncConfiguration 
{
  @Bean(name = "asyncExecutor")
  public Executor asyncExecutor(@Value("${asyncConfig.maxPoolSize}") final String maxPoolSize, 
      @Value("${asyncConfig.corePoolSize}") final String corePoolSize,
      @Value("${asyncConfig.queueCapacity}") final String queueCapacity,
      @Value("${asyncConfig.threadNamePrefix}") final String threadNamePrefix) {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(Integer.parseInt(corePoolSize));
    executor.setMaxPoolSize(Integer.parseInt(maxPoolSize));
    executor.setQueueCapacity(Integer.parseInt(queueCapacity));
    executor.setThreadNamePrefix(threadNamePrefix);
    executor.initialize();
    return executor;
  }
}
