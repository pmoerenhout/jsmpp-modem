package com.github.pmoerenhout.jsmppmodem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

import com.github.pmoerenhout.jsmppmodem.smsc.SmppService;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class Application {

  final static Logger LOG = LoggerFactory.getLogger(Application.class);

  @Autowired
  private ModemService modemService;

  @Autowired
  private SmppService smppService;

  public static void main(final String[] args) {
    final SpringApplication app = new SpringApplication(Application.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    // app.setHeadless(true);
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
      LOG.info("Let's inspect the beans provided by Spring Boot:");
      Arrays.asList(ctx.getBeanDefinitionNames()).stream().sorted().forEach(b -> LOG.debug("Bean name: {}", b));
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

  @Bean
  @Qualifier("modems")
  public List<Modem> getModems() {
    final List<Modem> modems = new ArrayList<>();
    modems.add(new Modem("3ebe0c73-5f87-4c0c-96f6-91f8cd1c0088", "/dev/tty.usbserial-00101314B", 115200));
    return modems;
  }

  @Bean
  @Qualifier("smppTaskExecutor")
  public TaskExecutor getSmppTaskExecutor() {
    final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(5);
    threadPoolTaskExecutor.setMaxPoolSize(10);
    threadPoolTaskExecutor.setQueueCapacity(25);
    threadPoolTaskExecutor.setThreadNamePrefix("smpp-");
    return threadPoolTaskExecutor;
  }

  @Bean(name = "threadPoolTaskExecutor")
  public Executor threadPoolTaskExecutor() {
    return new ThreadPoolTaskExecutor();
  }

  @Bean
  public Executor asyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("GithubLookup-");
    executor.initialize();
    return executor;
  }
}