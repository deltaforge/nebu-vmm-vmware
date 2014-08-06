package nl.bitbrains.nebu.vmm.vmware.provider;

import java.text.ParseException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.bitbrains.nebu.common.factories.IntegerFactory;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.common.topology.factory.TopologyFactories;
import nl.bitbrains.nebu.common.util.xml.XMLConverter;
import nl.bitbrains.nebu.common.util.xml.XMLFactory;
import nl.bitbrains.nebu.vmm.vmware.api.Singleton;
import nl.bitbrains.nebu.vmm.vmware.api.VMware;
import nl.bitbrains.nebu.vmm.vmware.entity.VMTemplate;
import nl.bitbrains.nebu.vmm.vmware.entity.VMTemplateBuilder;
import nl.bitbrains.nebu.vmm.vmware.entity.VMTemplateFactory;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;
import nl.bitbrains.nebu.vmm.vmware.provider.VMTemplateProvider;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.jdom2.Element;
import org.jdom2.JDOMException;
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
import org.w3c.dom.Document;

@RunWith(PowerMockRunner.class)
@PrepareForTest(XMLConverter.class)
@PowerMockIgnore({ "javax.management.*" })
public class TestVMTemplateProvider extends JerseyTest {

    @Mock
    VMware vmw;

    @Override
    protected Application configure() {
        MockitoAnnotations.initMocks(this);
        Singleton.setVmware(this.vmw);
        return new ResourceConfig(VMTemplateProvider.class);
    }

    private PhysicalTopology getBasicTopology() {
        final PhysicalTopology topology = new PhysicalTopology();
        return topology;
    }

    private Entity<Document> getBasicTemplateAsEntity() throws JDOMException {
        return this.wrapObject(new VMTemplateFactory(), this.getBasicTemplate());
    }

    private VMTemplate getBasicTemplate() {
        return new VMTemplateBuilder().withUuid("uuid").withTemplate("template")
                .withSrcVapp("srcvapp").withName("name").withDestVapp("destvapp").build();
    }

    private <T> Entity<Document> wrapObject(final XMLFactory<T> fac, final T obj)
            throws JDOMException {
        return Entity.entity(XMLConverter.convertJDOMElementW3CDocument(fac.toXML(obj)),
                             MediaType.TEXT_XML);
    }

    private PhysicalTopology getTopologyFromResponse(final Response response) throws ParseException {
        return new PhysicalTopology(TopologyFactories
                .createDefault()
                .getPhysicalRootFactory()
                .fromXML(XMLConverter.convertW3CDocumentJDOMElement(response
                        .readEntity(Document.class))).build());
    }

    @Test
    public void testPutTemplate() throws JDOMException {
        final String uuid = "this-is-a-uuid";
        final Response resp = this.target(VMTemplateProvider.PATH + "/" + uuid).request()
                .put(this.getBasicTemplateAsEntity());

        Assert.assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testGetNonExistingTemplate() {
        final String uuid = "this-uuid-does-not-exist";
        final Response resp = this.target(VMTemplateProvider.PATH + "/" + uuid + "/phys").request()
                .get();

        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testPutAndGetTemplate() throws ParseException, JDOMException, VMwareException {
        final String uuid = "this-is-a-new-uuid";

        final PhysicalTopology topology = this.getBasicTopology();
        Mockito.when(this.vmw.getPhysicalTopologyForVapps(Matchers.anyListOf(String.class)))
                .thenReturn(topology);

        this.target(VMTemplateProvider.PATH + "/" + uuid).request()
                .put(this.getBasicTemplateAsEntity());
        final Response resp = this.target(VMTemplateProvider.PATH + "/" + uuid + "/phys").request()
                .get();
        final PhysicalTopology resTopology = this.getTopologyFromResponse(resp);

        Assert.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        Assert.assertEquals(topology, resTopology);
    }

    @Test
    public void testPutWrongTemplate() throws JDOMException {
        final String uuid = "this-uuid-is-used-to-set-an-incorrect-template";

        final Response resp = this.target(VMTemplateProvider.PATH + "/" + uuid).request()
                .put(this.wrapObject(new IntegerFactory(), 5));

        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testPutAndGetTemplateVMwareException() throws ParseException, JDOMException,
            VMwareException {
        final String uuid = "this-is-a-new-uuid";

        Mockito.when(this.vmw.getPhysicalTopologyForVapps(Matchers.anyListOf(String.class)))
                .thenThrow(new VMwareException(""));

        this.target(VMTemplateProvider.PATH + "/" + uuid).request()
                .put(this.getBasicTemplateAsEntity());
        final Response resp = this.target(VMTemplateProvider.PATH + "/" + uuid + "/phys").request()
                .get();

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testPutAndGetTemplateJDOMException() throws ParseException, JDOMException,
            VMwareException {
        final String uuid = "this-is-a-new-uuid";

        final PhysicalTopology topology = this.getBasicTopology();
        Mockito.when(this.vmw.getPhysicalTopologyForVapps(Matchers.anyListOf(String.class)))
                .thenReturn(topology);

        this.target(VMTemplateProvider.PATH + "/" + uuid).request()
                .put(this.getBasicTemplateAsEntity());

        PowerMockito.mockStatic(XMLConverter.class);
        PowerMockito.when(XMLConverter.convertJDOMElementW3CDocument(Matchers.any(Element.class)))
                .thenThrow(new JDOMException());

        final Response resp = this.target(VMTemplateProvider.PATH + "/" + uuid + "/phys").request()
                .get();

        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }
}
