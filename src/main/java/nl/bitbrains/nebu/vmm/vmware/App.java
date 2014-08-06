package nl.bitbrains.nebu.vmm.vmware;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.ws.rs.core.Application;

import nl.bitbrains.nebu.common.config.AuthenticationConfiguration;
import nl.bitbrains.nebu.common.config.ClientConfiguration;
import nl.bitbrains.nebu.common.config.Configuration;
import nl.bitbrains.nebu.common.config.InvalidConfigurationException;
import nl.bitbrains.nebu.vmm.vmware.api.DefaultVMware;
import nl.bitbrains.nebu.vmm.vmware.api.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Setting up the server side of the application.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class App extends Application {

    private static Logger logger = LogManager.getRootLogger();

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this
     * application.
     * 
     * @param port
     *            The port on which to put this server.
     * @return Grizzly HTTP server.
     */
    private static HttpServer startServer(final int port) {
        // create a resource config that scans for JAX-RS resources and
        // providers
        // in com.example package
        final ResourceConfig rc = new ResourceConfig().packages("nl.bitbrains.nebu.vmm.vmware");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create("http://0.0.0.0:"
                                                                 + Integer.toString(port)),
                                                         rc);
    }

    /**
     * Main method.
     * 
     * @param args
     *            Needs one Argument. Path to config file.
     * @throws IOException
     *             When the config file cannot be accessed.
     * @throws InvalidConfigurationException
     *             if invalid config is encountered.
     */
    @SuppressWarnings("deprecation")
    public static void main(final String[] args) throws IOException, InvalidConfigurationException {
        App.logger.info("Nebu VMM VMware Extension starting.");

        // Parsing configuration file.
        final Configuration config = Configuration.parseConfigurationFile(new File(args[0]));
        final ClientConfiguration vcloudConfig = config.getClientConfig("vcloud");
        final AuthenticationConfiguration vcloudAuth = vcloudConfig.getAuthenticationConfig();
        final ClientConfiguration vsphereConfig = config.getClientConfig("vsphere");
        final AuthenticationConfiguration vsphereAuth = vsphereConfig.getAuthenticationConfig();

        final DefaultVMware dvmware = new DefaultVMware();
        dvmware.init(vcloudAuth.getUsername(),
                     vcloudAuth.getPassword(),
                     vcloudConfig.getIpAddress(),
                     vsphereAuth.getUsername(),
                     vsphereAuth.getPassword(),
                     vsphereConfig.getIpAddress(),
                     vsphereConfig.getPort());
        Singleton.setVmware(dvmware);

        // Starting REST server.
        final HttpServer server = App.startServer(config.getServerConfig().getPort());
        System.in.read();
        server.stop();

        App.logger.info("Nebu VMM VMware Extension stopping.");
    }
}
