package nl.bitbrains.nebu.vmm.vmware.exception;

/**
 * Thrown when no physical topology can be retrieved from VMware.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class NoPhysicalTopologyException extends Exception {

    /**
     * Version ID.
     */
    private static final long serialVersionUID = 7673227381926508069L;

    /**
     * Creates a new exception.
     * 
     * @param message
     *            The exception message.
     */
    public NoPhysicalTopologyException(final String message) {
        super(message);
    }
}
