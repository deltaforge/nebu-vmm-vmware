package nl.bitbrains.nebu.vmm.vmware.provider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.factories.StringFactory;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.vmm.vmware.entity.VmBootStatus;
import nl.bitbrains.nebu.vmm.vmware.entity.VmBootStatus.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;

/**
 * Provides the status URI of the vmm vmware REST API.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
@Path(VmBootStatusProvider.PATH)
public class VmBootStatusProvider {

    /**
     * The logger for this class.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * The provider path.
     */
    public static final String PATH = "/status";

    /**
     * Retrieves the {@link VmBootStatus} with the given ID.
     * 
     * @param sha1
     *            The ID of the {@link VmBootStatus}.
     * @return The ID of the newly booted virtual machine, or an error code if
     *         the virtual machine is not yet ready.
     * @throws JDOMException
     *             If an error occurs in the XML conversion.
     */
    @GET
    @Path("{sha1}")
    public Response getStatus(@PathParam("sha1") final String sha1) throws JDOMException {
        VmBootStatusProvider.logger.info("Retrieving status information on task {}.", sha1);

        final VmBootStatus status = VmBootStatus.getStatus(sha1);
        if (status != null) {
            if (status.getStatus() == Status.SUCCESS) {
                VmBootStatusProvider.logger.info("Task {} is {}.", sha1, Status.SUCCESS.name());
                return Response.ok(XMLConverter.convertJDOMElementW3CDocument((new StringFactory()
                        .toXML(status.getVmId())))).build();
            } else {
                VmBootStatusProvider.logger.warn("Task {} is not in the {} state.",
                                                 sha1,
                                                 Status.SUCCESS.name());
                return Response.status(Response.Status.ACCEPTED).build();
            }
        }
        VmBootStatusProvider.logger.warn("Could not find task {}", sha1);
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
