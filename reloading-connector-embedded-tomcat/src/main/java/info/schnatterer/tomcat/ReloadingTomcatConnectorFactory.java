package info.schnatterer.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11AprProtocol;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;

import java.io.File;

/**
 * Provides methods to set up an embedded tomcat to use HTTPs and to be able to reload its SSL Context when the 
 * certificate file changes. 
 */
public final class ReloadingTomcatConnectorFactory {

    private ReloadingTomcatConnectorFactory() {}
    
    /**
     * Creates and sets up a Tomcat Https Connector that reloads its SSL Context when the certificate file changes.
     * Als adds an {@link AprLifecycleListener} required for the Connector's protocol to work.
     * 
     * @param tomcat instance to add the connector to
     * @param certificateKeyFile path to the certificate key file
     * @param certificateFile path to the certificate file
     * @param certificateChainFile path to the certificate chain file
     * @return the instance of the added connector for further customization
     */
    public static Connector addHttpsConnector(Tomcat tomcat, int httpsPort, String certificateKeyFile, 
                                              String certificateFile, String certificateChainFile) {
        
        Connector httpsConnector = new Connector("info.schnatterer.tomcat.ReloadingHttp11AprProtocol");

        httpsConnector.setPort(httpsPort);
        httpsConnector.setScheme("https");
        httpsConnector.setSecure(true);
        httpsConnector.setProperty("SSLEnabled", "true");

        Http11AprProtocol protocol = (Http11AprProtocol) httpsConnector.getProtocolHandler();
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
        httpsConnector.addSslHostConfig(sslHostConfig);

        httpsConnector.addUpgradeProtocol(new Http2Protocol());

        tomcat.getServer().addLifecycleListener(new AprLifecycleListener());
        
        tomcat.getService().addConnector(httpsConnector);
        
        return httpsConnector;
    }
}
