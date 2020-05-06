package info.schnatterer.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class Main {

    public static void main(String[] args) throws Exception {

        Tomcat tomcat = new Tomcat();
        // Without this call the connector seems not to start
        tomcat.getConnector();

        Context ctx = tomcat.addContext("", new File(".").getAbsolutePath());

        Tomcat.addServlet(ctx, "Servlet", new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp)throws IOException {
                Writer w = resp.getWriter();
                w.write("Hello Embedded Tomcat.\n");
                w.flush();
                w.close();
            }
        });

        ctx.addServletMappingDecoded("/*", "Servlet");

        Connector connector = new Connector("info.schnatterer.tomcat.ReloadingHttp11AprProtocol");
        tomcat.getServer().addLifecycleListener(new AprLifecycleListener());
        connector.setPort(8443);
        connector.setSecure(true);
        connector.setScheme("https");
        connector.setAttribute("SSLEnabled", "true");
        
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

        // add connector to tomcat
        tomcat.getService().addConnector(connector);

        tomcat.start();
        tomcat.getServer().await();
    }
}
