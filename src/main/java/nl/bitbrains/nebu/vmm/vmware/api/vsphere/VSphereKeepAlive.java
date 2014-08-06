package nl.bitbrains.nebu.vmm.vmware.api.vsphere;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vmware.vim25.mo.ServiceInstance;

/**
 * Simple thread that polls the vSphere API every 20 minutes to keep the
 * connection open.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VSphereKeepAlive implements Runnable {

    /**
     * The {@link Logger} for this object.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * The time to sleep between polls.
     */
    public static final long SLEEP_TIME_MILLIS = 20 * 60 * 1000;

    /**
     * Value used to stop thread when requested.
     */
    private boolean keeprunning = true;

    /**
     * The vSphere object whose connection to keep alive.
     */
    private final VSphere vsphere;

    /**
     * The vSphere username.
     */
    private final String username;

    /**
     * The vSphere password.
     */
    private final String password;

    /**
     * The URL of the vSphere API.
     */
    private final String url;

    /**
     * The port of the vSphere API.
     */
    private final int port;

    /**
     * Creates a new {@link VSphereKeepAlive} that polls the given
     * {@link ServiceInstance}.
     * 
     * @param vSphere
     *            The {@link VSphere} object whose connection to keep alive.
     * @param username
     *            The username to use.
     * @param password
     *            The password to use.
     * @param url
     *            The URL to connect to.
     * @param port
     *            The port to connect to.
     */
    public VSphereKeepAlive(final VSphere vSphere, final String username, final String password,
            final String url, final int port) {
        this.vsphere = vSphere;
        this.username = username;
        this.password = password;
        this.url = url;
        this.port = port;
    }

    @Override
    public void run() {
        while (this.keeprunning) {
            try {
                Thread.sleep(VSphereKeepAlive.SLEEP_TIME_MILLIS);
            } catch (final InterruptedException e1) {
                this.stop();
            }
            try {
                this.vsphere.getServiceInstance().currentTime();
            } catch (final RemoteException e) {
                try {
                    this.vsphere.init(this.username, this.password, this.url, this.port);
                } catch (RemoteException | MalformedURLException e1) {
                    VSphereKeepAlive.logger.error("Could not login to vSphere at address {}:{}.",
                                                  this.url,
                                                  this.port);
                    VSphereKeepAlive.logger.catching(e1);
                    this.stop();
                }
            }
        }
    }

    /**
     * Stops the thread.
     */
    public void stop() {
        VSphereKeepAlive.logger.warn("Stopping vSphere KeepAlive.");
        this.keeprunning = false;
    }

}
