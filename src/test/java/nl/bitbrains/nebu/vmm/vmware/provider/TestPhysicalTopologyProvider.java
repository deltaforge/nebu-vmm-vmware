package nl.bitbrains.nebu.vmm.vmware.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.cache.CacheException;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalHostBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.PhysicalStoreBuilder;
import nl.bitbrains.nebu.vmm.vmware.api.Singleton;
import nl.bitbrains.nebu.vmm.vmware.api.VMware;
import nl.bitbrains.nebu.vmm.vmware.entity.VMTemplate;
import nl.bitbrains.nebu.vmm.vmware.entity.VirtualApplication;
import nl.bitbrains.nebu.vmm.vmware.entity.VmBootStatus;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMLaunchException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;
import nl.bitbrains.nebu.vmm.vmware.provider.PhysicalTopologyProvider;
import nl.bitbrains.nebu.vmm.vmware.provider.VMTemplateProvider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(VMTemplateProvider.class)
@PowerMockIgnore({ "javax.management.*" })
public class TestPhysicalTopologyProvider extends JerseyTest {

    @Mock
    VMware vmw;
    @Mock
    VMTemplate mockTemplate;
    @Mock
    VirtualMachine vm;
    @Mock
    VirtualApplication vapp;
    @Mock
    VmBootStatus task;
    @Mock
    PhysicalHost host;

