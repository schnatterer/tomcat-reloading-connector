package info.schnatterer.springboot;

import info.schnatterer.tomcat.ReloadingTomcatServletWebServerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@Import(ReloadingTomcatServletWebServerFactory.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

    @RestController
    public class HelloController {

        @GetMapping("/")
        public String index() {
            return "Hello Spring Boot.\n";
        }
    }
}
