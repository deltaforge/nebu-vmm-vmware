package nl.bitbrains.nebu.vmm.vmware.converter;

import java.rmi.RemoteException;

import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalHostBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.PhysicalStoreBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.HostSystem;

/**
 * Converts VMware objects that represent physical hardware to Nebu objects.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public final class PhysicalResourceConverter {

    /**
     * The logger for this class.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * Specifies one order of magnitude of bytes.
     */
    private static final double magnitude = 1024;

    /**
     * Private constructor. This class only has static methods.
     */
    private PhysicalResourceConverter() {
    }

    /**
     * Converts a {@link Datastore} to a {@link PhysicalStore}.
     * 
     * @param store
     *            The {@link Datastore} to translate.
     * @return A new {@link PhysicalStore}.
     */
    public static PhysicalStore toPhysicalStore(final Datastore store) {
        final String uuid = store.getMOR().getVal();
        final long capacity = store.getSummary().getCapacity();
        final long used = capacity - store.getSummary().getFreeSpace();
        final PhysicalStoreBuilder storeBuiler = new PhysicalStoreBuilder();
        return storeBuiler.withCapacity(capacity).withUsed(used).withUuid(uuid).build();
    }

    /**
     * Converts a {@link HostSystem} to a {@link PhysicalHost}. Includes local
     * disks.
     * 
     * @param system
     *            The {@link HostSystem} to translate.
     * @param includeDisks
     *            Specifies whether the {@link PhysicalHost} object should
     *            include local {@link PhysicalStore} objects.
     * @return A new {@link PhysicalHost}.
     */
    public static PhysicalHost toPhysicalHost(final HostSystem system, final boolean includeDisks) {
        final String uuid = system.getMOR().getVal();
        final PhysicalHostBuilder hostBuilder = new PhysicalHostBuilder();
        hostBuilder.withUuid(uuid);
        if (includeDisks) {
            Datastore[] stores = null;
            try {
                stores = system.getDatastores();
                for (final Datastore store : stores) {
                    if (!store.getSummary().getMultipleHostAccess()) {
                        hostBuilder.withDisk(PhysicalResourceConverter.toPhysicalStore(store));
                    }
                }
            } catch (final RemoteException e) {
                PhysicalResourceConverter.logger
                        .warn("Could not get data stores from host system.");
            }
        }
        PhysicalResourceConverter.addLoadInfo(system, hostBuilder);
        return hostBuilder.build();
    }

    /**
     * Adds system load information to the
     * {@link nl.bitbrains.nebu.common.topology.PhysicalHostBuilder}. This
     * includes cpu and memory usage.
     * 
     * @param system
     *            The {@link HostSystem} from which to get load information.
     * @param hostBuilder
     *            The builder to which to add load information.
     */
    private static void addLoadInfo(final HostSystem system, final PhysicalHostBuilder hostBuilder) {
        final int mhz = system.getSummary().getHardware().getCpuMhz()
                * system.getSummary().getHardware().getNumCpuCores();
        final int usedMhz = system.getSummary().getQuickStats().getOverallCpuUsage();
        final double cpuRatio = usedMhz / (double) mhz;

        final double mem = system.getSummary().getHardware().getMemorySize()
                / (PhysicalResourceConverter.magnitude * PhysicalResourceConverter.magnitude);
        final int usedMem = system.getSummary().getQuickStats().getOverallMemoryUsage();
        final double memRatio = usedMem / mem;

        hostBuilder.withCpuUsage(cpuRatio);
        hostBuilder.withMemUsage(memRatio);
    }
}
