package nl.bitbrains.nebu.vmm.vmware.provider;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.VirtualMachineBuilder;
import nl.bitbrains.nebu.common.factories.StringFactory;
import nl.bitbrains.nebu.common.factories.VirtualMachineFactory;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.vmm.vmware.api.Singleton;
import nl.bitbrains.nebu.vmm.vmware.api.VMware;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;

import com.vmware.vcloud.sdk.VCloudException;

public class TestVirtualResourceProvider extends JerseyTest {

    @Mock
    VMware vmw;

    @Mock
    VirtualMachine vm;

    @Override
    protected Application configure() {
        MockitoAnnotations.initMocks(this);
        return new ResourceConfig(VirtualResourceProvider.class);
    }

    @Test
    public void testGetVirtualResources() throws VCloudException, ParseException, VMwareException {
        Singleton.setVmware(this.vmw);

        Mockito.when(this.vmw.getVirtualResourceList()).thenReturn(new ArrayList<String>());
        final Document xml = this.target(VirtualResourceProvider.PATH).request()
                .get(Document.class);
        final Element elem = XMLConverter.convertW3CDocumentJDOMElement(xml);
        final List<String> list = XMLConverter.convertJDOMElementToList(elem, new StringFactory());
        Assert.assertTrue("Empty list", list.isEmpty());
    }

    @Test
    public void testGetVirtualResourcesCannotGetList() throws VCloudException, ParseException,
            VMwareException {
        Singleton.setVmware(this.vmw);

        Mockito.when(this.vmw.getVirtualResourceList()).thenThrow(new VMwareException(""));
        final Response resp = this.target(VirtualResourceProvider.PATH).request().get();

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testGetVirtualMachine404() throws VCloudException, NoSuchVMException,
            VMwareException {
        Singleton.setVmware(this.vmw);

        Mockito.when(this.vmw.getVirtualMachineInfo(Matchers.anyString()))
                .thenThrow(new NoSuchVMException("test"));

        final Response resp = this.target("virt/0").request().get();
        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testGetVirtualMachine200() throws VMwareException, NoSuchVMException,
            ParseException {
        Singleton.setVmware(this.vmw);

        final String uuid = "test-uuid";
        final VirtualMachine vm = new VirtualMachineBuilder().withUuid(uuid).withHost(uuid).build();
        Mockito.when(this.vmw.getVirtualMachineInfo(Matchers.eq(uuid))).thenReturn(vm);

        final Response resp = this.target("virt/" + uuid).request().get();
        final VirtualMachine res = new VirtualMachineFactory()
                .fromXML(XMLConverter.convertW3CDocumentJDOMElement(resp.readEntity(Document.class)))
                .build();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        Assert.assertEquals(vm.getUniqueIdentifier(), res.getUniqueIdentifier());
        Assert.assertEquals(vm.getHost(), res.getHost());
    }

    @Test
    public void testDeleteVM() {
        Singleton.setVmware(this.vmw);

        final String uuid = "uuid";

        final Response resp = this.target("virt/" + uuid).request().delete();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testDeleteVMByGet() {
        Singleton.setVmware(this.vmw);

        final String uuid = "uuid";

        final Response resp = this.target("virt/" + uuid + "/assassinate").request().get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testDeleteVMThatDoesNotExist() throws VMwareException, NoSuchVMException {
        Singleton.setVmware(this.vmw);

        final String uuid = "uuid";

        Mockito.when(this.vmw.getVirtualMachineInfo(Matchers.eq(uuid)))
                .thenThrow(new VMwareException(""));

        final Response resp = this.target("virt/" + uuid).request().delete();

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testDeleteVMThatDoesNotExistByGet() throws VMwareException, NoSuchVMException {
        Singleton.setVmware(this.vmw);

        final String uuid = "uuid";

        Mockito.when(this.vmw.getVirtualMachineInfo(Matchers.eq(uuid)))
                .thenThrow(new VMwareException(""));

        final Response resp = this.target("virt/" + uuid + "/assassinate").request().get();

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testMoveVMNoParams() {
        Singleton.setVmware(this.vmw);

        final String uuid = "uuid";

        final Response resp = this.target("virt/" + uuid + "/move").request().get();

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testMoveVMHost() {
        Singleton.setVmware(this.vmw);

        final String uuid = "uuid";
        final String host = "host";

        final Response resp = this.target("virt/" + uuid + "/move").queryParam("host", host)
                .request().get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testMoveVMHostCouldNotGetVMInfo() throws VMwareException, NoSuchVMException {
        Singleton.setVmware(this.vmw);

        final String uuid = "uuid";
        final String host = "host";

        Mockito.doThrow(new VMwareException("")).when(this.vmw)
                .getVirtualMachineInfo(Matchers.eq(uuid));

        final Response resp = this.target("virt/" + uuid + "/move").queryParam("host", host)
                .request().get();

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testMoveVMHostCouldNotGetHostInfo() throws VMwareException {
        Singleton.setVmware(this.vmw);

        final String uuid = "uuid";
        final String host = "host";

        Mockito.doThrow(new VMwareException("")).when(this.vmw).getHostInfo(Matchers.eq(host));

        final Response resp = this.target("virt/" + uuid + "/move").queryParam("host", host)
                .request().get();

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testMoveVMHostMoveFailure() throws VMwareException {
        Singleton.setVmware(this.vmw);

        final String uuid = "uuid";
        final String host = "host";

        Mockito.doThrow(new VMwareException("")).when(this.vmw)
                .moveVMToHost(Matchers.any(VirtualMachine.class), Matchers.any(PhysicalHost.class));

        final Response resp = this.target("virt/" + uuid + "/move").queryParam("host", host)
                .request().get();

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testMoveVMStore() {
        Singleton.setVmware(this.vmw);

        final String uuid = "uuid";
        final String store = "store";

        final Response resp = this.target("virt/" + uuid + "/move").queryParam("store", store)
                .request().get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

    }

    @Test
    public void testMoveVMHostCouldNotGetStoreInfo() throws VMwareException {
        Singleton.setVmware(this.vmw);

        final String uuid = "uuid";
        final String store = "store";

        Mockito.doThrow(new VMwareException("")).when(this.vmw).getStoreInfo(Matchers.eq(store));

        final Response resp = this.target("virt/" + uuid + "/move").queryParam("store", store)
                .request().get();

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testMoveVMStoreMoveFailure() throws VMwareException {
        Singleton.setVmware(this.vmw);

        final String uuid = "uuid";
        final String store = "store";

        Mockito.doThrow(new VMwareException(""))
                .when(this.vmw)
                .moveVMToStore(Matchers.any(VirtualMachine.class),
                               Matchers.any(PhysicalStore.class));

        final Response resp = this.target("virt/" + uuid + "/move").queryParam("store", store)
                .request().get();

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }
}
