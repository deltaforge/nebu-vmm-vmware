package nl.bitbrains.nebu.vmm.vmware.api;

import java.util.List;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.vmm.vmware.entity.VirtualApplication;
import nl.bitbrains.nebu.vmm.vmware.entity.VmBootStatus;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMLaunchException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;

/**
 * General interface through which Nebu communicates with several VMware APIs.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public interface VMware {

    // Physical topology information

    /**
     * Retrieves the {@link PhysicalTopology} that is part of the given resource
     * pool.
     * 
     * @param poolname
     *            The name of the resource pool.
     * @return A subset of the {@link PhysicalTopology}.
     * @throws VMwareException
     *             When there is an internal error in the VMware API.
     */
    PhysicalTopology getPhysicalTopologyFromResourcePool(String poolname) throws VMwareException;

    /**
     * Gets the {@link PhysicalStore} that matches the given id.
     * 
     * @param uuid
     *            {@link PhysicalStore} identifier.
     * @return matching {@link PhysicalStore}.
     * @throws VMwareException
     *             If the object cannot be found.
     */
    PhysicalStore getStoreInfo(String uuid) throws VMwareException;

    /**
     * Gets the {@link PhysicalHost} that matches the given id.
     * 
     * @param uuid
     *            {@link PhysicalHost} identifier.
     * @return matching {@link PhysicalHost}.
     * @throws VMwareException
     *             If the object cannot be found.
     */
    PhysicalHost getHostInfo(String uuid) throws VMwareException;

    // Virtual topology information

    /**
     * Returns a list of all {@link VirtualApplication}s visible to the user.
     * 
     * @return A list of {@link VirtualApplication}s
     * @throws VMwareException
     *             When VMware objects cannot be found by their references.
     */
    List<VirtualApplication> getAllVapps() throws VMwareException;

    /**
     * @return A list of {@link VirtualMachine} uuids.
     * @throws VMwareException
     *             When things go seriously wrong with VMware.
     */
    List<String> getVirtualResourceList() throws VMwareException;

    /**
     * Extracts a list of {@link VirtualMachine} IDs from a list of
     * {@link VirtualMachine} names.
     * 
     * @param vmIds
     *            The list of all {@link VirtualMachine} IDs.
     * @param vmNames
     *            The list of {@link VirtualMachine} names from which the IDs
     *            should be found.
     * @return A list containing the IDs of all {@link VirtualMachine}s in the
     *         names list.
     * @throws NoSuchVMException
     *             When one of the {@link VirtualMachine}s from the names list
     *             does not have an ID that is present in the ID list.
     * @throws VMwareException
     *             When stuff goes wrong with vCloud.
     */
    List<String> getVmIdsFromNames(final List<String> vmIds, final List<String> vmNames)
            throws NoSuchVMException, VMwareException;

    /**
     * Extracts the ID of a {@link VirtualMachine} from a list of
     * {@link VirtualMachine} IDs.
     * 
     * @param vmIds
     *            The list of all {@link VirtualMachine} IDs.
     * @param vmName
     *            The name of the {@link VirtualMachine} whose ID should be
     *            found.
     * @return The ID of the {@link VirtualMachine} whose name was given.
     * @throws NoSuchVMException
     *             When the ID of the VM that corresponds with the given name
     *             could not be found.
     * @throws VMwareException
     *             When stuff goes wrong with the VMware API.
     */
    String getVmIdFromName(final List<String> vmIds, final String vmName) throws NoSuchVMException,
            VMwareException;

    /**
     * Finds the {@link VirtualMachine} that corresponds with the given UUID
     * {@link String}.
     * 
     * @param uuid
     *            The UUID of the {@link VirtualMachine} that should be
     *            retrieved.
     * @return A {@link VirtualMachine} whose UUID matches the given uuid.
     * @throws NoSuchVMException
     *             if the given UUID does not correspond with a known
     *             {@link VirtualMachine}.
     * @throws VMwareException
     *             When things go seriously wrong with VMware.
     */
    VirtualMachine getVirtualMachineInfo(final String uuid) throws VMwareException,
            NoSuchVMException;

    // Combined information

    /**
     * Retrieves the subset of the {@link PhysicalTopology} that is accessible
     * for at least one of the given {@link VirtualApplication}s.
     * 
     * @param vAppIds
     *            The IDs of the {@link VirtualApplication}s.
     * @return A subset of the {@link PhysicalTopology}.
     * @throws VMwareException
     *             When an error occurs during information retrieval.
     */
    PhysicalTopology getPhysicalTopologyForVapps(final List<String> vAppIds) throws VMwareException;

    /**
     * Finds a {@link VirtualApplication} which is (partially) hosted by the
     * given {@link PhysicalHost}. The {@link VirtualApplication} should be
     * present in the given list.
     * 
     * @param host
     *            The {@link PhysicalHost} from which to select
     *            {@link VirtualApplication}s.
     * @param possibleVapps
     *            A list to filter the potential answers.
     * @return A {@link VirtualApplication} from the given {@link PhysicalHost}.
     *         Its id matches one of the strings in the given list.
     */
    VirtualApplication selectVirtualApplicationFromHost(final PhysicalHost host,
            final List<String> possibleVapps);

    // Deploying

    /**
     * Deploys and launches a new virtual machine.
     * 
     * @param vm
     *            The {@link VirtualMachine} that should be copied.
     * @param dest
     *            The destination {@link VirtualApplication}.
     * @param hostId
     *            The ID of the physical host where the vm should be started.
     * @param hostname
     *            The hostname of the new virtual machine.
     * @return A {@link VmBootStatus} for the new virtual machine.
     * @throws VMLaunchException
     *             When an error occurs during creation or launch.
     */
    VmBootStatus createVM(VirtualMachine vm, VirtualApplication dest, String hostId, String hostname)
            throws VMLaunchException;

    /**
     * Deploys and launches a new virtual machine.
     * 
     * @param vm
     *            The {@link VirtualMachine} that should be copied.
     * @param dest
     *            The destination {@link VirtualApplication}.
     * @param hostId
     *            The ID of the physical host where the vm should be started.
     * @param storageUnitId
     *            The ID of the storage unit the vm should use.
     * @param hostname
     *            The hostname of the new virtual machine.
     * @return A {@link VmBootStatus} for the new virtual machine.
     * @throws VMLaunchException
     *             When an error occurs during creation or launch.
     */
    VmBootStatus createVM(VirtualMachine vm, VirtualApplication dest, String hostId,
            String hostname, String storageUnitId) throws VMLaunchException;

    /**
     * Turns off and deletes a VM.
     * 
     * @param vm
     *            The {@link VirtualMachine} object that represents the VM to
     *            delete.
     */
    void killVM(VirtualMachine vm);

    /**
     * Moves an existing VM to a {@link PhysicalHost}.
     * 
     * @param vm
     *            The VM to move.
     * @param host
     *            The host to move to.
     * @throws VMwareException
     *             When an error occurs during VM migration.
     */
    void moveVMToHost(VirtualMachine vm, PhysicalHost host) throws VMwareException;

    /**
     * Moves an existing VM to a new {@link PhysicalStore}.
     * 
     * @param vm
     *            The VM to move.
     * @param store
     *            The store to move to.
     * @throws VMwareException
     *             When an error occurs during VM migration.
     */
    void moveVMToStore(VirtualMachine vm, PhysicalStore store) throws VMwareException;

}
