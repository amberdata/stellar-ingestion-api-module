package io.amberdata.ingestion.stellar;

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

import io.amberdata.ingestion.core.IngestionCore;

@SpringBootApplication(exclude = {GsonAutoConfiguration.class})
@ComponentScan(basePackageClasses = {IngestionCore.class, StellarIngestionModuleDemoApplication.class})
public class StellarIngestionModuleDemoApplication implements CommandLineRunner {
    private static CountDownLatch EXIT_LATCH;

    private final Environment environment;

    @Autowired
    public StellarIngestionModuleDemoApplication (Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run (String... args) throws Exception {
        boolean testProfileDisabled = Arrays.stream(this.environment.getActiveProfiles())
            .noneMatch(profile -> profile.equalsIgnoreCase("test"));

        if (testProfileDisabled) {
            EXIT_LATCH = new CountDownLatch(1);
            EXIT_LATCH.await();
        }
    }

    public static void main (String[] args) {
        SpringApplication app = new SpringApplication(StellarIngestionModuleDemoApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.addListeners(new ApplicationPidFileWriter());
        app.run(args);
    }

    public static void shutdown () {
        if (EXIT_LATCH != null) {
            EXIT_LATCH.countDown();
        }
    }
}
