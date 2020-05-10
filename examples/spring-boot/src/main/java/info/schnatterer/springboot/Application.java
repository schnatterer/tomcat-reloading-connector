package info.schnatterer.springboot;

import org.apache.coyote.http11.Http11AprProtocol;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

import static java.util.Collections.singletonList;

@SpringBootApplication
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
    
    @Bean
    public TomcatServletWebServerFactory tomcatServletWebServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory() {
            @Override
            public Ssl getSsl() {
                // Need to do this in order to stop the default SSL customizer from running and imposing its
                // requirement on us using the JSSE Protocol for SSL
                return null;
            }
        };

        factory.setProtocol("info.schnatterer.tomcat.ReloadingHttp11AprProtocol");
        factory.setTomcatConnectorCustomizers(singletonList(connector -> {
            connector.setScheme("https");
            connector.setSecure(true);

            Http11AprProtocol protocol = (Http11AprProtocol) connector.getProtocolHandler();
            protocol.setSSLEnabled(true);

            // See for more options:
            // https://github.com/OryxProject/oryx/blob/dd938e17d4c872f91f1452a4d05a1d8e5eb33daf/framework/oryx-lambda-serving/src/main/java/com/cloudera/oryx/lambda/serving/ServingLayer.java#L215
            SSLHostConfig sslHostConfig = new SSLHostConfig();
            SSLHostConfigCertificate cert =
                    new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.RSA);
            cert.setCertificateKeyFile(new File("certs/pk.pem").getAbsolutePath());
            cert.setCertificateFile(new File("certs/crt.pem").getAbsolutePath());
            cert.setCertificateChainFile(new File("certs/ca.crt.pem").getAbsolutePath());
            sslHostConfig.addCertificate(cert);
            connector.addSslHostConfig(sslHostConfig);

            connector.addUpgradeProtocol(new Http2Protocol());
        }));

        return factory;
    }
}
