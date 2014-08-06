package nl.bitbrains.nebu.vmm.vmware.provider;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.factories.StringFactory;
import nl.bitbrains.nebu.common.factories.VirtualMachineFactory;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.vmm.vmware.api.Singleton;
import nl.bitbrains.nebu.vmm.vmware.api.VMware;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;

/**
 * 
 * Provides part of the REST API to the Nebu core. Is responsible for supplying
 * information about the virtual resources in VMware to Nebu.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 */
@Path(VirtualResourceProvider.PATH)
public class VirtualResourceProvider {

    /**
     * Logger of this object.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * Version ID.
     */
    public static final String PATH = "/virt";

    /**
     * Path where to find virtual resource information.
     */
    public static final String PATH_UUID = "{uuid}";

    /**
     * Path to kill existing VMs.
     */
    public static final String PATH_KILL = VirtualResourceProvider.PATH_UUID + "/assassinate";

    /**
     * Path to move existing VM to host.
     */
    public static final String PATH_MOVE = VirtualResourceProvider.PATH_UUID + "/move";

    /**
     * Param name of object UUID.
     */
    private static final String UUID_PARAM_NAME = "uuid";

    /**
     * Returns a list of all virtual machine uuids known to the virtual machine
     * manager.
     * 
     * @return A list of virtual machine uuids.
     * @throws JDOMException
     *             When things go wrong with JDOM parsing.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getVirtualResources() throws JDOMException {
        VirtualResourceProvider.logger.info("Retrieving virtual resources.");
        List<String> list = null;
        try {
            list = Singleton.getVmware().getVirtualResourceList();
        } catch (final VMwareException e) {
            VirtualResourceProvider.logger.catching(Level.ERROR, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        final Element xml = XMLConverter.convertCollectionToJDOMElement(list, new StringFactory());
        return Response.ok(XMLConverter.convertJDOMElementW3CDocument(xml)).build();
    }

    /**
     * Returns information about a virtual machine based on the given uuid.
     * 
     * @param uuid
     *            The uuid of the virtual machine whose information needs to be
     *            retrieved.
     * @return Information about a given virtual machine.
     * @throws NotFoundException
     *             When there is no VM with the given uuid.
     * @throws JDOMException
     *             When things go wrong with JDOM parsing.
     */
    @GET
    @Path(VirtualResourceProvider.PATH_UUID)
    @Produces(MediaType.APPLICATION_XML)
    public Response getVirtualMachineInfo(
            @PathParam(VirtualResourceProvider.UUID_PARAM_NAME) final String uuid)
            throws JDOMException {
        VirtualResourceProvider.logger.info("Retrieving information about VM with id {}.", uuid);
        VirtualMachine vm = null;
        try {
            vm = Singleton.getVmware().getVirtualMachineInfo(uuid);
        } catch (NoSuchVMException | VMwareException e) {
            VirtualResourceProvider.logger.catching(Level.ERROR, e);
            throw new NotFoundException(e.getMessage(), e);
        }

        final VirtualMachineFactory vmf = new VirtualMachineFactory();
        final Element xml = vmf.toXML(vm);

        VirtualResourceProvider.logger.info("Retrieved VM object with id {}.",
                                            vm.getUniqueIdentifier());

        return Response.ok(XMLConverter.convertJDOMElementW3CDocument(xml)).build();
    }

