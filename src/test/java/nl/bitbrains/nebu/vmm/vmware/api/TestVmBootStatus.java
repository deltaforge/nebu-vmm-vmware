package nl.bitbrains.nebu.vmm.vmware.api;

import nl.bitbrains.nebu.vmm.vmware.entity.VmBootStatus;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestVmBootStatus {

    private VmBootStatus bootStatus;

    @Before
    public void setUp() {
        this.bootStatus = new VmBootStatus();
    }

    @Test
    public void testGetUUID() {
        Assert.assertNotNull(this.bootStatus.getUniqueIdentifier());
        Assert.assertNotEquals("", this.bootStatus.getUniqueIdentifier());
    }

    @Test
    public void testEmptyVmId() {
        Assert.assertEquals("", this.bootStatus.getVmId());
    }

    @Test
    public void testSetVmId() {
        final String id = "id";
        this.bootStatus.setVmId(id);

        Assert.assertEquals(id, this.bootStatus.getVmId());
    }

    @Test
    public void testGetStatus() {
        Assert.assertEquals(VmBootStatus.Status.CREATED, this.bootStatus.getStatus());
    }

    @Test
    public void testSetStatus() {
        final VmBootStatus.Status status = VmBootStatus.Status.SUCCESS;
        this.bootStatus.setStatus(status);

        Assert.assertEquals(status, this.bootStatus.getStatus());
    }

}
