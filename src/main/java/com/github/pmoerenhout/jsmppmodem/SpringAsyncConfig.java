//package com.github.pmoerenhout.jsmppmodem;
//
//import java.util.concurrent.Executor;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.AsyncConfigurer;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//@Configuration
//@EnableAsync
//public class SpringAsyncConfig implements AsyncConfigurer {
//
//  private static final Logger log = LoggerFactory.getLogger(SpringAsyncConfig.class);
//
//  public Executor getAsyncExecutor() {
//    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//    executor.setCorePoolSize(5);
//    executor.setMaxPoolSize(20);
//    executor.setQueueCapacity(500);
//    executor.setThreadNamePrefix("AsyncExecutor-");
//    executor.initialize();
//    return executor;
//  }
//
//  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
//    return (t, m, o) -> {
//      log.error("Method:" + m + " Object:" + o, t);
//    };
//  }
//
//}