    /**
     * Kills an existing VM.
     * 
     * @param uuid
     *            The UUID of the VM to kill.
     * @return 500 iff the VM cannot be found. 200 Otherwise.
     */
    @DELETE
    @Path(VirtualResourceProvider.PATH_UUID)
    @Produces(MediaType.APPLICATION_XML)
    public Response deleteVirtualMachine(
            @PathParam(VirtualResourceProvider.UUID_PARAM_NAME) final String uuid) {
        final VMware vmware = Singleton.getVmware();
        VirtualMachine vm = null;
        try {
            vm = vmware.getVirtualMachineInfo(uuid);
        } catch (VMwareException | NoSuchVMException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        vmware.killVM(vm);
        return Response.ok().build();
    }

    /**
     * Kills an existing VM.
     * 
     * @param uuid
     *            The UUID of the VM to kill.
     * @return 500 iff the VM cannot be found. 200 Otherwise.
     */
    @GET
    @Path(VirtualResourceProvider.PATH_KILL)
    @Produces(MediaType.APPLICATION_XML)
    public Response deleteVirtualMachineByGet(
            @PathParam(VirtualResourceProvider.UUID_PARAM_NAME) final String uuid) {
        return this.deleteVirtualMachine(uuid);
    }

    /**
     * Moves an existing VM to a {@link PhysicalHost} or {@link PhysicalStore}.
     * 
     * @param uuid
     *            The vCloud UUID of the {@link VirtualMachine}. If this
     *            parameter is null VM host will not be changed.
     * @param hostid
     *            The vSphere ID of the {@link PhysicalHost}.
     * @param storeid
     *            The vSphere ID of the {@link PhysicalStore} to move to. If
     *            this parameter is null VM store will not be changed.
     * @return 200 on success. 500 when an error occurs. 400 when host ID is not
     *         given.
     */
    @GET
    @Path(VirtualResourceProvider.PATH_MOVE)
    @Produces(MediaType.APPLICATION_XML)
    public Response moveVirtualMachineByGet(
            @PathParam(VirtualResourceProvider.UUID_PARAM_NAME) final String uuid,
            @QueryParam("host") final String hostid, @QueryParam("store") final String storeid) {
        if (hostid == null && storeid == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        final VMware vmware = Singleton.getVmware();
        VirtualMachine vm;
        try {
            vm = vmware.getVirtualMachineInfo(uuid);
        } catch (VMwareException | NoSuchVMException e1) {
            VirtualResourceProvider.logger.error("Could not get VM information from VM ID {}.",
                                                 uuid);
            VirtualResourceProvider.logger.catching(e1);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        if (hostid != null && !(hostid.equals("")) && !this.moveVMHost(vmware, vm, hostid)) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        if (storeid != null && !(storeid.equals("")) && !this.moveVMStore(vmware, vm, storeid)) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }

    /**
     * Migrates a VM to a new host.
     * 
     * @param vmware
     *            The {@link VMware} instance to use.
     * @param vm
     *            The {@link VirtualMachine} to migrate.
     * @param hostid
     *            The vSphere ID of the {@link PhysicalHost}.
     * @return <code>true</code> iff move is successful.
     */
    private boolean moveVMHost(final VMware vmware, final VirtualMachine vm, final String hostid) {
        PhysicalHost host;
        try {
            host = vmware.getHostInfo(hostid);
        } catch (final VMwareException e) {
            VirtualResourceProvider.logger.error("Could not get host information from host ID {}.",
                                                 hostid);
            VirtualResourceProvider.logger.catching(e);
            return false;
        }
        try {
            vmware.moveVMToHost(vm, host);
        } catch (final VMwareException e) {
            VirtualResourceProvider.logger.error("Could not move VM {} to host {}.",
                                                 vm.getUniqueIdentifier(),
                                                 hostid);
            VirtualResourceProvider.logger.catching(e);
            return false;
        }
        return true;
    }

    /**
     * Migrates a VM to a new store.
     * 
     * @param vmware
     *            The {@link VMware} instance to use.
     * @param vm
     *            The {@link VirtualMachine} to move.
     * @param storeid
     *            The vSphere ID of the {@link PhysicalStore}.
     * @return <code>true</code> iff move is successful.
     */
    private boolean moveVMStore(final VMware vmware, final VirtualMachine vm, final String storeid) {
        PhysicalStore store = null;
        try {
            store = vmware.getStoreInfo(storeid);
        } catch (final VMwareException e) {
            VirtualResourceProvider.logger.error("Could not get store information from store {}.",
                                                 storeid);
            VirtualResourceProvider.logger.catching(e);
            return false;
        }
        try {
            vmware.moveVMToStore(vm, store);
        } catch (final VMwareException e) {
            VirtualResourceProvider.logger.error("Could not move VM {} to store {}.",
                                                 vm.getUniqueIdentifier(),
                                                 storeid);
            VirtualResourceProvider.logger.catching(e);
            return false;
        }
        return true;
    }
}
