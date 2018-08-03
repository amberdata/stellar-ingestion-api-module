package io.amberdata.ingestion.api.modules.stellar;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.core.env.Environment;

@SpringBootApplication(exclude = {GsonAutoConfiguration.class})
public class StellarIngestionModuleDemoApplication implements CommandLineRunner {

    private static CountDownLatch exitLatch;

    private final Environment environment;

    @Autowired
    public StellarIngestionModuleDemoApplication (Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run (String... args) throws Exception {
        boolean testProfileDisabled = Arrays.stream(environment.getActiveProfiles())
            .noneMatch(profile -> profile.equalsIgnoreCase("test"));

        if (testProfileDisabled) {
            exitLatch = new CountDownLatch(1);
            exitLatch.await();
        }
    }

    public static void main (String[] args) {
        SpringApplication app = new SpringApplication(StellarIngestionModuleDemoApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.addListeners(new ApplicationPidFileWriter());
        app.run(args);
    }

    public static void shutdown () {
        if (exitLatch != null) {
            exitLatch.countDown();
        }
    }
}
