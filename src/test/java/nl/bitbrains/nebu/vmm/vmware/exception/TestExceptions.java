package nl.bitbrains.nebu.vmm.vmware.exception;

import nl.bitbrains.nebu.vmm.vmware.exception.NoPhysicalTopologyException;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMLaunchException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestExceptions {

    private String message;
    private Exception exception;

    @Before
    public void setUp() {
        this.message = "message";
        this.exception = new Exception();
    }

    @Test
    public void testNoPhysicalTopologyException() {
        final NoPhysicalTopologyException e = new NoPhysicalTopologyException(this.message);

        Assert.assertNotNull(e);
        Assert.assertEquals(this.message, e.getMessage());
    }

    @Test
    public void testNoSuchVMException() {
        final NoSuchVMException e = new NoSuchVMException(this.message);

        Assert.assertNotNull(e);
        Assert.assertEquals(this.message, e.getMessage());
    }

    @Test
    public void testVMLaunchException() {
        final VMLaunchException e = new VMLaunchException(this.message);

        Assert.assertNotNull(e);
        Assert.assertEquals(this.message, e.getMessage());
    }

    @Test
    public void testVMLaunchExceptionThrowable() {
        final VMLaunchException e = new VMLaunchException(this.message, this.exception);

        Assert.assertNotNull(e);
        Assert.assertEquals(this.message, e.getMessage());
        Assert.assertEquals(this.exception, e.getCause());
    }

    @Test
    public void testVMwareException() {
        final VMwareException e = new VMwareException(this.message);

        Assert.assertNotNull(e);
        Assert.assertEquals(this.message, e.getMessage());
    }

    @Test
    public void testVMwareExceptionThrowable() {
        final VMwareException e = new VMwareException(this.message, this.exception);

        Assert.assertNotNull(e);
        Assert.assertEquals(this.message, e.getMessage());
        Assert.assertEquals(this.exception, e.getCause());
    }
}
