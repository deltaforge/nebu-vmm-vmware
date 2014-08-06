package nl.bitbrains.nebu.vmm.vmware.api;

import java.rmi.RemoteException;
import java.util.concurrent.TimeoutException;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.vmm.vmware.api.VMStartTask;
import nl.bitbrains.nebu.vmm.vmware.api.vcloud.VCloud;
import nl.bitbrains.nebu.vmm.vmware.api.vsphere.VSphere;
import nl.bitbrains.nebu.vmm.vmware.converter.VirtualConverter;
import nl.bitbrains.nebu.vmm.vmware.entity.VirtualApplication;
import nl.bitbrains.nebu.vmm.vmware.entity.VmBootStatus;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;

public class TestVMStartTask {

    @Mock
    private VSphere vsphere;
    @Mock
    private VCloud vcloud;
    @Mock
    private VMStartTask startTask;
    @Mock
    private VmBootStatus progress;
    @Mock
    private VirtualApplication vapp;
    @Mock
    private VirtualMachine virtualMachine;
    @Mock
    private VM vm;
    @Mock
    private ReferenceType ref;

    private final String hostid = "hostid";
    private final String hostname = "hostname";
    private final String storeid = "storeid";
    private final String newVMId = "new-vm-id";
    private final String vmName = VirtualConverter.buildVsphereName(this.hostname, this.newVMId);

    @Before
    public void setUp() throws VCloudException, TimeoutException, NoSuchVMException {
        MockitoAnnotations.initMocks(this);
        Mockito.when(this.vm.getReference()).thenReturn(this.ref);
        Mockito.when(this.vcloud.createVM(Matchers.any(VirtualMachine.class),
                                          Matchers.any(VirtualApplication.class),
                                          Matchers.anyString())).thenReturn(this.newVMId);

        this.startTask = new VMStartTask.Builder().withHostId(this.hostid)
                .withHostname(this.hostname).withStoreId(this.storeid)
                .withTaskProgress(this.progress).withVCloud(this.vcloud)
                .withVirtualApplication(this.vapp).withVirtualMachine(this.virtualMachine)
                .withVSphere(this.vsphere).build();
    }

    @Test
    public void testTaskGetProgress() {
        final VmBootStatus status = this.startTask.getProgress();

        Assert.assertEquals(this.progress, status);
    }

    @Test
    public void testTaskRun() throws VCloudException, TimeoutException, NoSuchVMException,
            RemoteException, VMwareException, InterruptedException {
        this.startTask.syncRun();

        Mockito.verify(this.vcloud).createVM(this.virtualMachine, this.vapp, this.hostname);
        Mockito.verify(this.vsphere).moveVMToHost(this.vmName, this.hostid);
        Mockito.verify(this.vsphere).moveVMToStorage(this.vmName, this.storeid);
        Mockito.verify(this.vcloud).powerOnVM(Matchers.eq(this.newVMId));
    }
}
