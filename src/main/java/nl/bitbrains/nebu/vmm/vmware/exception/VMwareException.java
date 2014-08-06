package nl.bitbrains.nebu.vmm.vmware.exception;

/**
 * Generic error to throw when an error occurs in one of the used VMware APIs.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VMwareException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -6898050542016462495L;

    /**
     * Creates a new {@link VMwareException} with a given message.
     * 
     * @param message
     *            The message to pass with this {@link Exception}.
     */
    public VMwareException(final String message) {
        super(message);
    }

    /**
     * Creates a new VMwareException that is based on the given throwable.
     * 
     * @param message
     *            The exception message.
     * @param t
     *            A {@link Throwable}.
     */
    public VMwareException(final String message, final Throwable t) {
        super(message, t);
    }

}
