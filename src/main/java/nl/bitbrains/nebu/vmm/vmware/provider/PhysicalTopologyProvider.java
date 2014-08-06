package nl.bitbrains.nebu.vmm.vmware.provider;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalResource;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.factory.TopologyFactories;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.vmm.vmware.api.Singleton;
import nl.bitbrains.nebu.vmm.vmware.entity.VMTemplate;
import nl.bitbrains.nebu.vmm.vmware.entity.VirtualApplication;
import nl.bitbrains.nebu.vmm.vmware.entity.VmBootStatus;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMLaunchException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;

/**
 * Provides the /phys part of the VMM REST API.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
@Path(PhysicalTopologyProvider.PATH)
public class PhysicalTopologyProvider {

    /**
     * Logger for this class.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * URI of this provider.
     */
    public static final String PATH = "/phys";

    /**
     * The uuid part of the path.
     */
    public static final String PATH_UUID = "{uuid}";

    /**
     * The create VM path.
     */
    public static final String PATH_CREATEVM = PhysicalTopologyProvider.PATH_UUID + "/createVM";

    /**
     * Specifies a {@link PhysicalHost}.
     */
    public static final String HOST_INFO = "host";

    /**
     * Specifies a {@link PhysicalStore}.
     */
    public static final String STORE_INFO = "store";

    /**
     * Launches a new VM in vCloud.
     * 
     * @param uuid
     *            The vSphere HostSystem ID. Represent the physical host where
     *            the new VM should be placed.
     * @param hostname
     *            The hostname the new VM should have.
     * @param template
     *            The vmm template to use to launch this new VM.
     * @param store
     *            The vSphere id of the datastore the new VM should use.
     * @return HTML response code.
     * @throws URISyntaxException
     *             When the created URI cannot be composed.
     */
    @Path(PhysicalTopologyProvider.PATH_CREATEVM)
    @POST
    public Response createVM(@PathParam("uuid") final String uuid,
            @QueryParam("hostname") final String hostname,
            @QueryParam("template") final String template, @QueryParam("store") final String store)
            throws URISyntaxException {
        final VMTemplate vmtemplate = VMTemplateProvider.getCache().get(template);
        if (hostname == null || vmtemplate == null || vmtemplate.getTemplate() == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        final String hostnameNoSpace = hostname.replaceAll(" ", "-");
        String taskid = null;

        PhysicalTopologyProvider.logger.info("Going to launch new VM on host {} with template {}.",
                                             uuid,
                                             template);
        PhysicalTopologyProvider.logger.info("VM will have hostname {}", hostnameNoSpace);

        final String vmid = vmtemplate.getTemplate();
        try {
            final VirtualMachine vm = Singleton.getVmware().getVirtualMachineInfo(vmid);
            final PhysicalHost host = Singleton.getVmware().getHostInfo(uuid);
            final List<String> vappstrings = vmtemplate.getDestVApps();
            Collections.shuffle(vappstrings);
            final VirtualApplication dest = Singleton.getVmware()
                    .selectVirtualApplicationFromHost(host, vappstrings);
            VmBootStatus task;
            if (store == null) {
                task = Singleton.getVmware().createVM(vm, dest, uuid, hostnameNoSpace);
                PhysicalTopologyProvider.logger.info("VM is not assigned a specific store.");
            } else {
                task = Singleton.getVmware().createVM(vm, dest, uuid, hostnameNoSpace, store);
                PhysicalTopologyProvider.logger.info("VM will use store {}.", store);
            }
            taskid = task.getUniqueIdentifier();
            VmBootStatus.addStatus(task);
        } catch (VMwareException | NoSuchVMException | VMLaunchException e) {
            PhysicalTopologyProvider.logger.catching(Level.ERROR, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        PhysicalTopologyProvider.logger.info("Created new VM start task. Using ID {}.", taskid);
        return Response.created(new URI(VmBootStatusProvider.PATH + "/" + taskid)).build();
    }

    /**
     * Retrieves a {@link PhysicalResource} that matches the given ID.
     * 
     * @param uuid
     *            The ID of the {@link PhysicalResource}.
     * @param type
     *            The type of the {@link PhysicalResource}. Currently,
     *            {@link PhysicalHost} and {@link PhysicalStore} are supported.
     * @return The {@link PhysicalResource}.
     */
    @Path(PhysicalTopologyProvider.PATH_UUID)
    @GET
    public Response getPhysForResource(@PathParam("uuid") final String uuid,
            @QueryParam("type") final String type) {
        PhysicalTopologyProvider.logger.info("Getting topology for resource {}.", uuid);
        try {
            if (type == null) {
                PhysicalTopologyProvider.logger.warn("No resource type given.");
                return Response.status(Status.NOT_FOUND).build();
            } else if (type.equals(PhysicalTopologyProvider.STORE_INFO)) {
                PhysicalTopologyProvider.logger.info("Resource is of type {}.",
                                                     PhysicalTopologyProvider.STORE_INFO);
                final PhysicalStore store = Singleton.getVmware().getStoreInfo(uuid);
                return Response.ok(XMLConverter.convertJDOMElementW3CDocument(TopologyFactories
                        .createDefault().getPhysicalStoreFactory().toXML(store))).build();
            } else if (type.equals(PhysicalTopologyProvider.HOST_INFO)) {
                PhysicalTopologyProvider.logger.info("Resource is of type {}.",
                                                     PhysicalTopologyProvider.HOST_INFO);
                final PhysicalHost host = Singleton.getVmware().getHostInfo(uuid);
                return Response.ok(XMLConverter.convertJDOMElementW3CDocument(TopologyFactories
                        .createDefault().getPhysicalCPUFactory().toXML(host))).build();
            } else {
                PhysicalTopologyProvider.logger.warn("Invalid resource type: {}.", type);
                return Response.status(Status.NOT_FOUND).build();
            }
        } catch (final JDOMException | VMwareException e) {
            PhysicalTopologyProvider.logger.catching(Level.ERROR, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
