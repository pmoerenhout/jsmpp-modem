package com.github.pmoerenhout.jsmppmodem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.filter.CharacterEncodingFilter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class Application {

//  @Autowired
//  private ModemService modemService;
//
//  @Autowired
//  private SmppService smppService;

  public static void main(final String[] args) {
    log.info("Main with arguments {}", args);
    final SpringApplication app = new SpringApplication(Application.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    app.setHeadless(true);
    ConfigurableApplicationContext ctx = app.run(args);
  }

//  public void run(final String... args) throws Exception {
//    LOG.info("Run...");
//    smppService.start();
//
//    modemService.init();
//    // smppService.start();
//    final Modem modem = modemService.getFirstModem();
//    LOG.info("Is first modem {} initialized? {}", modem.getId(), modem.isInitialized());
//
//    // modemService.deleteAllMessage(modem);
//    // modemService.send(modem, "31635778003", 1);
//    // modemService.send(modem, "31614240689", 1);
//    // modemService.send(modem, "31635930247", 100);
//    // modemService.send("31687263195", 10);
//
//    for (int i = 0; i < 50; i++) {
//      Thread.sleep(10000);
//      modemService.showAllMessages(modem);
//    }
//
//    modemService.close();
//
//    smppService.stop();
//  }

  @Bean
  public CommandLineRunner commandLineRunner(final ApplicationContext ctx) {
    return args -> {
      log.info("Let's inspect the beans provided by Spring Boot:");
      Arrays.asList(ctx.getBeanDefinitionNames()).stream().sorted().forEach(b -> log.debug("Bean name: {}", b));
    };
  }

//  @Bean
//  @Qualifier("unsolicitedCallback")
//  public UnsolicitedCallback getServerMessageReceiverListener() {
//    return new UnsolicitedCallback("sss");
//  }

  @Bean
  public CharacterEncodingFilter characterEncodingFilter() {
    final CharacterEncodingFilter filter = new CharacterEncodingFilter();
    filter.setEncoding("UTF-8");
    filter.setForceEncoding(true);
    return filter;
  }

  @Bean("modems")
  public List<Modem> getModems(@Value("${modem.port}") final String port,
                               @Value("${modem.speed:115200}") final int speed,
                               @Value("${modem.flow-control:RTSCTS}") final FlowControl flowControl) {
    final List<Modem> modems = new ArrayList<>();
    modems.add(getModem(port, speed, flowControl));
    return modems;
  }

  public Modem getModem(final String port, final int speed, final FlowControl flowControl) {
    return new Modem(UUID.randomUUID().toString(), port, speed, flowControl);
  }

  @Bean("smppTaskExecutor")
  public TaskExecutor smppTaskExecutor() {
    final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(2);
    threadPoolTaskExecutor.setMaxPoolSize(5);
    threadPoolTaskExecutor.setQueueCapacity(25);
    threadPoolTaskExecutor.setThreadNamePrefix("smpp-");
    return threadPoolTaskExecutor;
  }

  @Bean("taskExecutor")
  public Executor threadPoolTaskExecutor() {
    final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(2);
    threadPoolTaskExecutor.setMaxPoolSize(5);
    threadPoolTaskExecutor.setQueueCapacity(25);
    threadPoolTaskExecutor.setThreadNamePrefix("thread-");
    return threadPoolTaskExecutor;
  }

//  @Bean
//  public Executor asyncExecutor() {
//    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//    executor.setCorePoolSize(2);
//    executor.setMaxPoolSize(5);
//    executor.setQueueCapacity(500);
//    executor.setThreadNamePrefix("async-");
//    executor.initialize();
//    return executor;
//  }
}