package nl.bitbrains.nebu.vmm.vmware.exception;

/**
 * Thrown when an error occurs during the launch of a new virtual machine.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VMLaunchException extends Exception {

    /**
     * Version ID.
     */
    private static final long serialVersionUID = -7136349856315390982L;

    /**
     * Creates a new exception.
     * 
     * @param message
     *            The exception message.
     */
    public VMLaunchException(final String message) {
        super(message);
    }

    /**
     * Creates a new exception.
     * 
     * @param message
     *            The exception message.
     * @param t
     *            The {@link Throwable} that caused this exception to be thrown.
     */
    public VMLaunchException(final String message, final Throwable t) {
        super(message, t);
    }
}
