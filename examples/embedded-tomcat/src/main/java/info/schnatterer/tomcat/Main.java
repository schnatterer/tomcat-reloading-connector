package info.schnatterer.tomcat;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class Main {

    private static final int HTTPS_PORT = 8443;
    public static final String PK = "certs/pk.pem";
    public static final String CRT = "certs/crt.pem";
    public static final String CA = "certs/ca.crt.pem";

    public static void main(String[] args) throws Exception {

        Tomcat tomcat = new Tomcat();
        // Without this call the connector seems not to start
        tomcat.getConnector();

        Context ctx = tomcat.addContext("", new File(".").getAbsolutePath());

        Tomcat.addServlet(ctx, "Servlet", new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                Writer w = resp.getWriter();
                w.write("Hello Embedded Tomcat.\n");
                w.flush();
                w.close();
            }
        });

        ctx.addServletMappingDecoded("/*", "Servlet");

        ReloadingTomcatConnectorFactory.addHttpsConnector(tomcat, HTTPS_PORT, PK, CRT, CA);

        tomcat.start();
        tomcat.getServer().await();
    }
}
