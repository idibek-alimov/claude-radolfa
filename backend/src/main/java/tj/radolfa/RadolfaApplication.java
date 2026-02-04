package tj.radolfa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Radolfa – Spring Boot entry point.
 *
 * Component scanning starts from {@code tj.radolfa} and covers
 * domain, application, and infrastructure sub-packages automatically.
 */
@SpringBootApplication
public class RadolfaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RadolfaApplication.class, args);
    }
}
