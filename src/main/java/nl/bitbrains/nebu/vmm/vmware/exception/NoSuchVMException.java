package nl.bitbrains.nebu.vmm.vmware.exception;

/**
 * Thrown when a certain VM cannot be found in VMware.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class NoSuchVMException extends Exception {

    /**
     * version ID.
     */
    private static final long serialVersionUID = -2078616018686193395L;

    /**
     * Creates a new Exception.
     * 
     * @param message
     *            The exception message.
     */
    public NoSuchVMException(final String message) {
        super(message);
    }
}
