package com.github.pmoerenhout.jsmppmodem.app;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CharacterEncodingFilter;

@SpringBootApplication
public class Application implements CommandLineRunner {

  final static Logger LOG = LoggerFactory.getLogger(Application.class);

  @Autowired
  private ModemService modemService;

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(Application.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    // app.setHeadless(true);
    ConfigurableApplicationContext ctx = app.run(args);
  }

  public void run(String... args) throws Exception {
    LOG.info("Run...");
    modemService.init();
  }

  @Bean
  public CommandLineRunner commandLineRunner(final ApplicationContext ctx) {
    return args -> {
      LOG.debug("Let's inspect the beans provided by Spring Boot:");
      Arrays.asList(ctx.getBeanDefinitionNames()).stream().sorted().forEach(b -> LOG.debug("Bean name: {}", b));
    };
  }

  @Bean
  public CharacterEncodingFilter characterEncodingFilter() {
    final CharacterEncodingFilter filter = new CharacterEncodingFilter();
    filter.setEncoding("UTF-8");
    filter.setForceEncoding(true);
    return filter;
  }

}