package nl.bitbrains.nebu.vmm.vmware.api.vsphere;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.vmm.vmware.api.vsphere.VSphere;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.modules.junit4.PowerMockRunner;

import com.vmware.vim25.ArrayOfCheckResult;
import com.vmware.vim25.CheckResult;
import com.vmware.vim25.ClusterConfigInfoEx;
import com.vmware.vim25.ClusterDrsVmConfigInfo;
import com.vmware.vim25.ComputeResourceConfigSpec;
import com.vmware.vim25.DatastoreHostMount;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.HostHardwareSummary;
import com.vmware.vim25.HostListSummary;
import com.vmware.vim25.HostListSummaryQuickStats;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.InvalidState;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;
import com.vmware.vim25.NoActiveHostInCluster;
import com.vmware.vim25.NoPermission;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VirtualMachineSummary;
import com.vmware.vim25.mo.ClusterComputeResource;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.VirtualMachineProvisioningChecker;

@RunWith(PowerMockRunner.class)
public class TestVSphere {

    @Mock
    ServiceInstance instance;
    @Mock
    ResourcePool pool;
    @Mock
    ClusterComputeResource resource;
    @Mock
    Folder folder;
    @Mock
    Datacenter datacenter;
    @Mock
    HostSystem system;
    @Mock
    Datastore store;
    @Mock
    DatastoreSummary summary;
    @Mock
    VirtualMachineSummary vmSummary;
    @Mock
    VirtualMachineRuntimeInfo vmRuntimeInfo;
    @Mock
    ManagedObjectReference mor, mor2;
    @Mock
    DatastoreHostMount hostMount;
    @Mock
    InventoryNavigator navigator;
    @Mock
    VirtualMachine vm;
    @Mock
    HostSystem host;
    @Mock
    Task task;
    @Mock
    TaskInfo taskinfo;
    @Mock
    VirtualMachineProvisioningChecker provisioningChecker;
    @Mock
    ArrayOfCheckResult results;
    @Mock
    CheckResult result;
    @Mock
    PhysicalHost phost;
    @Mock
    HostListSummary hostSummary;
    @Mock
    HostHardwareSummary hardwareSummary;
    @Mock
    HostListSummaryQuickStats quickStats;
    @Mock
    ClusterConfigInfoEx configInfo;
    @Mock
    ClusterDrsVmConfigInfo drsVmConfigInfo;
    @Mock
    ArrayOfCheckResult arrayOfCheckResult;
    @Mock
    CheckResult checkResult;
    @Mock
    LocalizedMethodFault localizedMethodFault;
    @Mock
    MethodFault methodFault;
    @Mock
    ClusterDrsVmConfigInfo clusterDrsVmConfigInfo;

    String val = "val";
    HostSystem[] systems;
    Datastore[] stores;
    DatastoreHostMount[] hostMounts;
    CheckResult[] checkResults;
    LocalizedMethodFault[] localizedMethodFaults;
    ClusterDrsVmConfigInfo[] clusterDrsVmConfigInfos;

