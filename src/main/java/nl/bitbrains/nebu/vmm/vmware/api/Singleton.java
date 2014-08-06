package nl.bitbrains.nebu.vmm.vmware.api;

/**
 * Class to keep singleton objects that need to be accessible from everywhere in
 * the program.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public final class Singleton {

    /**
     * Singleton {@link VMware} object.
     */
    private static VMware vmware;

    /**
     * Private constructor. This class only has static methods.
     */
    private Singleton() {
    }

    /**
     * @return the vmware
     */
    public static VMware getVmware() {
        return Singleton.vmware;
    }

    /**
     * @param vmware
     *            the vmware to set
     */
    public static void setVmware(final VMware vmware) {
        Singleton.vmware = vmware;
    }
}
