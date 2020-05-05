package info.schnatterer.tomcat;

import org.apache.coyote.http11.Http11AprProtocol;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;

public class ReloadingHttp11AprProtocol extends Http11AprProtocol {

    private final Object lock = new Object();
    private boolean configChanged = false;

    @Override
    public void init() throws Exception {
        super.init();
        new WatchSslConfigThread().start();
        new ReloadSslConfigThread().start();
    }

    class WatchSslConfigThread extends Thread {

        private final Log log = LogFactory.getLog(WatchSslConfigThread.class);

        @Override
        public void run() {

            SSLHostConfig[] sslHostConfigs = getEndpoint().findSslHostConfigs();
            if (sslHostConfigs.length < 1) {
                log.error("No SSLHostConfig found. Can't watch for changes");
                return;
            }

            // TODO this is only the simplest of all cases
            // Presume there is only one host config. Reload only if something in the folder of the certificate changes
            Set<SSLHostConfigCertificate> certificates = sslHostConfigs[0].getCertificates();
            if (certificates.isEmpty()) {
                log.error("No Certificate found in SSLHostConfig found. Can't watch for changes");
                return;
            }

            String certificateFile = certificates.iterator().next().getCertificateFile();
            if (certificateFile == null) {
                log.error("Certificate file is null.Can't watch for changes");
                return;
            }

            Path path = Paths.get(certificateFile).getParent();
            log.info("Watching certificate folder for change: " + path);

            WatchService watchService;
            try {
                watchService = FileSystems.getDefault().newWatchService();

                path.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);

                watchAndReload(watchService);
            } catch (IOException e) {
                log.error("Error while setting up watch for certificate folder: " + path.toAbsolutePath().toString(), e);
                throw new WatchSslConfigException(e);
            }
        }

        @SuppressWarnings("InfiniteRecursion") // We want this thread to last as long as tomcat runs
        private void watchAndReload(WatchService watchService) {
            WatchKey key;
            try {
                while ((key = watchService.take()) != null) {
                    // The events must be polled to not run into an endless take() loop
                    for (WatchEvent<?> event : key.pollEvents()) {
                        log.debug(event.kind() + " - file: " + event.context());
                    }
                    // If reloadSslHostConfigs() would be processed here, there would be exceptions as all cert files
                    // have to be replaced before the ssl host config is valid again
                    synchronized (lock) {
                        configChanged = true;
                        lock.notifyAll();
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                log.error("Error while watching certificate folder. Retrying.", e);
                interrupt();
            } finally {
                watchAndReload(watchService);
            }
        }
    }

    class ReloadSslConfigThread extends Thread {
        private final Log log = LogFactory.getLog(ReloadSslConfigThread.class);

        @Override
        @SuppressWarnings("InfiniteRecursion") // We want this thread to last as long as tomcat runs
        public void run() {
            try {
                log.debug("Listening for certificate changes");
                while (!configChanged) {
                    synchronized (lock) {
                        lock.wait();
                    }
                }

                log.debug("Received notification of changed certificate. Delaying reload to make sure all cert files are written.");
                Thread.sleep(3000);

                getEndpoint().reloadSslHostConfigs();
                log.info("Reloaded SSL Config");
                configChanged = false;
            } catch (Exception e) {
                log.error("Error while waiting for certificate to change. Retrying.", e);
            } finally {
                run();
            }
        }
    }
    
    static class WatchSslConfigException extends RuntimeException {
        WatchSslConfigException(Exception e) {
            super(e);
        }
    }
}