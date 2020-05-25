package info.schnatterer.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11AprProtocol;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class ReloadingTomcatServletWebServerFactory extends TomcatServletWebServerFactory {

    @Value("${server.ssl.certificateFile}")
    private String certificateFile;
    @Value("${server.ssl.certificateKeyFile}")
    private String certificateKeyFile;
    @Value("${server.ssl.certificateChainFile}")
    private String certificateChainFile;
    @Value("${server.http.port:0}")
    private int httpPort;
    @Value("${server.port}")
    private int serverPort;

    @Override
    public Ssl getSsl() {
        // Disable JSSE Protocol for SSL
        return null;
    }

    public ReloadingTomcatServletWebServerFactory() {
        setProtocol("info.schnatterer.tomcat.ReloadingHttp11AprProtocol");
        addConnectorCustomizers(setupReloadingConnector());
    }

    private TomcatConnectorCustomizer setupReloadingConnector() {
        return connector -> {
            connector.setScheme("https");
            connector.setSecure(true);

            Http11AprProtocol protocol = (Http11AprProtocol) connector.getProtocolHandler();
            protocol.setSSLEnabled(true);
            // See for more options:
            // https://github.com/OryxProject/oryx/blob/dd938e17d4c872f91f1452a4d05a1d8e5eb33daf/framework/oryx-lambda-serving/src/main/java/com/cloudera/oryx/lambda/serving/ServingLayer.java#L215
            SSLHostConfig sslHostConfig = new SSLHostConfig();
            SSLHostConfigCertificate cert =
                    new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.RSA);
            cert.setCertificateKeyFile(new File(certificateKeyFile).getAbsolutePath());
            cert.setCertificateFile(new File(certificateFile).getAbsolutePath());
            cert.setCertificateChainFile(new File(certificateChainFile).getAbsolutePath());
            sslHostConfig.addCertificate(cert);
            connector.addSslHostConfig(sslHostConfig);

            connector.addUpgradeProtocol(new Http2Protocol());

            if (httpPort > 0) {
                addAdditionalTomcatConnectors(createHttpConnector(httpPort));
            }
        };
    }

    private Connector createHttpConnector(int httpPort) {
        Connector httpConnector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        httpConnector.setScheme("http");
        httpConnector.setPort(httpPort);
        httpConnector.setSecure(false);
        httpConnector.setRedirectPort(serverPort);
        return httpConnector;
    }
}
