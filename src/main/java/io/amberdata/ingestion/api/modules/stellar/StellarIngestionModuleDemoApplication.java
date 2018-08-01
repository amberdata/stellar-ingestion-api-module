package io.amberdata.ingestion.api.modules.stellar;

import java.util.concurrent.CountDownLatch;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = {GsonAutoConfiguration.class})
public class StellarIngestionModuleDemoApplication implements CommandLineRunner {

    private static CountDownLatch exitLatch;

    public static void main (String[] args) {
        SpringApplication app = new SpringApplication(StellarIngestionModuleDemoApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.addListeners(new ApplicationPidFileWriter());
        app.run(args);
    }

    public static void shutdown () {
        exitLatch.countDown();
    }

    @Override
    public void run (String... args) throws Exception {
        exitLatch = new CountDownLatch(1);
        exitLatch.await();
    }
}