    @Override
    protected Application configure() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(VMTemplateProvider.class);
        return new ResourceConfig(PhysicalTopologyProvider.class);
    }

    private void setUpTemplateCache(final String key, final VMTemplate value) throws CacheException {
        final Map<String, VMTemplate> values = new HashMap<String, VMTemplate>();
        values.put(key, value);
        Mockito.when(VMTemplateProvider.getCache()).thenReturn(values);
    }

    private void setUpCreateVM(final VMware vmware, final VMTemplate template,
            final String vmTemplate, final VirtualMachine vm, final String hostId,
            final PhysicalHost host, final VirtualApplication vapp, final VmBootStatus task)
            throws VMwareException, NoSuchVMException, VMLaunchException {
        Mockito.when(template.getTemplate()).thenReturn(vmTemplate);
        Mockito.when(template.getDestVApps()).thenReturn(new ArrayList<String>());
        Mockito.when(vmware.getVirtualMachineInfo(Matchers.eq(vmTemplate))).thenReturn(vm);
        Mockito.when(vmware.getHostInfo(Matchers.eq(hostId))).thenReturn(host);
        Mockito.when(vmware.selectVirtualApplicationFromHost(Matchers.eq(host),
                                                             Matchers.anyListOf(String.class)))
                .thenReturn(this.vapp);
        Mockito.when(vmware.createVM(Matchers.any(VirtualMachine.class),
                                     Matchers.any(VirtualApplication.class),
                                     Matchers.anyString(),
                                     Matchers.anyString())).thenReturn(task);
        Mockito.when(vmware.createVM(Matchers.any(VirtualMachine.class),
                                     Matchers.any(VirtualApplication.class),
                                     Matchers.anyString(),
                                     Matchers.anyString(),
                                     Matchers.anyString())).thenReturn(task);
    }

    private PhysicalHost createPhysicalHost() {
        return new PhysicalHostBuilder().withCpuUsage(0).withMemUsage(0).withUuid("uuid").build();
    }

    private PhysicalStore createPhysicalStore() {
        return new PhysicalStoreBuilder().withUsed(0).withCapacity(0).withUuid("uuid").build();
    }

    @Test
    public void testCreateVMNoTemplate() {
        Singleton.setVmware(this.vmw);
        final String uuid = "uuid";
        final String hostname = "this-is-a-hostname";

        final Response resp = this.target(PhysicalTopologyProvider.PATH + "/" + uuid + "/createVM")
                .queryParam("hostname", hostname).request().post(null);

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testCreateVMNoHostname() {
        Singleton.setVmware(this.vmw);
        final String uuid = "uuid";
        final String template = "this-is-a-template";

        final Response resp = this.target(PhysicalTopologyProvider.PATH + "/" + uuid + "/createVM")
                .queryParam("template", template).request().post(null);

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testCreateVMHasHostnameHasTemplate() throws CacheException, VMwareException,
            NoSuchVMException, VMLaunchException {
        Singleton.setVmware(this.vmw);
        final String uuid = "uuid";
        final String template = "this-is-a-template";
        final String hostname = "this-is-a-hostname";
        final String vmwareTemplate = "this-is-a-vmware-template";
        this.setUpTemplateCache(template, this.mockTemplate);
        this.setUpCreateVM(this.vmw,
                           this.mockTemplate,
                           vmwareTemplate,
                           this.vm,
                           uuid,
                           this.host,
                           this.vapp,
                           this.task);

        final Response resp = this.target(PhysicalTopologyProvider.PATH + "/" + uuid + "/createVM")
                .queryParam("hostname", hostname).queryParam("template", template).request()
                .post(null);

        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testCreateVMHasHostnameHasTemplateWithStore() throws CacheException,
            VMwareException, NoSuchVMException, VMLaunchException {
        Singleton.setVmware(this.vmw);
        final String uuid = "uuid";
        final String template = "this-is-a-template";
        final String hostname = "this-is-a-hostname";
        final String vmwareTemplate = "this-is-a-vmware-template";
        final String store = "this-is-a-store";
        this.setUpTemplateCache(template, this.mockTemplate);
        this.setUpCreateVM(this.vmw,
                           this.mockTemplate,
                           vmwareTemplate,
                           this.vm,
                           uuid,
                           this.host,
                           this.vapp,
                           this.task);

        final Response resp = this.target(PhysicalTopologyProvider.PATH + "/" + uuid + "/createVM")
                .queryParam("hostname", hostname).queryParam("template", template)
                .queryParam("store", store).request().post(null);

        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testCreateVMVirtualMachineInfoVMwareException() throws CacheException,
            VMwareException, NoSuchVMException, VMLaunchException {
        Singleton.setVmware(this.vmw);
        final String uuid = "uuid";
        final String template = "this-is-a-template";
        final String hostname = "this-is-a-hostname";
        final String vmwareTemplate = "this-is-a-vmware-template";
        final String store = "this-is-a-store";
        this.setUpTemplateCache(template, this.mockTemplate);
        this.setUpCreateVM(this.vmw,
                           this.mockTemplate,
                           vmwareTemplate,
                           this.vm,
                           uuid,
                           this.host,
                           this.vapp,
                           this.task);
        Mockito.when(this.vmw.getVirtualMachineInfo(Matchers.anyString()))
                .thenThrow(new VMwareException(""));

        final Response resp = this.target(PhysicalTopologyProvider.PATH + "/" + uuid + "/createVM")
                .queryParam("hostname", hostname).queryParam("template", template)
                .queryParam("store", store).request().post(null);

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testCreateVMVirtualMachineInfoNoSuchVMException() throws CacheException,
            VMwareException, NoSuchVMException, VMLaunchException {
        Singleton.setVmware(this.vmw);
        final String uuid = "uuid";
        final String template = "this-is-a-template";
        final String hostname = "this-is-a-hostname";
        final String vmwareTemplate = "this-is-a-vmware-template";
        final String store = "this-is-a-store";
        this.setUpTemplateCache(template, this.mockTemplate);
        this.setUpCreateVM(this.vmw,
                           this.mockTemplate,
                           vmwareTemplate,
                           this.vm,
                           uuid,
                           this.host,
                           this.vapp,
                           this.task);
        Mockito.when(this.vmw.getVirtualMachineInfo(Matchers.anyString()))
                .thenThrow(new NoSuchVMException(""));

        final Response resp = this.target(PhysicalTopologyProvider.PATH + "/" + uuid + "/createVM")
                .queryParam("hostname", hostname).queryParam("template", template)
                .queryParam("store", store).request().post(null);

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testGetPhysNoType() {
        Singleton.setVmware(this.vmw);

        final String uuid = "this-is-a-uuid";

        final Response resp = this.target(PhysicalTopologyProvider.PATH + "/" + uuid).request()
                .get();

        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testGetPhysForHost() throws VMwareException, NoSuchVMException {
        Singleton.setVmware(this.vmw);
        final PhysicalHost realHost = this.createPhysicalHost();
        Mockito.when(this.vmw.getHostInfo(Matchers.anyString())).thenReturn(realHost);

        final String uuid = "this-is-a-uuid";
        final String type = PhysicalTopologyProvider.HOST_INFO;

        final Response resp = this.target(PhysicalTopologyProvider.PATH + "/" + uuid)
                .queryParam("type", type).request().get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testGetPhysForStore() throws VMwareException {
        Singleton.setVmware(this.vmw);
        final PhysicalStore realStore = this.createPhysicalStore();
        Mockito.when(this.vmw.getStoreInfo(Matchers.anyString())).thenReturn(realStore);

        final String uuid = "this-is-a-uuid";
        final String type = PhysicalTopologyProvider.STORE_INFO;

        final Response resp = this.target(PhysicalTopologyProvider.PATH + "/" + uuid)
                .queryParam("type", type).request().get();

        Assert.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testGetPhysInvalidType() throws VMwareException {
        Singleton.setVmware(this.vmw);

        final String uuid = "this-is-a-uuid";
        final String type = "Some-weird-type";

        final Response resp = this.target(PhysicalTopologyProvider.PATH + "/" + uuid)
                .queryParam("type", type).request().get();

        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testGetPhysThrowsException() throws VMwareException {
        Singleton.setVmware(this.vmw);
        Mockito.when(this.vmw.getStoreInfo(Matchers.anyString()))
                .thenThrow(new VMwareException(""));

        final String uuid = "this-is-a-uuid";
        final String type = PhysicalTopologyProvider.STORE_INFO;

        final Response resp = this.target(PhysicalTopologyProvider.PATH + "/" + uuid)
                .queryParam("type", type).request().get();

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }
}
