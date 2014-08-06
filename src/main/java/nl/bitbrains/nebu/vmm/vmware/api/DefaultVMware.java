package nl.bitbrains.nebu.vmm.vmware.api;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.topology.PhysicalHost;
import nl.bitbrains.nebu.common.topology.PhysicalStore;
import nl.bitbrains.nebu.common.topology.PhysicalTopology;
import nl.bitbrains.nebu.common.util.ErrorChecker;
import nl.bitbrains.nebu.vmm.vmware.api.vcloud.VCloud;
import nl.bitbrains.nebu.vmm.vmware.api.vsphere.VSphere;
import nl.bitbrains.nebu.vmm.vmware.converter.PhysicalResourceConverter;
import nl.bitbrains.nebu.vmm.vmware.converter.VirtualConverter;
import nl.bitbrains.nebu.vmm.vmware.entity.VirtualApplication;
import nl.bitbrains.nebu.vmm.vmware.entity.VmBootStatus;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMLaunchException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ManagedEntity;

/**
 * Default implementation of the {@link VMware} interface.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class DefaultVMware implements VMware {

    /**
     * Logger for this class.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * Name of XML root element.
     */
    public static final String ROOT_NAME = "root";

    /**
     * Used for retrieving virtual topology information from VMware.
     */
    private VCloud vcloud;

    /**
     * Used for retrieving physical topology information from VMware.
     */
    private VSphere vsphere;

    /**
     * Sets up the connection to vCloud and vSphere.
     * 
     * @param vcloudusername
     *            The vCloud username.
     * @param vloudpassword
     *            The vCloud password.
     * @param vcloudurl
     *            The URL of the vCloud REST API.
     * @param vsphereusername
     *            The vSphere username.
     * @param vspherepassword
     *            The vSphere password.
     * @param vsphereurl
     *            The URL of the vSphere REST API.
     * @param vsphereport
     *            The port of the vSphere REST API.
     * @throws RemoteException
     *             If something goes wrong at the remote.
     * @throws MalformedURLException
     *             If a given URL string is malformed.
     */
    public void init(final String vcloudusername, final String vloudpassword,
            final String vcloudurl, final String vsphereusername, final String vspherepassword,
            final String vsphereurl, final int vsphereport) throws RemoteException,
            MalformedURLException {
        this.vcloud = new VCloud();
        this.vsphere = new VSphere();
        this.vcloud.initAndKeepAlive(vcloudusername, vloudpassword, vcloudurl);
        this.vsphere.initAndKeepAlive(vsphereusername, vspherepassword, vsphereurl, vsphereport);
    }

    @Override
    public PhysicalTopology getPhysicalTopologyFromResourcePool(final String poolname)
            throws VMwareException {
        try {
            return this.vsphere.getPhysicalTopologyFromResourcePoolName(poolname);
        } catch (final RemoteException e) {
            throw new VMwareException("Could not get topologt from Resource Pool name", e);
        }
    }

    @Override
    public PhysicalStore getStoreInfo(final String uuid) throws VMwareException {
        final ManagedEntity entity = this.vsphere.searchItemsById(VSphere.TYPE_DATASTORE, uuid);
        if (entity instanceof Datastore) {
            final Datastore store = (Datastore) entity;
            return PhysicalResourceConverter.toPhysicalStore(store);
        }
        throw new VMwareException("Could not find PhysicalStore " + uuid);
    }

    @Override
    public PhysicalHost getHostInfo(final String uuid) throws VMwareException {
        final ManagedEntity entity = this.vsphere.searchItemsById(VSphere.TYPE_HOST, uuid);
        if (entity instanceof HostSystem) {
            final HostSystem system = (HostSystem) entity;
            return PhysicalResourceConverter.toPhysicalHost(system, true);
        }
        throw new VMwareException("Could not find PhysicalHost " + uuid);
    }

    @Override
    public List<VirtualApplication> getAllVapps() throws VMwareException {
        try {
            return this.vcloud.getAllVapps();
        } catch (final VCloudException e) {
            throw new VMwareException("Could not get all vApps.", e);
        }
    }

    @Override
    public List<String> getVirtualResourceList() throws VMwareException {
        try {
            return this.vcloud.getVirtualResourceList();
        } catch (final VCloudException e) {
            throw new VMwareException("Could not get virtual resource list.", e);
        }
    }

    @Override
    public List<String> getVmIdsFromNames(final List<String> vmIds, final List<String> vmNames)
            throws NoSuchVMException, VMwareException {
        try {
            return this.vcloud.getVmIdsFromNames(vmIds, vmNames);
        } catch (final VCloudException e) {
            throw new VMwareException("Could not get VM Ids from names.", e);
        }
    }

    @Override
    public String getVmIdFromName(final List<String> vmIds, final String vmName)
            throws NoSuchVMException, VMwareException {
        try {
            return this.vcloud.getVmIdFromName(vmIds, vmName);
        } catch (final VCloudException e) {
            throw new VMwareException("Could not get VM id from name.", e);
        }
    }

    @Override
    public VirtualMachine getVirtualMachineInfo(final String uuid) throws VMwareException,
            NoSuchVMException {
        VirtualMachine vm;
        try {
            vm = this.vcloud.getVirtualMachineInfo(uuid);
            final String vmName = VirtualConverter.buildVsphereName(vm.getHostname(), uuid);
            vm.setHost(this.vsphere.getVirtualMachineHost(vmName));
            for (final String storeid : this.vsphere.getVirtualMachineStores(vmName)) {
                vm.addStore(storeid);
            }
        } catch (final VCloudException | RemoteException e) {
            throw new VMwareException("Could not get virtual machine info.", e);
        }
        return vm;
    }

    @Override
    public PhysicalTopology getPhysicalTopologyForVapps(final List<String> vAppIds)
            throws VMwareException {
        DefaultVMware.logger.info("Getting phys for vapps {}.", vAppIds);
        List<VirtualApplication> vapps = null;
        try {
            vapps = this.vcloud.getAllVapps();
        } catch (final VCloudException e) {
            DefaultVMware.logger.catching(Level.ERROR, e);
            throw new VMwareException("Could not get all vApps.", e);
        }
        final List<String> vappNames = new ArrayList<String>(vAppIds.size());
        for (final VirtualApplication vapp : vapps) {
            if (vAppIds.contains(vapp.getUniqueIdentifier())) {
                final String vcloudname = vapp.getName();
                final String vcloudid = vapp.getUniqueIdentifier();
                vappNames.add(VirtualConverter.buildVsphereName(vcloudname, vcloudid));
            }
        }
        return this.vsphere.getPhysicalTopologyFromVappList(vappNames);
    }

    @Override
    public VmBootStatus createVM(final VirtualMachine vm, final VirtualApplication dest,
            final String hostId, final String hostname) throws VMLaunchException {
        return this.createVM(vm, dest, hostId, hostname, null);
    }

    @Override
    public VmBootStatus createVM(final VirtualMachine vm, final VirtualApplication dest,
            final String hostId, final String hostname, final String storageUnitId)
            throws VMLaunchException {
        ErrorChecker.throwIfNullArgument(vm, "vm");
        ErrorChecker.throwIfNullArgument(dest, "dest");
        ErrorChecker.throwIfNullArgument(hostId, "hostId");
        ErrorChecker.throwIfNullArgument(hostname, "hostname");
        DefaultVMware.logger.info("Going to copy VM with id {}.", vm.getUniqueIdentifier());
        DefaultVMware.logger.info("Creating new VM at vApp {}.", dest.getName());
        DefaultVMware.logger.info("Creating new VM at host {}.", hostId);
        DefaultVMware.logger.info("New VM will have hostname {}.", hostname);
        final VMStartTask.Builder builder = new VMStartTask.Builder().withVCloud(this.vcloud)
                .withVSphere(this.vsphere).withVirtualMachine(vm).withVirtualApplication(dest)
                .withHostId(hostId).withHostname(hostname);
        if (storageUnitId != null) {
            builder.withStoreId(storageUnitId);
        }
        final VMStartTask task = builder.build();
        new Thread(task).start();
        DefaultVMware.logger.info("Created and started stask {}.", task);
        return task.getProgress();
    }

    @Override
    public void killVM(final VirtualMachine vm) {
        this.vcloud.killVM(vm);
    }

    @Override
    public void moveVMToHost(final VirtualMachine vm, final PhysicalHost host)
            throws VMwareException {
        try {
            this.vsphere.moveVMToHost(VirtualConverter.buildVsphereName(vm.getHostname(),
                                                                        vm.getUniqueIdentifier()),
                                      host.getUniqueIdentifier());
        } catch (RemoteException | InterruptedException e) {
            throw new VMwareException("Error while moving VM to new host.", e);
        }
    }

    @Override
    public void moveVMToStore(final VirtualMachine vm, final PhysicalStore store)
            throws VMwareException {
        try {
            this.vsphere
                    .moveVMToStorage(VirtualConverter.buildVsphereName(vm.getHostname(),
                                                                       vm.getUniqueIdentifier()),
                                     store.getUniqueIdentifier());
        } catch (RemoteException | VMwareException | InterruptedException e) {
            throw new VMwareException("Error while moving VM to new store.", e);
        }
    }

    @Override
    public VirtualApplication selectVirtualApplicationFromHost(final PhysicalHost host,
            final List<String> possibleVapps) {
        List<VirtualApplication> vapps = null;
        try {
            vapps = this.getAllVapps();

        } catch (final VMwareException e) {
            DefaultVMware.logger.catching(Level.ERROR, e);
            return null;
        }
        for (final VirtualApplication vapp : vapps) {
            if (possibleVapps.contains(vapp.getUniqueIdentifier())
                    && this.vsphere.hostHasVapp(host, VirtualConverter.buildVsphereName(vapp
                            .getName(), vapp.getUniqueIdentifier()))) {
                return vapp;
            }
        }
        return null;
    }

    /**
     * @return the vcloud
     */
    public VCloud getVcloud() {
        return this.vcloud;
    }

    /**
     * @param vcloud
     *            the vcloud to set
     */
    public void setVcloud(final VCloud vcloud) {
        this.vcloud = vcloud;
    }

    /**
     * @return the vsphere
     */
    public VSphere getVsphere() {
        return this.vsphere;
    }

    /**
     * @param vsphere
     *            the vsphere to set
     */
    public void setVsphere(final VSphere vsphere) {
        this.vsphere = vsphere;
    }

}