    private VSphere vsphere;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.systems = new HostSystem[1];
        this.systems[0] = this.system;
        this.stores = new Datastore[1];
        this.stores[0] = this.store;
        this.hostMounts = new DatastoreHostMount[1];
        this.hostMounts[0] = this.hostMount;
        this.checkResults = new CheckResult[1];
        this.checkResults[0] = this.checkResult;
        this.localizedMethodFaults = new LocalizedMethodFault[1];
        this.localizedMethodFaults[0] = this.localizedMethodFault;
        this.clusterDrsVmConfigInfos = new ClusterDrsVmConfigInfo[1];
        this.clusterDrsVmConfigInfos[0] = this.clusterDrsVmConfigInfo;
        this.vsphere = new VSphere();
        this.vsphere.setServiceInstance(this.instance);
        this.vsphere.setNavigator(this.navigator);
    }

    private void mockForTopology() throws InvalidProperty, RuntimeFault, RemoteException {
        Mockito.when(this.pool.getOwner()).thenReturn(this.resource);
        Mockito.when(this.resource.getParent()).thenReturn(this.datacenter);
        Mockito.when(this.resource.getHosts()).thenReturn(this.systems);
        Mockito.when(this.resource.getDatastores()).thenReturn(this.stores);
        Mockito.when(this.resource.getMOR()).thenReturn(this.mor);
        Mockito.when(this.datacenter.getMOR()).thenReturn(this.mor);
        Mockito.when(this.system.getMOR()).thenReturn(this.mor);
        Mockito.when(this.system.getSummary()).thenReturn(this.hostSummary);
        Mockito.when(this.hostSummary.getHardware()).thenReturn(this.hardwareSummary);
        Mockito.when(this.hostSummary.getQuickStats()).thenReturn(this.quickStats);
        Mockito.when(this.hardwareSummary.getNumCpuCores()).thenReturn((short) 1);
        Mockito.when(this.hardwareSummary.getCpuMhz()).thenReturn(1);
        Mockito.when(this.hardwareSummary.getMemorySize()).thenReturn((long) 1);
        Mockito.when(this.quickStats.getOverallCpuUsage()).thenReturn(1);
        Mockito.when(this.quickStats.getOverallMemoryUsage()).thenReturn(1);
        Mockito.when(this.store.getMOR()).thenReturn(this.mor);
        Mockito.when(this.store.getSummary()).thenReturn(this.summary);
        Mockito.when(this.mor.getVal()).thenReturn(this.val);
    }

    private void mockAddDiskToHost(final ManagedObjectReference mor) {
        Mockito.when(this.store.getHost()).thenReturn(this.hostMounts);
        Mockito.when(this.hostMount.getKey()).thenReturn(mor);
    }

    private void mockSearchItems(final String type, final ManagedEntity entity)
            throws InvalidProperty, RuntimeFault {
        Mockito.when(this.vsphere.searchItems(Matchers.eq(type), Matchers.anyString()))
                .thenReturn(entity);
    }

    private void mockSearchItems(final VirtualMachine vm, final ResourcePool pool,
            final ClusterComputeResource resource, final ManagedObjectReference mor,
            final String val) throws InvalidProperty, RuntimeFault, RemoteException {
        Mockito.when(vm.getResourcePool()).thenReturn(pool);
        Mockito.when(pool.getOwner()).thenReturn(resource);
        Mockito.when(resource.getMOR()).thenReturn(mor);
        Mockito.when(vm.getMOR()).thenReturn(mor);
        Mockito.when(mor.getVal()).thenReturn(val);
    }

    private void mockChecker(final ServiceInstance instance) throws NoActiveHostInCluster,
            InvalidState, RuntimeFault, RemoteException {
        Mockito.when(instance.getVirtualMachineProvisioningChecker())
                .thenReturn(this.provisioningChecker);
        Mockito.when(this.provisioningChecker.checkMigrate_Task(Matchers.any(VirtualMachine.class),
                                                                Matchers.any(HostSystem.class),
                                                                Matchers.any(ResourcePool.class),
                                                                Matchers.any(VirtualMachinePowerState.class),
                                                                Matchers.any(String[].class)))
                .thenReturn(this.task);
        Mockito.when(this.task.getTaskInfo()).thenReturn(this.taskinfo);
        Mockito.when(this.taskinfo.getResult()).thenReturn(this.results);
        Mockito.when(this.results.getCheckResult()).thenReturn(new CheckResult[] { this.result });
        Mockito.when(this.result.getError()).thenReturn(new LocalizedMethodFault[0]);
    }

    private void mockHostHasVapp(final String poolname, final String hostid) throws RemoteException {
        Mockito.when(this.folder.getParent()).thenReturn(this.folder);
        Mockito.when(this.folder.getName()).thenReturn(poolname);
        Mockito.when(this.pool.getOwner()).thenReturn(this.resource);
        Mockito.when(this.system.getMOR()).thenReturn(this.mor);
        Mockito.when(this.mor.getVal()).thenReturn(hostid);
        Mockito.when(this.phost.getUniqueIdentifier()).thenReturn(hostid);
        this.mockSearchItems(VSphere.TYPE_FOLDER, this.folder);
    }

    private void mockDRSUpdate(final ClusterComputeResource resource) throws RuntimeFault,
            RemoteException {
        Mockito.when(resource.getConfigurationEx()).thenReturn(this.configInfo);
        Mockito.when(resource.reconfigureComputeResource_Task(Matchers
                .any(ComputeResourceConfigSpec.class), Matchers.anyBoolean()))
                .thenReturn(this.task);
        Mockito.when(this.configInfo.getDrsVmConfig())
                .thenReturn(new ClusterDrsVmConfigInfo[] { this.drsVmConfigInfo });
        Mockito.when(this.drsVmConfigInfo.getKey()).thenReturn(this.mor);
    }

    private void mockVMMigrateCreatesTask() throws RemoteException {
        Mockito.when(this.vm.migrateVM_Task(Matchers.any(ResourcePool.class),
                                            Matchers.any(HostSystem.class),
                                            Matchers.any(VirtualMachineMovePriority.class),
                                            Matchers.any(VirtualMachinePowerState.class)))
                .thenReturn(this.task);
    }

    @Test(expected = MalformedURLException.class)
    public void testInitBadUrl() throws RemoteException, MalformedURLException {
        this.vsphere.init("", "", "not-an-url", 0);
    }

    @Test
    public void testClusterFromResPool() throws InvalidProperty, RuntimeFault, RemoteException {
        this.vsphere.getClusterFromResourcePool(this.pool);

        Mockito.verify(this.pool).getOwner();
    }

    @Test
    public void testDatacenterFromResource() {
        final Datacenter dc = this.vsphere.getDatacenterFromComputeResource(this.resource);

        Mockito.verify(this.resource).getParent();
        Assert.assertNull(dc);
    }

    @Test
    public void testDatacenterFromResourceParentNotDC() {
        Mockito.when(this.resource.getParent()).thenReturn(this.folder);
        final Datacenter dc = this.vsphere.getDatacenterFromComputeResource(this.resource);

        Mockito.verify(this.resource).getParent();
        Assert.assertNull(dc);
    }

    @Test
    public void testDatacenterFromResourcePoolRealParent() {
        Mockito.when(this.resource.getParent()).thenReturn(this.datacenter);
        final Datacenter dc = this.vsphere.getDatacenterFromComputeResource(this.resource);

        Assert.assertEquals(this.datacenter, dc);
    }

    @Test(expected = VMwareException.class)
    public void testTopologyFromPoolName() throws RemoteException, VMwareException {
        this.vsphere.getPhysicalTopologyFromResourcePoolName("");
    }

    @Test(expected = VMwareException.class)
    public void testTopologyFromPoolNameRemoteException() throws RemoteException, VMwareException {
        Mockito.when(this.instance.getRootFolder()).thenThrow(RemoteException.class);
        final PhysicalTopology topo = this.vsphere.getPhysicalTopologyFromResourcePoolName("");
        Assert.assertNull(topo);
    }

    @Test
    public void testTopologyFromPool() throws RemoteException {
        this.mockForTopology();
        this.summary.multipleHostAccess = true;

        final PhysicalTopology topo = this.vsphere.getPhysicalTopologyFromResourcePool(this.pool);

        Assert.assertNotNull(topo);
    }

    @Test
    public void testTopologyAddLocalDisk() throws RemoteException {
        this.mockForTopology();
        final String id1 = "id1";
        Mockito.when(this.mor2.getVal()).thenReturn(id1);
        Mockito.when(this.mor.getVal()).thenReturn(id1);
        this.mockAddDiskToHost(this.mor2);

        this.summary.multipleHostAccess = false;
        final PhysicalTopology topo = this.vsphere.getPhysicalTopologyFromResourcePool(this.pool);

        final PhysicalStore pStore = topo.getCPUs().get(0).getDisks().get(0);
        Assert.assertEquals(1, topo.getCPUs().size());
        Assert.assertTrue(pStore.getUniqueIdentifier().equals(this.store.getMOR().getVal()));
    }

    @Test
    public void testTopologySkipLocalDisk() throws RemoteException {
        this.mockForTopology();
        final String id1 = "id1";
        final String id2 = "id2";
        Mockito.when(this.mor2.getVal()).thenReturn(id1);
        Mockito.when(this.mor.getVal()).thenReturn(id2);
        this.mockAddDiskToHost(this.mor2);

        this.summary.multipleHostAccess = false;
        final PhysicalTopology topo = this.vsphere.getPhysicalTopologyFromResourcePool(this.pool);

        Assert.assertEquals(1, topo.getCPUs().size());
        Assert.assertTrue(topo.getCPUs().get(0).getDisks().isEmpty());
    }

    @Test
    public void testTopologySkipLocalNullDisk() throws RemoteException {
        this.mockForTopology();
        final DatastoreHostMount[] nullHosts = new DatastoreHostMount[1];
        Mockito.when(this.store.getHost()).thenReturn(nullHosts);

        this.summary.multipleHostAccess = false;
        final PhysicalTopology topo = this.vsphere.getPhysicalTopologyFromResourcePool(this.pool);

        Assert.assertEquals(1, topo.getCPUs().size());
        Assert.assertTrue(topo.getCPUs().get(0).getDisks().isEmpty());
    }

    @Test(expected = VMwareException.class)
    public void testGetTopologyFromVappName() throws VMwareException {
        this.vsphere.getPhysicalTopologyFromVapp("");
    }

    @Test(expected = VMwareException.class)
    public void testGetTopologyFromVapps() throws VMwareException {
        final List<String> vappnames = new ArrayList<String>();
        vappnames.add("");
        this.vsphere.getPhysicalTopologyFromVappList(vappnames);
    }

    @Test
    public void testSearchItemsByIdDatastore() {
        final String type = VSphere.TYPE_DATASTORE;
        final String id = "test";
        final ManagedEntity entity = this.vsphere.searchItemsById(type, id);

        Assert.assertEquals(id, entity.getMOR().getVal());
        Assert.assertEquals(Datastore.class, entity.getClass());
    }

    @Test
    public void testSearchItemsByIdHost() {
        final String type = VSphere.TYPE_HOST;
        final String id = "test";
        final ManagedEntity entity = this.vsphere.searchItemsById(type, id);

        Assert.assertEquals(id, entity.getMOR().getVal());
        Assert.assertEquals(HostSystem.class, entity.getClass());
    }

    @Test
    public void testSearchItemsByIdFolder() {
        final String type = VSphere.TYPE_FOLDER;
        final String id = "test";
        final ManagedEntity entity = this.vsphere.searchItemsById(type, id);

        Assert.assertEquals(id, entity.getMOR().getVal());
        Assert.assertEquals(Folder.class, entity.getClass());
    }

    @Test
    public void testSearchItemsByIdRespool() {
        final String type = VSphere.TYPE_RESOURCEPOOL;
        final String id = "test";
        final ManagedEntity entity = this.vsphere.searchItemsById(type, id);

        Assert.assertEquals(id, entity.getMOR().getVal());
        Assert.assertEquals(ResourcePool.class, entity.getClass());
    }

    @Test(expected = VMwareException.class)
    public void testMoveVMToHostNotFound() throws RemoteException, VMwareException,
            InterruptedException {
        this.mockSearchItems(VSphere.TYPE_VM, null);
        this.mockSearchItems(VSphere.TYPE_HOST, null);
        final String vmname = "vmname";
        final String hostid = "hostid";

        this.vsphere.moveVMToHost(vmname, hostid);
    }

    @Test(expected = VMwareException.class)
    public void testMoveVMToHostNoCluster() throws RemoteException, VMwareException,
            InterruptedException {
        this.mockSearchItems(VSphere.TYPE_VM, this.vm);
        this.mockSearchItems(VSphere.TYPE_HOST, this.host);
        this.mockSearchItems(this.vm, this.pool, this.resource, this.mor, this.val);
        Mockito.when(this.pool.getOwner()).thenReturn(null);
        final String vmname = "vmname";
        final String hostid = "hostid";

        this.vsphere.moveVMToHost(vmname, hostid);
    }

    @Test(expected = VMwareException.class)
    public void testMoveVMToHostNoTask() throws RemoteException, VMwareException,
            InterruptedException {
        this.mockSearchItems(VSphere.TYPE_VM, this.vm);
        this.mockSearchItems(VSphere.TYPE_HOST, this.host);
        this.mockSearchItems(this.vm, this.pool, this.resource, this.mor, this.val);
        this.mockDRSUpdate(this.resource);
        this.mockChecker(this.instance);
        this.mockVMMigrateCreatesTask();
        Mockito.when(this.vm.migrateVM_Task(Matchers.any(ResourcePool.class),
                                            Matchers.any(HostSystem.class),
                                            Matchers.any(VirtualMachineMovePriority.class),
                                            Matchers.any(VirtualMachinePowerState.class)))
                .thenReturn(null);
        final String vmname = "vmname";
        final String hostid = "hostid";

        this.vsphere.moveVMToHost(vmname, hostid);
    }

    @Test(expected = VMwareException.class)
    public void testMoveVMToHostException() throws RemoteException, VMwareException,
            InterruptedException {
        this.mockSearchItems(VSphere.TYPE_VM, this.vm);
        this.mockSearchItems(VSphere.TYPE_HOST, this.host);
        this.mockSearchItems(this.vm, this.pool, this.resource, this.mor, this.val);
        this.mockDRSUpdate(this.resource);
        this.mockChecker(this.instance);
        this.mockVMMigrateCreatesTask();
        Mockito.when(this.task.waitForTask()).thenThrow(new RemoteException());
        final String vmname = "vmname";
        final String hostid = "hostid";

        this.vsphere.moveVMToHost(vmname, hostid);
    }

    @Test
    public void testMoveVMToHost() throws RemoteException, VMwareException, InterruptedException {
        this.mockSearchItems(VSphere.TYPE_VM, this.vm);
        this.mockSearchItems(VSphere.TYPE_HOST, this.host);
        this.mockSearchItems(this.vm, this.pool, this.resource, this.mor, this.val);
        this.mockDRSUpdate(this.resource);
        this.mockChecker(this.instance);
        this.mockVMMigrateCreatesTask();
        final String vmname = "vmname";
        final String hostid = "hostid";

        this.vsphere.moveVMToHost(vmname, hostid);
    }

    @Test(expected = NoSuchVMException.class)
    public void testGetVirtualMachineHostInvalidName() throws RemoteException, NoSuchVMException {
        final String vmname = "vm";
        this.vsphere.getVirtualMachineHost(vmname);
    }

    @Test
    public void testGetVirtualMachineHostCorrectName() throws RemoteException, NoSuchVMException {
        final String hostid = "hostid";
        this.mockSearchItems(VSphere.TYPE_VM, this.vm);
        Mockito.when(this.vm.getSummary()).thenReturn(this.vmSummary);
        Mockito.when(this.vmSummary.getRuntime()).thenReturn(this.vmRuntimeInfo);
        Mockito.when(this.vmRuntimeInfo.getHost()).thenReturn(this.mor);
        Mockito.when(this.mor.getVal()).thenReturn(hostid);

        final String vmname = "vm";
        final String id = this.vsphere.getVirtualMachineHost(vmname);

        Assert.assertEquals(hostid, id);
    }

    @Test(expected = NoSuchVMException.class)
    public void testGetVirtualMachineStoresInvalidName() throws RemoteException, NoSuchVMException {
        final String vmname = "vm";
        this.vsphere.getVirtualMachineStores(vmname);
    }

    @Test
    public void testGetVirtualMachineStoresCorrectname() throws RemoteException, NoSuchVMException {
        final String storeid = "storeid";
        this.mockSearchItems(VSphere.TYPE_VM, this.vm);
        Mockito.when(this.vm.getDatastores()).thenReturn(this.stores);
        Mockito.when(this.store.getMOR()).thenReturn(this.mor);
        Mockito.when(this.mor.getVal()).thenReturn(storeid);

        final String vmname = "vm";
        final List<String> storeids = this.vsphere.getVirtualMachineStores(vmname);

        Assert.assertFalse(storeids.isEmpty());
        Assert.assertEquals(storeid, storeids.get(0));
    }

    @Test
    public void testHostHasVappInvalidVappName() throws InvalidProperty, RuntimeFault {
        final String vappname = "vappname";

        final boolean res = this.vsphere.hostHasVapp(this.phost, vappname);

        Assert.assertFalse(res);
    }

    @Test
    public void testHostHasVappValidVappName() throws RemoteException {
        final String poolname = "poolname";
        final String hostid = "poolid";
        final String vappname = "vappname";
        this.mockHostHasVapp(poolname, hostid);

        Mockito.when(this.resource.getHosts()).thenReturn(this.systems);
        this.mockSearchItems(VSphere.TYPE_RESOURCEPOOL, this.pool);

        final boolean res = this.vsphere.hostHasVapp(this.phost, vappname);

        Assert.assertTrue(res);
    }

    @Test
    public void testHostHasVappValidNoPool() throws RemoteException {
        final String poolname = "poolname";
        final String hostid = "poolid";
        final String vappname = "vappname";
        this.mockHostHasVapp(poolname, hostid);

        Mockito.when(this.resource.getHosts()).thenReturn(this.systems);

        final boolean res = this.vsphere.hostHasVapp(this.phost, vappname);

        Assert.assertFalse(res);
    }

    @Test
    public void testHostHasVappValidNoHosts() throws RemoteException {
        final String poolname = "poolname";
        final String hostid = "poolid";
        final String vappname = "vappname";
        this.mockHostHasVapp(poolname, hostid);

        Mockito.when(this.resource.getHosts()).thenReturn(new HostSystem[0]);
        this.mockSearchItems(VSphere.TYPE_RESOURCEPOOL, this.pool);

        final boolean res = this.vsphere.hostHasVapp(this.phost, vappname);

        Assert.assertFalse(res);
    }

    private void mockForGetPhysicalTopologyFromVapp() throws RemoteException {
        Mockito.when(this.navigator.searchManagedEntity(Matchers.eq(VSphere.TYPE_FOLDER),
                                                        Matchers.anyString()))
                .thenReturn(this.folder);
        Mockito.when(this.navigator.searchManagedEntity(Matchers.eq(VSphere.TYPE_RESOURCEPOOL),
                                                        Matchers.anyString()))
                .thenReturn(this.pool);
        Mockito.when(this.folder.getParent()).thenReturn(this.pool);
        Mockito.when(this.store.getSummary()).thenReturn(this.summary);
        this.summary.multipleHostAccess = true;
        this.mockForTopology();
    }

    @Test
    public void testGetPhysicalTopologyFromVapp() throws VMwareException, InvalidProperty,
            RuntimeFault, RemoteException {
        final String vappname = "vapp";

        this.mockForGetPhysicalTopologyFromVapp();

        this.vsphere.setNavigator(this.navigator);
        Assert.assertNotNull(this.vsphere.getPhysicalTopologyFromVapp(vappname));
    }

    @Test(expected = VMwareException.class)
    public void testGetPhysicalTopologyFromVappReturnsDifferentEntity() throws VMwareException,
            InvalidProperty, RuntimeFault, RemoteException {
        final String vappname = "vapp";

        this.mockForGetPhysicalTopologyFromVapp();
        Mockito.when(this.navigator.searchManagedEntity(Matchers.eq(VSphere.TYPE_FOLDER),
                                                        Matchers.anyString()))
                .thenReturn(this.pool);

        this.vsphere.setNavigator(this.navigator);
        this.vsphere.getPhysicalTopologyFromVapp(vappname);
    }

    @Test(expected = VMwareException.class)
    public void testGetPhysicalTopologyFromVappCannotFindFolder() throws VMwareException,
            InvalidProperty, RuntimeFault, RemoteException {
        final String vappname = "vapp";

        this.mockForGetPhysicalTopologyFromVapp();

        Mockito.when(this.navigator.searchManagedEntity(Matchers.anyString(), Matchers.anyString()))
                .thenThrow(new InvalidProperty());

        this.vsphere.setNavigator(this.navigator);
        this.vsphere.getPhysicalTopologyFromVapp(vappname);
    }

    @Test(expected = VMwareException.class)
    public void testGetPhysicalTopologyFromVappCannotGetTopologyFromVapp() throws VMwareException,
            InvalidProperty, RuntimeFault, RemoteException {
        final String vappname = "vapp";

        this.mockForGetPhysicalTopologyFromVapp();

        Mockito.when(this.pool.getOwner()).thenThrow(new RemoteException());

        this.vsphere.setNavigator(this.navigator);
        this.vsphere.getPhysicalTopologyFromVapp(vappname);
    }

    @Test
    public void testGetPhysicalTopologyFromVappListOneVapp() throws RemoteException,
            VMwareException {
        final String vappname = "vappname";
        final List<String> vappnames = new ArrayList<String>(1);
        vappnames.add(vappname);

        this.mockForGetPhysicalTopologyFromVapp();

        Assert.assertNotNull(this.vsphere.getPhysicalTopologyFromVappList(vappnames));
    }

    @Test
    public void testGetServiceInstance() {
        this.vsphere.setServiceInstance(this.instance);

        Assert.assertEquals(this.instance, this.vsphere.getServiceInstance());
    }

    private void mockForMoveVMToStorage() throws InvalidState, RuntimeFault, RemoteException {
        this.vsphere.setServiceInstance(this.instance);

        this.mockSearchItems(VSphere.TYPE_VM, this.vm);
        this.mockSearchItems(VSphere.TYPE_DATASTORE, this.store);

        Mockito.when(this.instance.getVirtualMachineProvisioningChecker())
                .thenReturn(this.provisioningChecker);
        Mockito.when(this.provisioningChecker.checkRelocate_Task(Matchers.any(VirtualMachine.class),
                                                                 Matchers.any(VirtualMachineRelocateSpec.class),
                                                                 Matchers.any(String[].class)))
                .thenReturn(this.task);
        Mockito.when(this.task.getTaskInfo()).thenReturn(this.taskinfo);
        Mockito.when(this.taskinfo.getResult()).thenReturn(this.arrayOfCheckResult);
        Mockito.when(this.arrayOfCheckResult.getCheckResult()).thenReturn(this.checkResults);
        Mockito.when(this.checkResult.getError()).thenReturn(null);
    }

    @Test
    public void testMoveVMToStorage() throws RemoteException, VMwareException, InterruptedException {
        final String vmName = "vmname";
        final String datastoreId = "storeId";

        this.mockForMoveVMToStorage();

        this.vsphere.moveVMToStorage(vmName, datastoreId);
    }

    @Test(expected = VMwareException.class)
    public void testMoveVMToStorageTaskError() throws RemoteException, VMwareException,
            InterruptedException {
        final String vmName = "vmname";
        final String datastoreId = "storeId";

        this.mockForMoveVMToStorage();
        Mockito.when(this.checkResult.getError()).thenReturn(this.localizedMethodFaults);
        Mockito.when(this.localizedMethodFault.getFault()).thenReturn(this.methodFault);

        this.vsphere.moveVMToStorage(vmName, datastoreId);
    }

    @Test(expected = VMwareException.class)
    public void testMoveVMToStorageTaskWaitFails() throws RemoteException, VMwareException,
            InterruptedException {
        final String vmName = "vmname";
        final String datastoreId = "storeId";

        this.mockForMoveVMToStorage();
        Mockito.when(this.task.waitForTask()).thenThrow(new RemoteException());

        this.vsphere.moveVMToStorage(vmName, datastoreId);
    }

    @Test(expected = VMwareException.class)
    public void testMoveVMToStorageResultNoArrayOfCheckResult() throws RemoteException,
            VMwareException, InterruptedException {
        final String vmName = "vmname";
        final String datastoreId = "storeId";

        this.mockForMoveVMToStorage();
        Mockito.when(this.taskinfo.getResult()).thenReturn(this.checkResult);

        this.vsphere.moveVMToStorage(vmName, datastoreId);
    }

    @Test(expected = VMwareException.class)
    public void testMoveVMToStorageCannotFindVM() throws RemoteException, VMwareException,
            InterruptedException {
        final String vmName = "vmname";
        final String datastoreId = "storeId";

        this.mockForMoveVMToStorage();
        this.mockSearchItems(VSphere.TYPE_VM, this.folder);

        this.vsphere.moveVMToStorage(vmName, datastoreId);
    }

    @Test
    public void testMoveVMToStorageWaitForTask() throws RemoteException, VMwareException,
            InterruptedException {
        final String vmName = "vmname";
        final String datastoreId = "storeId";

        this.mockForMoveVMToStorage();
        Mockito.when(this.vm.relocateVM_Task(Matchers.any(VirtualMachineRelocateSpec.class)))
                .thenReturn(this.task);

        this.vsphere.moveVMToStorage(vmName, datastoreId);
    }

    private void mockForDisableDRS() throws RuntimeFault, RemoteException {
        Mockito.when(this.vm.getMOR()).thenReturn(this.mor);
        Mockito.when(this.resource.getMOR()).thenReturn(this.mor);

        Mockito.when(this.resource.getConfigurationEx()).thenReturn(this.configInfo);
        Mockito.when(this.configInfo.getDrsVmConfig()).thenReturn(this.clusterDrsVmConfigInfos);
        Mockito.when(this.clusterDrsVmConfigInfo.getKey()).thenReturn(this.mor2);
        Mockito.when(this.mor2.getVal()).thenReturn(this.val);

        Mockito.when(this.resource.reconfigureComputeResource_Task(Matchers
                .any(ComputeResourceConfigSpec.class), Matchers.anyBoolean()))
                .thenReturn(this.task);
    }

    @Test
    public void testDisableDRS() throws RemoteException {
        this.mockForDisableDRS();

        this.vsphere.disableDRS(this.vm, this.resource);
    }

    @Test
    public void testDisableDRSExistingInfo() throws RemoteException {
        final String val = "val";

        this.mockForDisableDRS();

        Mockito.when(this.clusterDrsVmConfigInfo.getKey()).thenReturn(this.mor);
        Mockito.when(this.mor.getVal()).thenReturn(val);

        this.vsphere.disableDRS(this.vm, this.resource);
    }

    @Test
    public void testDisableDRSNoPermission() throws RemoteException {
        final String val = "val";

        this.mockForDisableDRS();

        Mockito.when(this.resource.reconfigureComputeResource_Task(Matchers
                .any(ComputeResourceConfigSpec.class), Matchers.anyBoolean()))
                .thenThrow(new NoPermission());

        this.vsphere.disableDRS(this.vm, this.resource);
    }

    @Test
    public void testDisableDRSRemoteException() throws RemoteException {
        final String val = "val";

        this.mockForDisableDRS();

        Mockito.when(this.resource.reconfigureComputeResource_Task(Matchers
                .any(ComputeResourceConfigSpec.class), Matchers.anyBoolean()))
                .thenThrow(new RemoteException());

        this.vsphere.disableDRS(this.vm, this.resource);
    }
}
