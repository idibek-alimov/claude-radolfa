package tj.radolfa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Radolfa – Spring Boot entry point.
 *
 * Component scanning starts from {@code tj.radolfa} and covers
 * domain, application, and infrastructure sub-packages automatically.
 */
@SpringBootApplication
@EnableScheduling
public class RadolfaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RadolfaApplication.class, args);
    }
}
