package nl.bitbrains.nebu.vmm.vmware.api;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.vmm.vmware.api.DefaultVMware;
import nl.bitbrains.nebu.vmm.vmware.api.vcloud.VCloud;
import nl.bitbrains.nebu.vmm.vmware.api.vsphere.VSphere;
import nl.bitbrains.nebu.vmm.vmware.converter.PhysicalResourceConverter;
import nl.bitbrains.nebu.vmm.vmware.entity.VirtualApplication;
import nl.bitbrains.nebu.vmm.vmware.entity.VmBootStatus;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMLaunchException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;

import org.junit.Assert;
import org.junit.Before;
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

import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.HostSystem;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PhysicalResourceConverter.class)
@PowerMockIgnore({ "javax.management.*" })
public class TestDefaultVMware {

    @Mock
    VSphere vsphere;
    @Mock
    VCloud vcloud;
    @Mock
    Datastore store;
    @Mock
    PhysicalStore pStore;
    @Mock
    HostSystem host;
    @Mock
    PhysicalHost pHost;
    @Mock
    VirtualMachine vm;
    @Mock
    VirtualApplication vapp;

    private DefaultVMware vmware;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(PhysicalResourceConverter.class);
        this.vmware = new DefaultVMware();
        this.vmware.setVcloud(this.vcloud);
        this.vmware.setVsphere(this.vsphere);
    }

    @Test
    public void testGetPhysicalTopologyFromResourcePool() throws VMwareException {
        final String poolname = "poolname";
        this.vmware.getPhysicalTopologyFromResourcePool(poolname);
    }

    @Test(expected = VMwareException.class)
    public void testGetPhysicalTopologyFromResourcePoolThrowsException() throws VMwareException,
            RemoteException {
        final String poolname = "poolname";
        Mockito.when(this.vsphere.getPhysicalTopologyFromResourcePoolName(Matchers.eq(poolname)))
                .thenThrow(new RemoteException());
        this.vmware.getPhysicalTopologyFromResourcePool(poolname);
    }

    @Test
    public void testGetStoreInfo() throws VMwareException {
        final String uuid = "uuid";
        Mockito.when(this.vsphere.searchItemsById(Matchers.eq(VSphere.TYPE_DATASTORE),
                                                  Matchers.eq(uuid))).thenReturn(this.store);
        PowerMockito.mockStatic(PhysicalResourceConverter.class);
        PowerMockito.when(PhysicalResourceConverter.toPhysicalStore(Matchers.eq(this.store)))
                .thenReturn(this.pStore);
        final PhysicalStore resStore = this.vmware.getStoreInfo(uuid);

        Assert.assertEquals(this.pStore, resStore);
    }

    @Test(expected = VMwareException.class)
    public void testGetStoreInfoThrowsException() throws VMwareException {
        final String uuid = "uuid";
        this.vmware.getStoreInfo(uuid);
    }

    @Test
    public void testGetHostInfo() throws VMwareException {
        final String uuid = "uuid";
        Mockito.when(this.vsphere.searchItemsById(Matchers.eq(VSphere.TYPE_HOST), Matchers.eq(uuid)))
                .thenReturn(this.host);
        PowerMockito.when(PhysicalResourceConverter.toPhysicalHost(Matchers.eq(this.host),
                                                                   Matchers.anyBoolean()))
                .thenReturn(this.pHost);
        final PhysicalHost resHost = this.vmware.getHostInfo(uuid);

        Assert.assertEquals(this.pHost, resHost);
    }

    @Test(expected = VMwareException.class)
    public void testGetHostInfoThrowsException() throws VMwareException {
        final String uuid = "uuid";
        this.vmware.getHostInfo(uuid);
    }

    @Test
    public void testGetAllVapps() throws VMwareException {
        final List<VirtualApplication> apps = this.vmware.getAllVapps();

        Assert.assertTrue(apps.isEmpty());
    }

    @Test(expected = VMwareException.class)
    public void testGetAllVappsThrowsException() throws VMwareException, VCloudException {
        Mockito.when(this.vcloud.getAllVapps()).thenThrow(new VCloudException(""));
        this.vmware.getAllVapps();
    }

    @Test
    public void testGetVirtualResourceList() throws VMwareException {
        final List<String> virtualResources = this.vmware.getVirtualResourceList();

        Assert.assertTrue(virtualResources.isEmpty());
    }

    @Test(expected = VMwareException.class)
    public void testGetVirtualResourceListThrowsException() throws VMwareException {
        Mockito.when(this.vmware.getVirtualResourceList()).thenThrow(new VCloudException(""));
        this.vmware.getVirtualResourceList();
    }

    @Test
    public void testGetVmIdsFromNames() throws NoSuchVMException, VMwareException, VCloudException {
        final List<String> vmIds = new ArrayList<String>();
        final List<String> vmNames = new ArrayList<String>();

        this.vmware.getVmIdsFromNames(vmIds, vmNames);

        Mockito.verify(this.vcloud).getVmIdsFromNames(Matchers.eq(vmIds), Matchers.eq(vmNames));
    }

    @Test(expected = VMwareException.class)
    public void testGetVmIdsFromNamesVCloudException() throws NoSuchVMException, VCloudException,
            VMwareException {
        final List<String> vmIds = new ArrayList<String>();
        final List<String> vmNames = new ArrayList<String>();

        Mockito.when(this.vcloud.getVmIdsFromNames(Matchers.eq(vmIds), Matchers.eq(vmNames)))
                .thenThrow(new VCloudException(""));

        this.vmware.getVmIdsFromNames(vmIds, vmNames);
    }

    @Test(expected = NoSuchVMException.class)
    public void testGetVmIdsFromNamesNoSuchVMException() throws NoSuchVMException, VCloudException,
            VMwareException {
        final List<String> vmIds = new ArrayList<String>();
        final List<String> vmNames = new ArrayList<String>();

        Mockito.when(this.vcloud.getVmIdsFromNames(Matchers.eq(vmIds), Matchers.eq(vmNames)))
                .thenThrow(new NoSuchVMException(""));

        this.vmware.getVmIdsFromNames(vmIds, vmNames);
    }

    @Test
    public void testGetVmIdFromName() throws NoSuchVMException, VMwareException, VCloudException {
        final List<String> vmIds = new ArrayList<String>();
        final String vmName = "vmname";

        this.vmware.getVmIdFromName(vmIds, vmName);

        Mockito.verify(this.vcloud).getVmIdFromName(Matchers.eq(vmIds), Matchers.eq(vmName));
    }

    @Test(expected = VMwareException.class)
    public void testGetVmIdFromNameVCloudException() throws NoSuchVMException, VMwareException,
            VCloudException {
        final List<String> vmIds = new ArrayList<String>();
        final String vmName = "vmname";

        Mockito.when(this.vcloud.getVmIdFromName(Matchers.eq(vmIds), Matchers.eq(vmName)))
                .thenThrow(new VCloudException(""));

        this.vmware.getVmIdFromName(vmIds, vmName);
    }

    @Test
    public void testGetVirtualMachineInfo() throws VMwareException, NoSuchVMException,
            VCloudException {
        final String uuid = "uuid";

        Mockito.when(this.vcloud.getVirtualMachineInfo(Matchers.eq(uuid))).thenReturn(this.vm);

        final VirtualMachine resVM = this.vmware.getVirtualMachineInfo(uuid);

        Assert.assertEquals(this.vm, resVM);
    }

    @Test(expected = VMwareException.class)
    public void testGetVirtualMachineInfoVCloudException() throws VMwareException,
            NoSuchVMException, VCloudException {
        final String uuid = "uuid";

        Mockito.when(this.vcloud.getVirtualMachineInfo(Matchers.eq(uuid)))
                .thenThrow(new VCloudException(""));

        this.vmware.getVirtualMachineInfo(uuid);
    }

    @Test
    public void testGetVirtualMachineInfoWithStores() throws VMwareException, NoSuchVMException,
            VCloudException, RemoteException {
        final String uuid = "uuid";
        final String stringStore = "store";
        final List<String> stores = new ArrayList<String>(1);
        stores.add(stringStore);

        Mockito.when(this.vcloud.getVirtualMachineInfo(Matchers.eq(uuid))).thenReturn(this.vm);
        Mockito.when(this.vsphere.getVirtualMachineStores(Matchers.anyString())).thenReturn(stores);

        this.vmware.getVirtualMachineInfo(uuid);

        Mockito.verify(this.vm).addStore(Matchers.eq(stringStore));
    }

    @Test
    public void testGetPhysicalTopologyFromVappsNoVapps() throws VMwareException {
        final List<String> vAppIds = new ArrayList<String>();
        final PhysicalTopology topology = this.vmware.getPhysicalTopologyForVapps(vAppIds);

        Assert.assertNull(topology);
    }

    @Test(expected = VMwareException.class)
    public void testGetPhysicalTopologyFromVappsVCloudException() throws VMwareException,
            VCloudException {
        final List<String> vAppIds = new ArrayList<String>();

        Mockito.when(this.vcloud.getAllVapps()).thenThrow(new VCloudException(""));

        this.vmware.getPhysicalTopologyForVapps(vAppIds);
    }

    @Test
    public void testGetPhysicalTopologyFromVappsOneVapp() throws VMwareException, VCloudException {
        final String vappid = "id";
        final List<String> vAppIds = new ArrayList<String>();
        vAppIds.add(vappid);
        final List<VirtualApplication> vapps = new ArrayList<VirtualApplication>(1);
        vapps.add(this.vapp);

        Mockito.when(this.vapp.getUniqueIdentifier()).thenReturn(vappid);
        Mockito.when(this.vcloud.getAllVapps()).thenReturn(vapps);

        this.vmware.getPhysicalTopologyForVapps(vAppIds);

        Mockito.verify(this.vsphere)
                .getPhysicalTopologyFromVappList(Matchers.anyListOf(String.class));
    }

    @Test
    public void testGetPhysicalTopologyFromVappsOneVappWrongId() throws VMwareException,
            VCloudException {
        final String vappid = "id";
        final String otherVappid = "id2";
        final List<String> vAppIds = new ArrayList<String>();
        vAppIds.add(vappid);
        final List<VirtualApplication> vapps = new ArrayList<VirtualApplication>(1);
        vapps.add(this.vapp);

        Mockito.when(this.vapp.getUniqueIdentifier()).thenReturn(otherVappid);
        Mockito.when(this.vcloud.getAllVapps()).thenReturn(vapps);

        this.vmware.getPhysicalTopologyForVapps(vAppIds);

        Mockito.verify(this.vsphere)
                .getPhysicalTopologyFromVappList(Matchers.anyListOf(String.class));
    }

    @Test
    public void testCreateVM() throws VMLaunchException {
        final String hostId = "hostId";
        final String hostname = "hostname";
        final String uuid = "uuid";

        Mockito.when(this.vm.getUniqueIdentifier()).thenReturn(uuid);
        Mockito.when(this.vm.getHostname()).thenReturn(hostname);

        final VmBootStatus status = this.vmware.createVM(this.vm, this.vapp, hostId, hostname);

        Assert.assertNotNull(status);
    }

    @Test
    public void testCreateVMWithStore() throws VMLaunchException {
        final String hostId = "hostId";
        final String storageId = "storeid";
        final String hostname = "hostname";
        final String uuid = "uuid";

        Mockito.when(this.vm.getUniqueIdentifier()).thenReturn(uuid);
        Mockito.when(this.vm.getHostname()).thenReturn(hostname);

        final VmBootStatus status = this.vmware.createVM(this.vm,
                                                         this.vapp,
                                                         hostId,
                                                         hostname,
                                                         storageId);

        Assert.assertNotNull(status);
    }

    @Test
    public void testSelectVirtualApplicationFromHostNoVapps() {
        final List<String> possibleVapps = new ArrayList<String>();
        final VirtualApplication vapp = this.vmware.selectVirtualApplicationFromHost(this.pHost,
                                                                                     possibleVapps);

        Assert.assertNull(vapp);
    }

    @Test
    public void testSelectVirtualApplicationFromHostException() throws VCloudException {
        final List<String> possibleVapps = new ArrayList<String>();

        Mockito.when(this.vcloud.getAllVapps()).thenThrow(new VCloudException(""));

        final VirtualApplication vapp = this.vmware.selectVirtualApplicationFromHost(this.pHost,
                                                                                     possibleVapps);

        Assert.assertNull(vapp);
    }

    @Test
    public void testSelectVirtualApplicationFromHostOneVappNotOnHost() throws VCloudException {
        final String vappId = "id";
        final List<String> possibleVapps = new ArrayList<String>();
        final List<VirtualApplication> vapps = new ArrayList<VirtualApplication>();
        possibleVapps.add(vappId);
        vapps.add(this.vapp);

        Mockito.when(this.vapp.getUniqueIdentifier()).thenReturn(vappId);
        Mockito.when(this.vcloud.getAllVapps()).thenReturn(vapps);

        final VirtualApplication vapp = this.vmware.selectVirtualApplicationFromHost(this.pHost,
                                                                                     possibleVapps);

        Assert.assertNull(vapp);
    }

    @Test
    public void testSelectVirtualApplicationFromHostOneVapp() throws VCloudException {
        final String vappId = "id";
        final List<String> possibleVapps = new ArrayList<String>();
        final List<VirtualApplication> vapps = new ArrayList<VirtualApplication>();
        possibleVapps.add(vappId);
        vapps.add(this.vapp);

        Mockito.when(this.vapp.getUniqueIdentifier()).thenReturn(vappId);
        Mockito.when(this.vcloud.getAllVapps()).thenReturn(vapps);
        Mockito.when(this.vsphere.hostHasVapp(Matchers.any(PhysicalHost.class),
                                              Matchers.anyString())).thenReturn(true);

        final VirtualApplication vapp = this.vmware.selectVirtualApplicationFromHost(this.pHost,
                                                                                     possibleVapps);

        Assert.assertEquals(this.vapp, vapp);
    }

    @Test
    public void testGetVCloudNull() {
        this.vmware.setVcloud(null);
        Assert.assertNull(this.vmware.getVcloud());
    }

    @Test
    public void testGetVCloud() {
        this.vmware.setVcloud(this.vcloud);
        Assert.assertEquals(this.vcloud, this.vmware.getVcloud());
    }

    @Test
    public void testGetVSphereNull() {
        this.vmware.setVsphere(null);
        Assert.assertNull(this.vmware.getVsphere());
    }

    @Test
    public void testGetVSphere() {
        this.vmware.setVsphere(this.vsphere);
        Assert.assertEquals(this.vsphere, this.vmware.getVsphere());
    }

    @Test
    public void testKillVM() {
        this.vmware.killVM(this.vm);
    }

    @Test
    public void testMoveVMToHost() throws VMwareException {
        final String hostname = "hostname";
        final String uuid = "uuid";

        Mockito.when(this.vm.getHostname()).thenReturn(hostname);
        Mockito.when(this.vm.getUniqueIdentifier()).thenReturn(uuid);

        this.vmware.moveVMToHost(this.vm, this.pHost);
    }

    @Test(expected = VMwareException.class)
    public void testMoveVMToHostThrowsException() throws VMwareException, RemoteException,
            InterruptedException {
        final String hostname = "hostname";
        final String uuid = "uuid";

        Mockito.when(this.vm.getHostname()).thenReturn(hostname);
        Mockito.when(this.vm.getUniqueIdentifier()).thenReturn(uuid);

        Mockito.doThrow(new RemoteException()).when(this.vsphere)
                .moveVMToHost(Matchers.anyString(), Matchers.anyString());

        this.vmware.moveVMToHost(this.vm, this.pHost);
    }

    @Test
    public void testMoveVMToStore() throws VMwareException {
        final String hostname = "hostname";
        final String uuid = "uuid";

        Mockito.when(this.vm.getHostname()).thenReturn(hostname);
        Mockito.when(this.vm.getUniqueIdentifier()).thenReturn(uuid);

        this.vmware.moveVMToStore(this.vm, this.pStore);
    }

    @Test(expected = VMwareException.class)
    public void testMoveVMToStoreThrowsException() throws VMwareException, RemoteException,
            InterruptedException {
        final String hostname = "hostname";
        final String uuid = "uuid";

        Mockito.when(this.vm.getHostname()).thenReturn(hostname);
        Mockito.when(this.vm.getUniqueIdentifier()).thenReturn(uuid);

        Mockito.doThrow(new RemoteException()).when(this.vsphere)
                .moveVMToStorage(Matchers.anyString(), Matchers.anyString());

        this.vmware.moveVMToStore(this.vm, this.pStore);
    }

}
