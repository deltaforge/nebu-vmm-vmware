package nl.bitbrains.nebu.vmm.vmware.entity;

import nl.bitbrains.nebu.common.interfaces.Identifiable;

/**
 * This class represents a vapp in the VMWare infrastructure.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 *         FIXME: Add more content.
 */
public class VirtualApplication implements Identifiable {

    /**
     * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
     * 
     */
    public enum Status {
        /**
         * {@link VirtualApplication} is powered on.
         */
        ON,

        /**
         * {@link VirtualApplication} is powered off.
         */
        OFF,

        /**
         * State of the {@link VirtualApplication} is unknown.
         */
        UNKNOWN
    }

    /**
     * UUID of this object.
     */
    private final String uuid;

    /**
     * Status of the this {@link VirtualApplication}.
     */
    private Status status;

    /**
     * The name of this {@link VirtualApplication}.
     */
    private String name;

    /**
     * Simple constructor.
     * 
     * @param uuid
     *            to set.
     */
    public VirtualApplication(final String uuid) {
        this.uuid = uuid;
        this.status = Status.UNKNOWN;
    }

    @Override
    public String getUniqueIdentifier() {
        return this.uuid;
    }

    /**
     * @param status
     *            to set.
     */
    public void setStatus(final Status status) {
        this.status = status;
    }

    /**
     * @return to get the status;
     */
    public Status getStatus() {
        return this.status;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

}
