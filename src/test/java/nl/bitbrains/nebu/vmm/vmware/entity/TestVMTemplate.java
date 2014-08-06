package nl.bitbrains.nebu.vmm.vmware.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nl.bitbrains.nebu.vmm.vmware.entity.VMTemplate;
import nl.bitbrains.nebu.vmm.vmware.entity.VMTemplateBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestVMTemplate {

    private VMTemplate template, template2;
    private final String uuid = "uuid";
    private final String name = "name";
    private final String templateStr = "template";
    private final String srcvapp = "srcvapp";
    private final String dstvapp = "dstvapp";

    @Before
    public void setupTemplate2() {
        this.template2 = new VMTemplateBuilder().withUuid(this.uuid).build();
    }

    @Test
    public void testWithUuid() {
        final VMTemplate template = new VMTemplateBuilder().withUuid(this.uuid).build();

        Assert.assertEquals(this.uuid, template.getUniqueIdentifier());
    }

    @Test
    public void testWithName() {
        final VMTemplate template = new VMTemplateBuilder().withUuid(this.uuid)
                .withName(this.name).build();

        Assert.assertEquals(this.name, template.getName());
    }

    @Test
    public void testWithTemplate() {
        final VMTemplate template = new VMTemplateBuilder().withUuid(this.uuid)
                .withTemplate(this.templateStr).build();

        Assert.assertEquals(this.templateStr, template.getTemplate());
    }

    @Test
    public void testWithSrcVapp() {
        final VMTemplate template = new VMTemplateBuilder().withUuid(this.uuid)
                .withSrcVapp(this.srcvapp).build();

        Assert.assertEquals(this.srcvapp, template.getScrVApp());
    }

    @Test
    public void testWithDstVapp() {
        final VMTemplate template = new VMTemplateBuilder().withUuid(this.uuid)
                .withDestVapp(this.dstvapp).build();

        Assert.assertEquals(this.dstvapp, template.getDestVApps().get(0));
    }

    @Test
    public void testWithDstVapps() {
        final List<String> dstVapps = new ArrayList<String>(1);
        dstVapps.add(this.dstvapp);
        final VMTemplate template = new VMTemplateBuilder().withUuid(this.uuid)
                .withDestVapps(dstVapps).build();

        Assert.assertEquals(dstVapps, template.getDestVApps());
    }

    @Test
    public void testSetterUuid() {
        final String uuid = UUID.randomUUID().toString();
        this.template2.setUuid(uuid);

        Assert.assertEquals(uuid, this.template2.getUniqueIdentifier());
    }

    @Test
    public void testSetterName() {
        final String name = UUID.randomUUID().toString();
        this.template2.setName(name);

        Assert.assertEquals(name, this.template2.getName());
    }

    @Test
    public void testSetterTemplate() {
        final String template = UUID.randomUUID().toString();
        this.template2.setTemplate(template);

        Assert.assertEquals(template, this.template2.getTemplate());
    }

    @Test
    public void testSetterSrcVapp() {
        final String srcvapp = UUID.randomUUID().toString();
        this.template2.setScrVApp(srcvapp);

        Assert.assertEquals(srcvapp, this.template2.getScrVApp());
    }

    @Test
    public void testSettingDstVapps() {
        final List<String> dstvapps = new ArrayList<String>(1);
        final String dstvapp = UUID.randomUUID().toString();
        dstvapps.add(dstvapp);
        this.template2.setDestVApps(dstvapps);

        Assert.assertEquals(dstvapps, this.template2.getDestVApps());
    }
}
