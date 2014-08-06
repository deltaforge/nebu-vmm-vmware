package nl.bitbrains.nebu.vmm.vmware.provider;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.cache.CacheManager;
import nl.bitbrains.nebu.common.topology.PhysicalRoot;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.common.topology.factory.TopologyFactories;
import nl.bitbrains.nebu.common.topology.factory.TopologyFactory;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.vmm.vmware.api.Singleton;
import nl.bitbrains.nebu.vmm.vmware.entity.VMTemplate;
import nl.bitbrains.nebu.vmm.vmware.entity.VMTemplateFactory;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.w3c.dom.Document;

/**
 * Provides REST API for launching new VMs.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
@Path(VMTemplateProvider.PATH)
public class VMTemplateProvider {

    /**
     * The logger for this class.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * The path on which this service is accessible.
     */
    public static final String PATH = "/vmtemplates";

    /**
     * The UUID path.
     */
    public static final String UUID_PATH = "{" + VMTemplateProvider.UUID_NAME + "}";

    /**
     * Parameter name for template UUID.
     */
    public static final String UUID_NAME = "templateUuid";

    /**
     * Physical Topology path.
     */
    public static final String UUID_PHYS_PATH = VMTemplateProvider.UUID_PATH + "/phys";

    /**
     * Map key for VM templates.
     */
    public static final String CACHE_KEY_TEMPLATES = "templateCache";

    /**
     * Gets the cache, also accessible for other classes.
     * 
     * @return the map of applications currently known to the system.
     */
    @SuppressWarnings("unchecked")
    public static final Map<String, VMTemplate> getCache() {
        Map<String, VMTemplate> map;
        try {
            map = (Map<String, VMTemplate>) CacheManager
                    .get(VMTemplateProvider.CACHE_KEY_TEMPLATES);
        } catch (final CacheException | IllegalArgumentException e) {
            map = new HashMap<String, VMTemplate>();
            CacheManager.put(VMTemplateProvider.CACHE_KEY_TEMPLATES, map);
        }
        return map;
    }

    /**
     * Puts a new virtual machine template.
     * 
     * @param doc
     *            A {@link Document} that represents a {@link VMTemplate}.
     * @param uuid
     *            The UUID id of this new {@link VMTemplate}.
     * @return The URI where the new {@link VMTemplate} can be found on the REST
     *         API.
     */
    @Path(VMTemplateProvider.UUID_PATH)
    @PUT
    public Response putTemplate(final Document doc,
            @PathParam(VMTemplateProvider.UUID_NAME) final String uuid) {
        VMTemplateProvider.logger.info("Receiving new VM template with uuid {}.", uuid);
        Response rep;
        try {
            final VMTemplate template = new VMTemplateFactory()
                    .fromXML(XMLConverter.convertW3CDocumentJDOMElement(doc)).withUuid(uuid)
                    .build();
            VMTemplateProvider.getCache().put(template.getUniqueIdentifier(), template);
            VMTemplateProvider.logger.info("Created new VM template with uuid {}.", uuid);
            rep = Response.created(null).build();
        } catch (final ParseException e) {
            VMTemplateProvider.logger.catching(Level.ERROR, e);
            rep = Response.status(Status.BAD_REQUEST).build();
        }
        return rep;
    }

    /**
     * Retrieves a {@link PhysicalTopology} object that contains all resources
     * available to this {@link VMTemplate}.
     * 
     * @param uuid
     *            The UUID of the {@link VMTemplate}.
     * @return A {@link PhysicalTopology}.
     */
    @Path(VMTemplateProvider.UUID_PHYS_PATH)
    @GET
    public Response getTemplatePhys(@PathParam(VMTemplateProvider.UUID_NAME) final String uuid) {
        // TODO Remove Pokemon catch.
        try {
            final VMTemplate template = VMTemplateProvider.getCache().get(uuid);
            VMTemplateProvider.logger.info("Getting topology for VM template {}.", uuid);
            if (template != null) {
                final List<String> vAppIds = template.getDestVApps();
                PhysicalTopology topology = null;
                try {
                    topology = Singleton.getVmware().getPhysicalTopologyForVapps(vAppIds);
                } catch (final VMwareException e1) {
                    VMTemplateProvider.logger.catching(Level.ERROR, e1);
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
                try {
                    final TopologyFactory<PhysicalRoot> factory = TopologyFactories.createDefault()
                            .getPhysicalRootFactory();
                    VMTemplateProvider.logger.info("Returning topology {} for VM template {}.",
                                                   topology,
                                                   uuid);
                    return Response.ok(XMLConverter.convertJDOMElementW3CDocument(factory
                            .toXML(topology.getRoot()))).build();
                } catch (final JDOMException e) {
                    VMTemplateProvider.logger.catching(Level.ERROR, e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
            }
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            VMTemplateProvider.logger.catching(e);
            return null;
        }
    }
}
