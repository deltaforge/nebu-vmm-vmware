package nl.bitbrains.nebu.vmm.vmware.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.interfaces.Identifiable;

/**
 * Represents the status of a new virtual machine that is being deployed and
 * booted.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VmBootStatus implements Identifiable {

    /**
     * Map that stores {@link VmBootStatus} tasks.
     */
    private static Map<String, VmBootStatus> tasks = new HashMap<String, VmBootStatus>();

    /**
     * The status of the task that deploys and boots the new virtual machine.
     * 
     * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
     * 
     */
    public enum Status {
        /**
         * Task has just been created.
         */
        CREATED,

        /**
         * Task is waiting to be executed.
         */
        WAITING,

        /**
         * Task is being processed.
         */
        PROCESSESING,

        /**
         * Task has completed successfully.
         */
        SUCCESS,

        /**
         * Task has failed.
         */
        FAILURE
    }

    /**
     * Task ID.
     */
    private final String id;

    /**
     * New virtual machine ID.
     */
    private String vmid;

    /**
     * Task status.
     */
    private Status status;

    /**
     * Creates a new {@link VmBootStatus}.
     */
    public VmBootStatus() {
        this.id = UUID.randomUUID().toString();
        this.status = Status.CREATED;
        this.vmid = "";
    }

    @Override
    public String getUniqueIdentifier() {
        return this.id;
    }

    /**
     * 
     * @return The ID of the {@link VirtualMachine}.
     */
    public String getVmId() {
        return this.vmid;
    }

    /**
     * Set the ID of the {@link VirtualMachine}.
     * 
     * @param vmid
     *            The ID.
     */
    public void setVmId(final String vmid) {
        this.vmid = vmid;
    }

    /**
     * 
     * @return The status of this {@link VmBootStatus}.
     */
    public Status getStatus() {
        return this.status;
    }

    /**
     * The status of this {@link VmBootStatus}.
     * 
     * @param status
     *            The {@link VmBootStatus}.
     */
    public void setStatus(final Status status) {
        this.status = status;
    }

    /**
     * Gets a {@link VmBootStatus} object.
     * 
     * @param id
     *            The id of the {@link VmBootStatus}.
     * @return The {@link VmBootStatus} or null if a matching status does not
     *         exist.
     */
    public static VmBootStatus getStatus(final String id) {
        return VmBootStatus.tasks.get(id);
    }

    /**
     * @param status
     *            The {@link VmBootStatus} to add.
     */
    public static void addStatus(final VmBootStatus status) {
        VmBootStatus.tasks.put(status.getUniqueIdentifier(), status);
    }

    /**
     * Removes all {@link VmBootStatus} objects from management. They can no
     * longer be retrieved via the REST API.
     */
    public static void clearStatusList() {
        VmBootStatus.tasks.clear();
    }

}
