package io.amberdata.inbound.stellar;

import io.amberdata.inbound.core.InboundCore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

@SpringBootApplication(exclude = {GsonAutoConfiguration.class})
@ComponentScan(basePackageClasses = {InboundCore.class, StellarInboundApplication.class})
public class StellarInboundApplication implements CommandLineRunner {
  private static CountDownLatch EXIT_LATCH;

  private final Environment environment;

  @Autowired
  public StellarInboundApplication(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void run(String... args) throws Exception {
    boolean testProfileDisabled = Arrays
        .stream(this.environment.getActiveProfiles())
        .noneMatch(profile -> profile.equalsIgnoreCase("test"));

    if (testProfileDisabled) {
      EXIT_LATCH = new CountDownLatch(1);
      EXIT_LATCH.await();
    }
  }

  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(StellarInboundApplication.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    app.addListeners(new ApplicationPidFileWriter());
    app.run(args);
  }

  public static void shutdown() {
    if (EXIT_LATCH != null) {
      EXIT_LATCH.countDown();
    }
  }
}
