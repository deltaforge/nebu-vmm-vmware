package nl.bitbrains.nebu.vmm.vmware.api.vsphere;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.bitbrains.nebu.common.topology.PhysicalDataCenter;
import nl.bitbrains.nebu.common.topology.PhysicalDataCenterBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalRack;
import nl.bitbrains.nebu.common.topology.PhysicalRackBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalRoot;
import nl.bitbrains.nebu.common.topology.PhysicalRootBuilder;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.common.util.ErrorChecker;
import nl.bitbrains.nebu.vmm.vmware.api.DefaultVMware;
import nl.bitbrains.nebu.vmm.vmware.converter.PhysicalResourceConverter;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vmware.vim25.ArrayOfCheckResult;
import com.vmware.vim25.ArrayUpdateOperation;
import com.vmware.vim25.CheckResult;
import com.vmware.vim25.ClusterConfigInfoEx;
import com.vmware.vim25.ClusterConfigSpecEx;
import com.vmware.vim25.ClusterDrsVmConfigInfo;
import com.vmware.vim25.ClusterDrsVmConfigSpec;
import com.vmware.vim25.DatastoreHostMount;
import com.vmware.vim25.DrsBehavior;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;
import com.vmware.vim25.NoPermission;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.VirtualMachineMovePriority;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.mo.ClusterComputeResource;
import com.vmware.vim25.mo.ComputeResource;
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
import com.vmware.vim25.mo.util.MorUtil;

