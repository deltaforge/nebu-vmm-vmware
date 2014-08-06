package nl.bitbrains.nebu.vmm.vmware.provider;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.vmm.vmware.entity.VmBootStatus;
import nl.bitbrains.nebu.vmm.vmware.provider.VmBootStatusProvider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class TestVmBootStatusProvider extends JerseyTest {

    @Override
    protected Application configure() {
        MockitoAnnotations.initMocks(this);
        return new ResourceConfig(VmBootStatusProvider.class);
    }

    @Test
    public void testNonExistingStatus() {
        final String uuid = "this-uuid-does-not-exist";
        final Response resp = this.target(VmBootStatusProvider.PATH + "/" + uuid).request().get();

        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testStatusNotFound() {
        final String uuid = "this-uuid-does-exist";
        final VmBootStatus status = new VmBootStatus();
        status.setStatus(VmBootStatus.Status.FAILURE);
        VmBootStatus.clearStatusList();
        final Response resp = this.target(VmBootStatusProvider.PATH + "/" + uuid).request().get();

        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testStatusNotSuccess() {
        final VmBootStatus status = new VmBootStatus();
        status.setStatus(VmBootStatus.Status.FAILURE);
        VmBootStatus.clearStatusList();
        VmBootStatus.addStatus(status);
        final Response resp = this
                .target(VmBootStatusProvider.PATH + "/" + status.getUniqueIdentifier()).request()
                .get();

        Assert.assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testStatusSuccess() {
        final VmBootStatus status = new VmBootStatus();
        status.setStatus(VmBootStatus.Status.SUCCESS);
        VmBootStatus.clearStatusList();
        VmBootStatus.addStatus(status);
        final Response resp = this
                .target(VmBootStatusProvider.PATH + "/" + status.getUniqueIdentifier()).request()
                .get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
    }

}
