package nl.bitbrains.nebu.vmm.vmware.api.vcloud;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vmware.vcloud.sdk.VCloudRuntimeException;

/**
 * Class to keep the vCloud connection alive over longer periods of time.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VCloudKeepAlive implements Runnable {

    /**
     * The {@link Logger} for this object.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * Time to sleep before refreshing connection.
     */
    public static final long SLEEP_TIME_MILLIS = 20 * 60 * 1000;

    /**
     * Specifies if this thread should keep running.
     */
    private boolean keepRunning = true;

    /**
     * The {@link VCloud} instance whose connection to keep alive.
     */
    private final VCloud vcloud;

    /**
     * The vCloud username.
     */
    private final String username;

    /**
     * The vCloud password.
     */
    private final String password;

    /**
     * The vCloud URL API.
     */
    private final String url;

    /**
     * Creates a new {@link VCloudKeepAlive} object to keep alive the given
     * client.
     * 
     * @param vcloud
     *            The {@link VCloud} whose connection to keep alive.
     * @param password
     *            The vCloud username.
     * @param username
     *            The vCloud password.
     * @param url
     *            The vCloud REST API URL.
     */
    public VCloudKeepAlive(final VCloud vcloud, final String username, final String password,
            final String url) {
        this.vcloud = vcloud;

        this.username = username;
        this.password = password;
        this.url = url;
    }

    @Override
    public void run() {
        while (this.keepRunning) {
            try {
                Thread.sleep(VCloudKeepAlive.SLEEP_TIME_MILLIS);
            } catch (final InterruptedException e) {
                this.stop();
            }
            boolean extend = false;
            try {
                extend = this.vcloud.getVcc().extendSession();
            } catch (final VCloudRuntimeException e) {
                VCloudKeepAlive.logger
                        .warn("Could not renew vCloud session. Will try to reset session.");
            }
            if (!extend) {
                final boolean initResult = this.vcloud.init(this.username, this.password, this.url);
                if (!initResult) {
                    VCloudKeepAlive.logger.error("Could not login to vCloud at address {}.",
                                                 this.url);
                    this.stop();
                }
            }
        }
    }

    /**
     * Stops the thread gracefully.
     */
    public void stop() {
        VCloudKeepAlive.logger.warn("Stopping vCloud KeepAlive.");
        this.keepRunning = false;
    }

}
