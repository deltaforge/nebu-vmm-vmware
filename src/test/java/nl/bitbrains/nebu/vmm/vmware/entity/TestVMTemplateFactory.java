package nl.bitbrains.nebu.vmm.vmware.entity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import nl.bitbrains.nebu.common.factories.IdentifiableFactory;
import nl.bitbrains.nebu.vmm.vmware.entity.VMTemplate;
import nl.bitbrains.nebu.vmm.vmware.entity.VMTemplateBuilder;
import nl.bitbrains.nebu.vmm.vmware.entity.VMTemplateFactory;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestVMTemplateFactory {

    private VMTemplate vmtemplate;
    private String uuid;
    private String name;
    private String template;
    private String srcvapp;
    private String desvapp;
    private VMTemplateFactory factory;

    @Before
    public void setUp() {
        this.uuid = "uuid";
        this.name = "name";
        this.template = "template";
        this.srcvapp = "srcvapp";
        this.desvapp = "desvapp";
        this.vmtemplate = new VMTemplateBuilder().withUuid(this.uuid).withSrcVapp(this.srcvapp)
                .build();
        this.factory = new VMTemplateFactory();
    }

    private Element createVMTemplateElement(final String id, final String srcvapp,
            final String name, final String template, final List<String> dstvapps) {
        final Element elem = new Element(VMTemplateFactory.TAG_ELEMENT_ROOT);
        elem.setAttribute(IdentifiableFactory.TAG_ID, id);
        elem.setAttribute(VMTemplateFactory.ATTRIBUTE_TYPE,
                          VMTemplateFactory.ATTRIBUTE_TYPE_VMWARE_VALUE);
        elem.addContent(new Element(VMTemplateFactory.TAG_NAME).setText(name));
        elem.addContent(new Element(VMTemplateFactory.TAG_TEMPLATE).setText(template));
        elem.addContent(new Element(VMTemplateFactory.TAG_SCRVAPP)
                .setAttribute(IdentifiableFactory.TAG_ID, srcvapp));
        final Element depvapps = new Element(VMTemplateFactory.TAG_DESVAPP);
        for (final String vapp : dstvapps) {
            depvapps.addContent(new Element(VMTemplateFactory.TAG_VAPP)
                    .setAttribute(IdentifiableFactory.TAG_ID, vapp));
        }
        elem.addContent(depvapps);
        return elem;
    }

    private Element createSimpleVMTemplateElement() {
        final List<String> dstvapps = new ArrayList<String>(1);
        dstvapps.add(this.desvapp);
        return this.createVMTemplateElement(this.uuid,
                                            this.srcvapp,
                                            this.name,
                                            this.template,
                                            dstvapps);
    }

    @Test
    public void testToXmlHasId() {
        final Element elem = this.factory.toXML(this.vmtemplate);

        Assert.assertEquals(this.uuid, elem.getAttributeValue(IdentifiableFactory.TAG_ID));
    }

    @Test
    public void testToXmlHasSrcVapp() {
        final Element elem = this.factory.toXML(this.vmtemplate);

        Assert.assertEquals(this.srcvapp, elem.getChild(VMTemplateFactory.TAG_SCRVAPP)
                .getAttributeValue(IdentifiableFactory.TAG_ID));
    }

    @Test
    public void testToXmlHasDstVappList() {
        final Element elem = this.factory.toXML(this.vmtemplate);

        Assert.assertNotNull(elem.getChild(VMTemplateFactory.TAG_DESVAPP));
    }

    @Test
    public void testToXmlHasSingleDstVapp() {
        this.vmtemplate.getDestVApps().add(this.desvapp);
        final Element elem = this.factory.toXML(this.vmtemplate);

        Assert.assertNotNull(elem.getChild(VMTemplateFactory.TAG_DESVAPP));
        Assert.assertEquals(1,
                            elem.getChild(VMTemplateFactory.TAG_DESVAPP)
                                    .getChildren(VMTemplateFactory.TAG_VAPP).size());
    }

    @Test
    public void testFromXmlHasUuid() throws ParseException {
        final VMTemplate parsedTemplate = this.factory
                .fromXML(this.createSimpleVMTemplateElement()).build();

        Assert.assertEquals(this.uuid, parsedTemplate.getUniqueIdentifier());
    }

    @Test
    public void testFromXmlHasName() throws ParseException {
        final VMTemplate parsedTemplate = this.factory
                .fromXML(this.createSimpleVMTemplateElement()).build();

        Assert.assertEquals(this.name, parsedTemplate.getName());
    }

    @Test
    public void testFromXmlHasSrcVapp() throws ParseException {
        final VMTemplate parsedTemplate = this.factory
                .fromXML(this.createSimpleVMTemplateElement()).build();

        Assert.assertEquals(this.srcvapp, parsedTemplate.getScrVApp());
    }

    @Test
    public void testFromXmlHasDstVappList() throws ParseException {
        final VMTemplate parsedTemplate = this.factory
                .fromXML(this.createSimpleVMTemplateElement()).build();

        Assert.assertEquals(1, parsedTemplate.getDestVApps().size());
        Assert.assertEquals(this.desvapp, parsedTemplate.getDestVApps().get(0));
    }
}
