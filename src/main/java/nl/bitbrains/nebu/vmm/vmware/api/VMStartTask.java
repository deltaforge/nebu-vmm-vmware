package nl.bitbrains.nebu.vmm.vmware.api;

import java.rmi.RemoteException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.interfaces.IBuilder;
import nl.bitbrains.nebu.common.util.ErrorChecker;
import nl.bitbrains.nebu.vmm.vmware.api.vcloud.VCloud;
import nl.bitbrains.nebu.vmm.vmware.api.vsphere.VSphere;
import nl.bitbrains.nebu.vmm.vmware.converter.VirtualConverter;
import nl.bitbrains.nebu.vmm.vmware.entity.VirtualApplication;
import nl.bitbrains.nebu.vmm.vmware.entity.VmBootStatus;
import nl.bitbrains.nebu.vmm.vmware.entity.VmBootStatus.Status;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;
import nl.bitbrains.nebu.vmm.vmware.exception.VMwareException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vim25.NoPermission;

/**
 * Class that takes care of starting a new VM.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VMStartTask implements Runnable {

    /**
     * Logger used by this class.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * The {@link VCloud} to use.
     */
    private final VCloud vcloud;

    /**
     * The {@link VSphere} to use.
     */
    private final VSphere vsphere;

    /**
     * The {@link VirtualMachine} to start.
     */
    private final VirtualMachine vm;

    /**
     * The {@link VirtualApplication} to start the new VM in.
     */
    private final VirtualApplication virtualApp;

    /**
     * The vSphere host id where to put the VM.
     */
    private final String hostid;

    /**
     * The vSphere storage id.
     */
    private final String storageid;

    /**
     * The hostname the VM should have.
     */
    private final String hostname;

    /**
     * The {@link VmBootStatus} object where to save the deployment status.
     */
    private final VmBootStatus progress;

    /**
     * Creates a new {@link VMStartTask}.
     * 
     * @param vcloud
     *            The {@link VCloud} to use.
     * @param vsphere
     *            The {@link VSphere} to use.
     * @param vm
     *            The {@link VirtualMachine} to start.
     * @param vapp
     *            The {@link VirtualApplication} where the new VM should be
     *            placed.
     * @param hostid
     *            The vSphere ID of the host system.
     * @param storageid
     *            The vSpehre ID of the storage.
     * @param hostname
     *            The hostname the VM should get.
     * @param progress
     *            The {@link VmBootStatus} where to save the deployment status.
     */
    protected VMStartTask(final VCloud vcloud, final VSphere vsphere, final VirtualMachine vm,
            final VirtualApplication vapp, final String hostid, final String storageid,
            final String hostname, final VmBootStatus progress) {
        this.vcloud = vcloud;
        this.vsphere = vsphere;
        this.vm = vm;
        this.virtualApp = vapp;
        this.hostid = hostid;
        this.storageid = storageid;
        this.hostname = hostname;
        this.progress = progress;
    }

    /**
     * @return The deployment progress.
     */
    public VmBootStatus getProgress() {
        return this.progress;
    }

    @Override
    public void run() {
        this.syncRun();
    }

    /**
     * Deploys a new virtual machine in three steps.
     * 
     * 1. Adding the vm to the vapp. 2. Moving the vm to the correct host. 3.
     * Moving the vm to the correct datastore. 4. Powering on the vm.
     */
    public void syncRun() {
        VCloud.LOCK.lock();
        try {
            this.progress.setStatus(Status.PROCESSESING);

            // 1. Add new vm to the existing vapp.
            // TODO create vcloud createVM wrapper to simplify calls.
            VMStartTask.logger.info("Creating new VM.");
            final String vmID = this.vcloud.createVM(this.vm, this.virtualApp, this.hostname);

            this.progress.setStatus(Status.CREATED);

            // 2. Move the vm to the correct host.
            final String newVMName = VirtualConverter.buildVsphereName(this.hostname, vmID);
            VMStartTask.logger.info("Moving VM from host {} to host {}.",
                                    this.vsphere.getVirtualMachineHost(newVMName),
                                    this.hostid);
            this.vsphere.moveVMToHost(newVMName, this.hostid);

            // 3. Move the vm to the correct datastore.
            if (this.storageid != null) {
                VMStartTask.logger.info("Moving VM from store {} to store {}.",
                                        this.vsphere.getVirtualMachineStores(newVMName),
                                        this.hostid);
                this.vsphere.moveVMToStorage(newVMName, this.storageid);
            }

            VMStartTask.logger.info("Powering on VM.");
            // 4. Powering on the vm.
            this.vcloud.powerOnVM(vmID);
            this.progress.setVmId(vmID);
            this.progress.setStatus(Status.SUCCESS);
            VMStartTask.logger.info("VM Create task completed.");
        } catch (final NoPermission e1) {
            VMStartTask.logger.catching(Level.ERROR, e1);
            VMStartTask.logger.error(e1.getPrivilegeId());
        } catch (final VCloudException | TimeoutException | NoSuchVMException | RemoteException
                | VMwareException | InterruptedException e) {
            VMStartTask.logger.catching(Level.ERROR, e);
        } finally {
            VCloud.LOCK.unlock();
        }
    }

    /**
     * Builds a {@link VMStartTask}.
     * 
     * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
     * 
     */
    public static class Builder implements IBuilder<VMStartTask> {

        private VCloud vcloud;
        private VSphere vsphere;
        private VirtualMachine vm;
        private VirtualApplication vapp;
        private String hostid;
        private String storageid;
        private String hostname;
        private VmBootStatus progress;

        /**
         * Creates a new builder;
         */
        public Builder() {
            this.reset();
        }

        /**
         * @param vcloud
         *            The {@link VCloud} to use.
         * @return The {@link Builder}, for fluency.
         */
        public Builder withVCloud(final VCloud vcloud) {
            ErrorChecker.throwIfNullArgument(vcloud, "vcloud");

            this.vcloud = vcloud;
            return this;
        }

        /**
         * @param vsphere
         *            The {@link VSphere} to use.
         * @return The {@link Builder}, for fluency.
         */
        public Builder withVSphere(final VSphere vsphere) {
            ErrorChecker.throwIfNullArgument(vsphere, "vsphere");

            this.vsphere = vsphere;
            return this;
        }

        /**
         * @param vm
         *            The {@link VirtualMachine} to use.
         * @return The {@link Builder}, for fluency.
         */
        public Builder withVirtualMachine(final VirtualMachine vm) {
            ErrorChecker.throwIfNullArgument(vm, "vm");

            this.vm = vm;
            return this;
        }

        /**
         * @param vapp
         *            The {@link VirtualApplication} to use.
         * @return The {@link Builder}, for fluency.
         */
        public Builder withVirtualApplication(final VirtualApplication vapp) {
            ErrorChecker.throwIfNullArgument(vapp, "vapp");

            this.vapp = vapp;
            return this;
        }

        /**
         * @param hostId
         *            The host ID to use.
         * @return The {@link Builder}, for fluency.
         */
        public Builder withHostId(final String hostId) {
            ErrorChecker.throwIfNullArgument(hostId, "hostId");

            this.hostid = hostId;
            return this;
        }

        /**
         * @param storeid
         *            The store ID to use.
         * @return The {@link Builder}, for fluency.
         */
        public Builder withStoreId(final String storeid) {
            ErrorChecker.throwIfNullArgument(storeid, "storeid");

            this.storageid = storeid;
            return this;
        }

        /**
         * @param hostname
         *            The hostname to use.
         * @return The {@link Builder}, for fluency.
         */
        public Builder withHostname(final String hostname) {
            ErrorChecker.throwIfNullArgument(hostname, "hostname");

            this.hostname = hostname;
            return this;
        }

        /**
         * @param progress
         *            The {@link VmBootStatus} to use.
         * @return The {@link Builder}, for fluency.
         */
        public Builder withTaskProgress(final VmBootStatus progress) {
            ErrorChecker.throwIfNullArgument(progress, "progress");

            this.progress = progress;
            return this;
        }

        @Override
        public VMStartTask build() {
            ErrorChecker.throwIfNotSet(this.vcloud, "VCloud");
            ErrorChecker.throwIfNotSet(this.vm, "VirtualMachine");
            ErrorChecker.throwIfNotSet(this.vapp, "VirtualApplication");

            return new VMStartTask(this.vcloud, this.vsphere, this.vm, this.vapp, this.hostid,
                    this.storageid, this.hostname, this.progress);
        }

        @Override
        public final void reset() {
            this.vcloud = null;
            this.vsphere = null;
            this.vm = null;
            this.vapp = null;
            this.hostid = null;
            this.storageid = null;
            this.hostname = "host-" + UUID.randomUUID();
            this.progress = new VmBootStatus();
        }
    }
}
