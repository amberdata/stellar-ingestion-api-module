package io.amberdata.inbound.stellar;

import io.amberdata.inbound.core.InboundCore;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

@SpringBootApplication(exclude = {GsonAutoConfiguration.class})
@ComponentScan(basePackageClasses = {InboundCore.class, StellarInboundApplication.class})
public class StellarInboundApplication implements CommandLineRunner {
  private static CountDownLatch EXIT_LATCH;

  private final Environment environment;

  /**
   * Default constructor.
   *
   * @param environment the environment for the application.
   */
  @Autowired
  public StellarInboundApplication(Environment environment) {
    this.environment = environment;
  }

  /**
   * Runs the applications.
   *
   * @param args the arguments to pass to the application.
   *
   * @throws Exception If an error occurred during this operation.
   */
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

  /**
   * Entry point for the application.
   *
   * @param args the arguments to pass to the application.
   */
  public static void main(String[] args) {
    SpringApplication app = new SpringApplication(StellarInboundApplication.class);
    app.setWebApplicationType(WebApplicationType.NONE);
    app.addListeners(new ApplicationPidFileWriter());
    app.run(args);
  }

  /**
   * Shuts down the application.
   */
  public static void shutdown() {
    if (EXIT_LATCH != null) {
      EXIT_LATCH.countDown();
    }
  }
}