/**
 * Class responsible for retrieving physical topology information from vSphere
 * through its Java SDK.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VSphere {

    /**
     * The {@link Logger} for this object.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * {@link ManagedEntity} type name for {@link VirtualMachine}.
     */
    public static final String TYPE_VM = "VirtualMachine";

    /**
     * {@link ManagedEntity} type name for {@link PhysicalHost}.
     */
    public static final String TYPE_HOST = "HostSystem";

    /**
     * {@link ManagedEntity} type name for {@link Datastore}.
     */
    public static final String TYPE_DATASTORE = "Datastore";

    /**
     * {@link ManagedEntity} type name for {@link Folder}.
     */
    public static final String TYPE_FOLDER = "Folder";

    /**
     * {@link ManagedEntity} type name for {@link ResourcePool}.
     */
    public static final String TYPE_RESOURCEPOOL = "ResourcePool";

    /**
     * Specifies if this {@link VSphere} object should attempt to perform
     * vMotion operations.
     */
    public static final boolean VSPHERE_VMOTION_ACCESS = true;

    /**
     * Specifies if this {@link VSphere} object should attempt to perform
     * svMotion operations.
     */
    public static final boolean VSPHERE_STORAGE_VMOTION_ACCESS = true;

    /**
     * Specifies if this {@link VSphere} object should attempt to add DRS
     * override rules.
     */
    public static final boolean VSPHERE_DRS_OVERRIDE_ACCESS = false;

    /**
     * The server instance that offers the vSphere REST API.
     */
    private ServiceInstance sInstance;

    /**
     * Navigator to search through the MOB.
     */
    private InventoryNavigator navigator;

    /**
     * Sets up a new connection to the vSphere API. Keeps the connection to
     * vSphere alive. Re-establishes the connection after the connection drops.
     * 
     * @param username
     *            The vSphere username.
     * @param password
     *            The vSphere password.
     * @param url
     *            String that represents the vSphere REST API URI.
     * @param port
     *            The port of the vSphere REST API URI.
     * @throws RemoteException
     *             When an error occurs at the remote.
     * @throws MalformedURLException
     *             When the given {@link String} is not of the right format.
     */
    public void initAndKeepAlive(final String username, final String password, final String url,
            final int port) throws RemoteException, MalformedURLException {
        this.init(username, password, url, port);
        final VSphereKeepAlive keepAlive = new VSphereKeepAlive(this, username, password, url, port);
        new Thread(keepAlive).start();
    }

    /**
     * Sets up a new connection to the vSphere API.
     * 
     * @param username
     *            The vSphere username.
     * @param password
     *            The vSphere password.
     * @param url
     *            String that represents the vSphere REST API URI.
     * @param port
     *            The port of the vSphere REST API URI.
     * @throws RemoteException
     *             When an error occurs at the remote.
     * @throws MalformedURLException
     *             When the given {@link String} is not of the right format.
     */
    public void init(final String username, final String password, final String url, final int port)
            throws RemoteException, MalformedURLException {
        VSphere.logger.debug("Logging in at vSphere at address {}:{}.", url, port);
        this.sInstance = new ServiceInstance(new URL(url), username, password, true);
        this.navigator = new InventoryNavigator(this.sInstance.getRootFolder());
    }

    /**
     * Retrieves the cluster that owns the given {@link ResourcePool}.
     * 
     * @param pool
     *            The {@link ResourcePool}.
     * @return The {@link ComputeResource} Cluster parent of the
     *         {@link ResourcePool}.
     * @throws InvalidProperty
     *             If the {@link ResourcePool} does not have a parent.
     * @throws RuntimeFault
     *             When an error occurs in the vSphere API.
     * @throws RemoteException
     *             When an error occurs at the remote.
     */
    public ComputeResource getClusterFromResourcePool(final ResourcePool pool)
            throws InvalidProperty, RemoteException {
        return pool.getOwner();
    }

    /**
     * Retrieves the {@link Datacenter} in which the given
     * {@link ComputeResource} resides.
     * 
     * @param cluster
     *            The {@link ComputeResource}.
     * @return The corresponding {@link Datacenter}.
     */
    public Datacenter getDatacenterFromComputeResource(final ComputeResource cluster) {
        ManagedEntity parent = cluster.getParent();
        while (parent != null && !(parent instanceof Datacenter)) {
            parent = parent.getParent();
        }
        return (Datacenter) parent;
    }

    /**
     * Builds a {@link PhysicalTopology} containing all resources available to
     * the given {@link ResourcePool}.
     * 
     * @param name
     *            The vSphere name of the {@link ResourcePool}.
     * @return The matching {@link PhysicalTopology}.
     * @throws RemoteException
     *             When an error occurs at the remote.
     * @throws VMwareException
     *             If the {@link ResourcePool} cannot be found from the given
     *             name.
     */
    public PhysicalTopology getPhysicalTopologyFromResourcePoolName(final String name)
            throws RemoteException, VMwareException {
        final ManagedEntity entity = this.searchItems(VSphere.TYPE_RESOURCEPOOL, name);
        if (entity instanceof ResourcePool) {
            final ResourcePool pool = (ResourcePool) entity;
            return this.getPhysicalTopologyFromResourcePool(pool);
        }
        throw new VMwareException("Could not find resource pool.");
    }

    /**
     * Builds a {@link PhysicalTopology} containing all resources available to
     * the given {@link ResourcePool}.
     * 
     * @param pool
     *            The {@link ResourcePool}.
     * @return The matching {@link PhysicalTopology}.
     * @throws RemoteException
     *             When an error occurs at the remote.
     */
    public PhysicalTopology getPhysicalTopologyFromResourcePool(final ResourcePool pool)
            throws RemoteException {
        final ComputeResource cluster = this.getClusterFromResourcePool(pool);
        final List<HostSystem> hosts = this.getHostsFromComputeResource(cluster);
        final List<Datastore> storage = this.getStorageFromComputeResource(cluster);
        final Datacenter dc = this.getDatacenterFromComputeResource(cluster);

        final PhysicalRoot root = new PhysicalRootBuilder().withUuid(DefaultVMware.ROOT_NAME)
                .build();
        final PhysicalDataCenter datacenter = new PhysicalDataCenterBuilder().withUuid(dc.getMOR()
                .getVal()).build();
        final PhysicalRack rack = new PhysicalRackBuilder().withUuid(cluster.getMOR().getVal())
                .build();
        final List<PhysicalHost> cpus = new ArrayList<PhysicalHost>(hosts.size());
        for (final HostSystem host : hosts) {
            cpus.add(PhysicalResourceConverter.toPhysicalHost(host, false));
        }

        final PhysicalTopology topology = new PhysicalTopology(root);

        // Add dc to topology.
        topology.addDataCenter(datacenter);
        // Add racks to dc.
        topology.addRackToDataCenter(rack, datacenter);
        // Add hosts to racks.
        for (final PhysicalHost cpu : cpus) {
            topology.addCPUToRack(cpu, rack);
        }
        // Add disks to hosts or racks.
        this.addDisksToTopology(storage, rack, topology);
        return topology;
    }

    /**
     * Adds {@link Datastore}s to a {@link PhysicalTopology}. If the
     * {@link Datastore} is local, it is added to the corresponding
     * {@link PhysicalHost} in the {@link PhysicalTopology}. If the
     * {@link Datastore} is not local, it is added to the given
     * {@link PhysicalRack}. This {@link PhysicalRack} should be part of the
     * {@link PhysicalTopology}.
     * 
     * @param storage
     *            The {@link Datastore} objects to add to the
     *            {@link PhysicalTopology}.
     * @param rack
     *            The {@link PhysicalRack} where to put shared disks.
     * @param topology
     *            The {@link PhysicalTopology} that contains the given rack and
     *            {@link PhysicalHost}s for the local {@link Datastore}s.
     */
    private void addDisksToTopology(final List<Datastore> storage, final PhysicalRack rack,
            final PhysicalTopology topology) {
        for (final Datastore store : storage) {
            final PhysicalStore pStore = PhysicalResourceConverter.toPhysicalStore(store);
            if (store.getSummary().multipleHostAccess) {
                topology.addDiskToRack(pStore, rack);
            } else {
                this.addDiskToHost(store, pStore, topology);
            }
        }
    }

    /**
     * Adds a local {@link PhysicalStore} to a {@link PhysicalHost} from the
     * {@link PhysicalTopology} if the {@link Datastore} is a local disk. The
     * {@link Datastore} should match the given {@link PhysicalStore}. The
     * {@link Datastore} host should be present in the {@link PhysicalTopology}.
     * 
     * @param store
     *            The {@link Datastore} whose {@link HostSystem} will be matched
     *            to a {@link PhysicalHost} from the {@link PhysicalTopology},
     *            iff it is a local disk.
     * @param pStore
     *            The {@link PhysicalStore} that matches the {@link Datastore}.
     *            This object will be added to a {@link PhysicalHost} from the
     *            {@link PhysicalTopology}.
     * @param topology
     *            The topology that contains {@link PhysicalHost} objects.
     */
    private void addDiskToHost(final Datastore store, final PhysicalStore pStore,
            final PhysicalTopology topology) {
        final DatastoreHostMount[] dataHosts = store.getHost();
        if (dataHosts.length > 0) {
            final DatastoreHostMount hostMount = dataHosts[0];
            if (hostMount != null) {
                final ManagedObjectReference host = hostMount.getKey();
                for (final PhysicalHost pHost : topology.getCPUs()) {
                    if (pHost.getUniqueIdentifier().equals(host.getVal())) {
                        topology.addDiskToHost(pStore, pHost);
                    }
                }
            }
        }
    }

    /**
     * Retrieves all {@link Datastore} objects that are associated with the
     * given {@link ComputeResource}.
     * 
     * @param cluster
     *            The input {@link ComputeResource} cluster.
     * @return A list of {@link Datastore} objects that belong to the
     *         {@link ComputeResource}.
     */
    private List<Datastore> getStorageFromComputeResource(final ComputeResource cluster) {
        ErrorChecker.throwIfNullArgument(cluster, "cluster");
        return new ArrayList<Datastore>(Arrays.asList(cluster.getDatastores()));
    }

    /**
     * Retrieves all {@link HostSystem} contained in the given
     * {@link ComputeResource}.
     * 
     * @param cluster
     *            The {@link ComputeResource}.
     * @return All {@link HostSystem}s available to the {@link ComputeResource}.
     */
    private List<HostSystem> getHostsFromComputeResource(final ComputeResource cluster) {
        return new ArrayList<HostSystem>(Arrays.asList(cluster.getHosts()));
    }

    /**
     * Creates a {@link PhysicalTopology} that contains all resources that are
     * available to the given vApp.
     * 
     * @param vappname
     *            The vSphere name of the vApp.
     * @return A {@link PhysicalTopology}.
     * @throws VMwareException
     *             When the vApp cannot be found.
     */
    public PhysicalTopology getPhysicalTopologyFromVapp(final String vappname)
            throws VMwareException {
        ManagedEntity entity;
        entity = this.searchItems(VSphere.TYPE_FOLDER, vappname);
        if (entity == null) {
            throw new VMwareException("Could not find vApp folder.");
        }
        if (entity instanceof Folder) {
            final Folder vapp = (Folder) entity;
            final ManagedEntity parent = vapp.getParent();
            try {
                return this.getPhysicalTopologyFromResourcePoolName(parent.getName());
            } catch (final RemoteException e) {
                throw new VMwareException("Could not get topology from resource pool name.", e);
            }
        }
        throw new VMwareException("Vapp is no folder.");
    }

    /**
     * Creates a {@link PhysicalTopology} that contains all recources that are
     * available to at least one of the vApps in the list.
     * 
     * @param vappnames
     *            List of vApp names.
     * @return A {@link PhysicalTopology}.
     * @throws VMwareException
     *             If one or more of the vApps cannot be found.
     */
    public PhysicalTopology getPhysicalTopologyFromVappList(final List<String> vappnames)
            throws VMwareException {
        VSphere.logger.entry();
        PhysicalTopology result = new PhysicalTopology(new PhysicalRootBuilder()
                .withUuid(DefaultVMware.ROOT_NAME).build());
        for (final String vAppName : vappnames) {
            final PhysicalTopology topoForVapp = this.getPhysicalTopologyFromVapp(vAppName);
            result = PhysicalTopology.mergeTree(result, topoForVapp);
        }
        return VSphere.logger.exit(result);
    }

    /**
     * Searches for an item in the vSphere managed object store.
     * 
     * @param type
     *            The type of object to look for.
     * @param name
     *            The name of the object.
     * @return A {@link ManagedEntity} that matches the object, or
     *         <code>null</code> if the object cannot be found.
     */
    public ManagedEntity searchItems(final String type, final String name) {
        try {
            return this.navigator.searchManagedEntity(type, name);
        } catch (final RemoteException e) {
            VSphere.logger.catching(Level.WARN, e);
        }
        return null;
    }

    /**
     * Searches for an item in vSphere with the given type and id.
     * 
     * @param type
     *            The type of the object.
     * @param id
     *            The id of the object.
     * @return The matched {@link ManagedEntity}.
     */
    public ManagedEntity searchItemsById(final String type, final String id) {
        final ManagedObjectReference mor = new ManagedObjectReference();
        mor.setType(type);
        mor.setVal(id);
        return (ManagedEntity) MorUtil.createExactManagedObject(this.sInstance
                .getServerConnection(), mor);
    }

    /**
     * @return the sInstance
     */
    public ServiceInstance getServiceInstance() {
        return this.sInstance;
    }

    /**
     * @param sInstance
     *            the sInstance to set
     */
    protected void setServiceInstance(final ServiceInstance sInstance) {
        this.sInstance = sInstance;
    }

    /**
     * 
     * @param navigator
     *            The {@link InventoryNavigator} to set.
     */
    protected void setNavigator(final InventoryNavigator navigator) {
        this.navigator = navigator;
    }

    /**
     * Moves a VM to a specific host.
     * 
     * @param vmname
     *            The vSphere name of the virtual machine.
     * @param hostid
     *            The vSphere id of the host.
     * @throws VMwareException
     *             When the virtual machine, host, or cluster cannot be
     *             retrieved from the managed object browser.
     * @throws RemoteException
     *             When an error occurs at the remote.
     * @throws InterruptedException
     *             When waiting for the VM to be moved gets interrupted.
     */
    public void moveVMToHost(final String vmname, final String hostid) throws VMwareException,
            RemoteException, InterruptedException {
        final ManagedEntity vmEntity = this.searchItems(VSphere.TYPE_VM, vmname);
        final ManagedEntity hostEntity = this.searchItemsById(VSphere.TYPE_HOST, hostid);

        if (vmEntity instanceof VirtualMachine && hostEntity instanceof HostSystem) {
            final VirtualMachine vm = (VirtualMachine) vmEntity;
            final HostSystem host = (HostSystem) hostEntity;
            final ComputeResource resource = vm.getResourcePool().getOwner();

            if (resource instanceof ClusterComputeResource) {
                VSphere.logger.debug("Moving VM {} to host {}.", vmname, hostid);
                final Task task = this.moveVMToHost((ClusterComputeResource) resource, vm, host);
                if (task == null) {
                    throw new VMwareException("Could not create VM migration task.");
                }
            } else {
                throw new VMwareException("Could not find cluster from VM.");
            }
        } else {
            throw new VMwareException("Could not find VM or Host while moving.");
        }
    }

    /**
     * Moves the given {@link VirtualMachine} to the given {@link HostSystem}.
     * The {@link VirtualMachine} and the {@link HostSystem} should reside in
     * the given {@link ClusterComputeResource}. This method only tries to take
     * action when VSPHERE_WRITE_ACCESS has been set to <code>true</code>.
     * 
     * @param cluster
     *            The {@link ClusterComputeResource} which contains the
     *            {@link VirtualMachine} and the {@link HostSystem}.
     * @param vm
     *            The {@link VirtualMachine} to move.
     * @param host
     *            The {@link HostSystem} where the {@link VirtualMachine} should
     *            be moved to.
     * @return A {@link Task} object for moving the {@link VirtualMachine} to
     *         the {@link HostSystem}.
     * @throws RemoteException
     *             When an error occurs at the remote.
     * @throws InterruptedException
     *             When waiting for the {@link VirtualMachine} to be moved gets
     *             interrupted.
     * @throws VMwareException
     *             If the given {@link VirtualMachine} is not part of the given
     *             {@link ClusterComputeResource}, or the {@link VirtualMachine}
     *             does not pass migration checks.
     */
    private Task moveVMToHost(final ClusterComputeResource cluster, final VirtualMachine vm,
            final HostSystem host) throws RemoteException, InterruptedException, VMwareException {
        if (!vm.getResourcePool().getOwner().getMOR().equals(cluster.getMOR())) {
            throw new VMwareException("Virtual Machine does not belong to given cluster.");
        }
        if (!this.checkMoveVMToHost(vm, host)) {
            throw new VMwareException("Virtual Machine does not pass migration checks.");
        }
        if (VSphere.VSPHERE_DRS_OVERRIDE_ACCESS) {
            this.disableDRS(vm, cluster);
        }
        if (VSphere.VSPHERE_VMOTION_ACCESS) {
            return vm.migrateVM_Task(null, host, VirtualMachineMovePriority.defaultPriority, null);
        }
        VSphere.logger.warn("Did not perform vMotion because write access is set to false.");
        return null;
    }

    /**
     * Checks if it is possible to migrate the {@link VirtualMachine} to the
     * given {@link HostSystem}.
     * 
     * @param vm
     *            The {@link VirtualMachine} to migrate.
     * @param host
     *            The {@link HostSystem} where the {@link VirtualMachine} should
     *            migrate to.
     * @return <code>true</code> iff the migration should be successful.
     */
    private boolean checkMoveVMToHost(final VirtualMachine vm, final HostSystem host) {
        try {
            final Task task = this.sInstance.getVirtualMachineProvisioningChecker()
                    .checkMigrate_Task(vm, host, null, null, null);
            task.waitForTask();
            return this.checkResult(task.getTaskInfo().getResult());
        } catch (final RemoteException | InterruptedException e) {
            VSphere.logger.catching(Level.WARN, e);
        }
        return false;
    }

    /**
     * Makes the given {@link VirtualMachine} use the given {@link Datastore}.
     * 
     * @param vmName
     *            The vSphere name of the {@link VirtualMachine}.
     * @param datastoreId
     *            The vSphere id of the {@link Datastore}.
     * @throws VMwareException
     *             If the {@link VirtualMachine} or {@link Datastore} cannot be
     *             found.
     * @throws RemoteException
     *             If an error occurs at the remote.
     * @throws InterruptedException
     *             If the method is interrupted during the move operation.
     */
    public void moveVMToStorage(final String vmName, final String datastoreId)
            throws VMwareException, RemoteException, InterruptedException {
        final ManagedEntity vmEntity = this.searchItems(VSphere.TYPE_VM, vmName);
        final ManagedEntity storeEntity = this.searchItemsById(VSphere.TYPE_DATASTORE, datastoreId);

        if (vmEntity instanceof VirtualMachine && storeEntity instanceof Datastore) {
            final VirtualMachine vm = (VirtualMachine) vmEntity;
            final Datastore store = (Datastore) storeEntity;

            VSphere.logger.debug("Moving VM {} to store {}.", vmName, datastoreId);
            final Task task = this.moveVMToStorage(vm, store);
            if (task != null) {
                task.waitForTask();
            }
        } else {
            throw new VMwareException("Could not find VM or Datastore while moving");
        }
    }

    /**
     * Makes the given {@link VirtualMachine} use the given {@link Datastore}.
     * 
     * @param vm
     *            The {@link VirtualMachine} whose {@link Datastore} needs to be
     *            changed.
     * @param store
     *            The {@link Datastore} that the {@link VirtualMachine} should
     *            use.
     * @return The task that executes this update.
     * @throws RemoteException
     *             When an error occurs at the remote.
     * @throws VMwareException
     *             If the {@link VirtualMachine} does not pass relocate checks.
     */
    private Task moveVMToStorage(final VirtualMachine vm, final Datastore store)
            throws RemoteException, VMwareException {
        final VirtualMachineRelocateSpec spec = new VirtualMachineRelocateSpec();
        spec.setDatastore(store.getMOR());
        if (!this.checkMoveVMToStorage(vm, spec)) {
            throw new VMwareException("Virtual Machine does not pass relocate checks.");
        }
        if (VSphere.VSPHERE_STORAGE_VMOTION_ACCESS) {
            return vm.relocateVM_Task(spec);
        }
        VSphere.logger.warn("Did not perform svMotion because write access is set to false.");
        return null;
    }

    /**
     * Checks if the {@link VirtualMachine} can be moved to the given
     * {@link Datastore}.
     * 
     * @param vm
     *            The {@link VirtualMachine} to relocate.
     * @param spec
     *            The {@link Datastore} to relocate to.
     * @return <code>true</code> iff the relocation should be successful.
     */
    private boolean checkMoveVMToStorage(final VirtualMachine vm,
            final VirtualMachineRelocateSpec spec) {
        try {
            final Task task = this.sInstance.getVirtualMachineProvisioningChecker()
                    .checkRelocate_Task(vm, spec, null);
            task.waitForTask();
            return this.checkResult(task.getTaskInfo().getResult());
        } catch (final RemoteException | InterruptedException e) {
            VSphere.logger.catching(Level.WARN, e);
        }
        return false;
    }

    /**
     * Checks a {@link Task} result for errors. Expects a
     * {@link ArrayOfCheckResult} result.
     * 
     * @param obj
     *            The {@link Task} {@link ArrayOfCheckResult} object.
     * @return <code>true</code> iff the result contained no errors.
     */
    private boolean checkResult(final Object obj) {
        if (obj instanceof ArrayOfCheckResult) {
            final ArrayOfCheckResult resultArray = (ArrayOfCheckResult) obj;
            final CheckResult[] results = resultArray.getCheckResult();
            for (int i = 0; i < results.length; i++) {
                final boolean check = this.checkResult(results[i]);
                if (!check) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Checks a single {@link Task} result for errors.
     * 
     * @param result
     *            The {@link CheckResult} to check.
     * @return <code>true</code> iff the {@link CheckResult} contained no
     *         errors.
     */
    private boolean checkResult(final CheckResult result) {
        final LocalizedMethodFault[] errors = result.getError();
        if (errors == null || errors.length == 0) {
            return true;
        } else {
            for (int i = 0; i < errors.length; i++) {
                final MethodFault fault = errors[i].getFault();
                VSphere.logger.warn("Check result error. Messages: {}\t{}.",
                                    errors[i].getLocalizedMessage(),
                                    fault.getMessage());
            }
        }
        return false;
    }

    /**
     * Turns off the DRS for a specific {@link VirtualMachine} in the given
     * {@link ClusterComputeResource}.
     * 
     * @param vm
     *            The {@link VirtualMachine} for which to turn off DRS.
     * @param cluster
     *            The {@link ClusterComputeResource} that hosts to
     *            {@link VirtualMachine}.
     */
    void disableDRS(final VirtualMachine vm, final ClusterComputeResource cluster) {
        VSphere.logger.info("Disabling DRS for vm {} on cluster {}.", vm.getMOR().getVal(), cluster
                .getMOR().getVal());
        VSphere.logger.trace("Retrieving cluster and DRS information.");
        final ClusterConfigInfoEx clusterInfo = (ClusterConfigInfoEx) cluster.getConfigurationEx();
        final ClusterDrsVmConfigInfo existingInfo = this.findMatchingDrsVmConfigInfo(vm,
                                                                                     clusterInfo);

        VSphere.logger.trace("Building new VM configuration.");
        final ClusterDrsVmConfigInfo info = this.prepareVmConfigInfo(vm, existingInfo);
        ArrayUpdateOperation operation = null;

        if (existingInfo == null) {
            operation = ArrayUpdateOperation.add;
        } else {
            operation = ArrayUpdateOperation.edit;
        }
        VSphere.logger.trace("Decided DRS override operation is an {}", operation.name());

        final ClusterDrsVmConfigSpec configSpec = this.createConfigSpec(operation, info);
        final ClusterConfigSpecEx specEx = new ClusterConfigSpecEx();
        specEx.setDrsVmConfigSpec(new ClusterDrsVmConfigSpec[] { configSpec });
        Task task = null;
        try {
            VSphere.logger.trace("Attempting to reconfigure cluster DRS settings.");
            task = cluster.reconfigureComputeResource_Task(specEx, true);
            task.waitForTask();
        } catch (final NoPermission e1) {
            VSphere.logger.catching(Level.ERROR, e1);
            VSphere.logger.debug(e1.getPrivilegeId());
        } catch (final RemoteException | InterruptedException e) {
            VSphere.logger.catching(Level.ERROR, e);
        }
    }

    /**
     * Creates a {@link ClusterDrsVmConfigInfo} that turns off DRS for the given
     * vm. If the given {@link ClusterDrsVmConfigInfo} is not <code>null</code>,
     * this object will be updated to turn off DRS. Otherwise, a new object is
     * created.
     * 
     * @param vm
     *            The vm for which to turn off DRS.
     * @param existingInfo
     *            An existing DRS override rule for the given vm. If this is
     *            <code>null</code>, a new {@link ClusterDrsVmConfigInfo} will
     *            be created.
     * @return A {@link ClusterDrsVmConfigInfo} that turns off DRS for the given
     *         vm.
     */
    private ClusterDrsVmConfigInfo prepareVmConfigInfo(final VirtualMachine vm,
            final ClusterDrsVmConfigInfo existingInfo) {
        ClusterDrsVmConfigInfo info = null;
        if (existingInfo != null) {
            info = existingInfo;
        } else {
            info = new ClusterDrsVmConfigInfo();
            info.setKey(vm.getMOR());
        }
        info.setEnabled(true);
        info.setBehavior(DrsBehavior.manual);
        return info;
    }

    /**
     * Creates a new {@link ClusterDrsVmConfigSpec} for the given
     * {@link ArrayUpdateOperation} and {@link ClusterDrsVmConfigInfo}.
     * 
     * @param operation
     *            The {@link ArrayUpdateOperation} that should be used.
     * @param info
     *            The {@link ClusterDrsVmConfigInfo} that should be used.
     * @return A new {@link ClusterDrsVmConfigSpec} that uses the given
     *         parameters.
     */
    private ClusterDrsVmConfigSpec createConfigSpec(final ArrayUpdateOperation operation,
            final ClusterDrsVmConfigInfo info) {
        final ClusterDrsVmConfigSpec spec = new ClusterDrsVmConfigSpec();
        spec.setOperation(operation);
        spec.setInfo(info);
        return spec;
    }

    /**
     * Looks for an existing DRS override rule for the given
     * {@link VirtualMachine} in the {@link ClusterConfigInfoEx}.
     * 
     * @param vm
     *            The {@link VirtualMachine} in the override rule we are looking
     *            for.
     * @param clusterInfo
     *            The {@link ClusterConfigInfoEx} that contains all drs override
     *            rules for a specific cluster.
     * @return the {@link ClusterDrsVmConfigInfo} if it exists,
     *         <code>null</code> otherwise.
     */
    private ClusterDrsVmConfigInfo findMatchingDrsVmConfigInfo(final VirtualMachine vm,
            final ClusterConfigInfoEx clusterInfo) {
        for (final ClusterDrsVmConfigInfo vminfo : clusterInfo.getDrsVmConfig()) {
            if (vminfo.getKey().getVal().equals(vm.getMOR().getVal())) {
                return vminfo;
            }
        }
        return null;
    }

    /**
     * Get the host id of a vm.
     * 
     * @param vsphereName
     *            The vSphere name of the virtual machine.
     * @return The vSphere id of its host.
     * @throws RemoteException
     *             When an error occurs at the remote.
     * @throws NoSuchVMException
     *             When the virtual machine cannot be found.
     */
    public String getVirtualMachineHost(final String vsphereName) throws RemoteException,
            NoSuchVMException {
        final ManagedEntity entity = this.searchItems(VSphere.TYPE_VM, vsphereName);
        if (entity instanceof VirtualMachine) {
            final VirtualMachine vm = (VirtualMachine) entity;
            return vm.getSummary().getRuntime().getHost().getVal();
        }
        throw new NoSuchVMException("Cannot find vm with name: " + vsphereName);
    }

    /**
     * Get the store id of every store the vm has access to.
     * 
     * @param vsphereName
     *            The vSphere name of the virtual machine.
     * @return A list containing the id of every datastore the virtual machine
     *         has access to.
     * @throws NoSuchVMException
     *             When the vm cannot be found from the given name.
     * @throws RemoteException
     *             When an error occurs at the remote.
     */
    public List<String> getVirtualMachineStores(final String vsphereName) throws NoSuchVMException,
            RemoteException {
        final ManagedEntity entity = this.searchItems(VSphere.TYPE_VM, vsphereName);
        if (entity instanceof VirtualMachine) {
            final VirtualMachine vm = (VirtualMachine) entity;
            final Datastore[] stores = vm.getDatastores();
            final List<String> storeIds = new ArrayList<String>(stores.length);
            for (final Datastore dstore : stores) {
                storeIds.add(dstore.getMOR().getVal());
            }
            return storeIds;
        }
        throw new NoSuchVMException("Cannot find vm with name: " + vsphereName);
    }

    /**
     * Checks if the given Virtual Application {@link String} can run on the
     * given {@link PhysicalHost}.
     * 
     * @param host
     *            The {@link PhysicalHost}.
     * @param vappname
     *            The vSphere virtual Application name.
     * @return <code>true</code> iff the virtual application can use the given
     *         {@link PhysicalHost}.
     */
    public boolean hostHasVapp(final PhysicalHost host, final String vappname) {
        ManagedEntity entity;
        try {
            entity = this.searchItems(VSphere.TYPE_FOLDER, vappname);

            if (entity instanceof Folder) {
                final Folder vapp = (Folder) entity;
                return this.hostHasVapp(host, vapp);
            }
        } catch (final RemoteException e) {
            VSphere.logger.catching(Level.ERROR, e);
        }
        return false;
    }

    /**
     * Checks if the given Virtual Application {@link Folder} can run on the
     * given {@link PhysicalHost}.
     * 
     * @param host
     *            The {@link PhysicalHost}.
     * @param vapp
     *            The vSphere virtual application {@link Folder}.
     * @return <code>true</code> iff the virtual application can use the given
     *         {@link PhysicalHost}.
     * @throws RemoteException
     *             If an error occurs at the remote.
     */
    private boolean hostHasVapp(final PhysicalHost host, final Folder vapp) throws RemoteException {
        final String poolname = vapp.getParent().getName();
        final ManagedEntity poolEntity = this.searchItems(VSphere.TYPE_RESOURCEPOOL, poolname);
        if (poolEntity instanceof ResourcePool) {
            final ResourcePool pool = (ResourcePool) poolEntity;
            return this.poolHasHost(host, pool);
        }
        return false;
    }

    /**
     * Checks if the given {@link ResourcePool} contains the given
     * {@link PhysicalHost}.
     * 
     * @param host
     *            the {@link PhysicalHost}.
     * @param pool
     *            The {@link ResourcePool}.
     * @return <code>true</code> iff the {@link ResourcePool} contains a
     *         {@link HostSystem} that is equal to the given
     *         {@link PhysicalHost}.
     * @throws RemoteException
     *             If an error occurs at the remote.
     */
    private boolean poolHasHost(final PhysicalHost host, final ResourcePool pool)
            throws RemoteException {
        final List<HostSystem> hosts = this.getHostsFromComputeResource(this
                .getClusterFromResourcePool(pool));
        for (final HostSystem system : hosts) {
            if (system.getMOR().getVal().equals(host.getUniqueIdentifier())) {
                return true;
            }
        }
        return false;
    }
}
