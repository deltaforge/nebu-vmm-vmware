package nl.bitbrains.nebu.vmm.vmware.api.vcloud;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.util.ErrorChecker;
import nl.bitbrains.nebu.vmm.vmware.converter.VirtualConverter;
import nl.bitbrains.nebu.vmm.vmware.entity.VirtualApplication;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vmware.vcloud.api.rest.schema.GuestCustomizationSectionType;
import com.vmware.vcloud.api.rest.schema.RecomposeVAppParamsType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.SourcedCompositionItemParamType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VCloudRuntimeException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.constants.UndeployPowerActionType;
import com.vmware.vcloud.sdk.constants.Version;

/**
 * 
 * This class is responsible for retrieving information from VMware via its Java
 * API. It can retrieve information about virtual machines in the data center.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VCloud {

    /**
     * The {@link Logger} for this object.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * The port to connect to.
     */
    private static int port = 443;

    /**
     * Lock to prevent concurrent VM deployment. vCloud does not support
     * concurrent deployment. Removing this lock will break deployment of
     * multiple machines.
     */
    public static final Lock LOCK = new ReentrantLock(true);

    /**
     * The {@link VcloudClient} that connects to the vCloud REST API.
     */
    private VcloudClient vcc;

    /**
     * Create a new {@link VCloud}.
     */
    public VCloud() {
        VcloudClient.setLogLevel(java.util.logging.Level.WARNING);
        // TODO Do we want to make version configurable? We are currently using
        // the 5.1 version of the VMware SDK jar.

    }

    /**
     * Create a new VMware.
     * 
     * @param vcc
     *            The {@link VcloudClient} that this {@link VCloud} should use.
     */
    public VCloud(final VcloudClient vcc) {
        VcloudClient.setLogLevel(java.util.logging.Level.OFF);
        this.vcc = vcc;
    }

    /**
     * Sets up the connection to vCloud. Keeps the vCloud connection alive.
     * Re-established connection after connection is dropped.
     * 
     * @param username
     *            The username used for vCloud login.
     * @param password
     *            The password used for vCloud login.
     * @param vcloudurl
     *            The URL where the vCloud REST API resides.
     * 
     * @return <code>true</code> iff the login was successful.
     */
    public boolean initAndKeepAlive(final String username, final String password,
            final String vcloudurl) {
        final boolean result = this.init(username, password, vcloudurl);
        final VCloudKeepAlive keepAlive = new VCloudKeepAlive(this, username, password, vcloudurl);
        new Thread(keepAlive).start();
        return result;
    }

    /**
     * Sets up the connection to vCloud.
     * 
     * @param username
     *            The username used for vCloud login.
     * @param password
     *            The password used for vCloud login.
     * @param vcloudurl
     *            The URL where the vCloud REST API resides.
     * 
     * @return <code>true</code> iff the login was successful.
     */
    public boolean init(final String username, final String password, final String vcloudurl) {
        VCloud.logger.debug("Logging in at vCloud at address {}.", vcloudurl);
        try {
            this.vcc = new VcloudClient(vcloudurl, Version.V5_1);
            this.vcc.registerScheme("https", VCloud.port, FakeSSLSocketFactory.getInstance());
        } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException
                | KeyStoreException | VCloudRuntimeException e1) {
            VCloud.logger.catching(Level.ERROR, e1);
            return false;
        }
        try {
            this.vcc.login(username, password);
        } catch (final VCloudException e) {
            VCloud.logger.catching(Level.ERROR, e);
            return false;
        }
        return true;
    }

    /**
     * Returns a list of all {@link Vapp}s visible to the user.
     * 
     * @return A list of {@link Vapp}s
     * @throws VCloudException
     *             When VMware objects cannot be found by their references.
     */
    public List<VirtualApplication> getAllVapps() throws VCloudException {
        final List<Vapp> resources = this.getAllVCloudVapps();
        final List<VirtualApplication> virtapps = new ArrayList<VirtualApplication>(
                resources.size());
        for (final Vapp vapp : resources) {
            virtapps.add(VirtualConverter.vApp2VirtualApplication(vapp));
        }
        return virtapps;
    }

    /**
     * 
     * @return A list of {@link VirtualMachine} uuids.
     * @throws VCloudException
     *             When things go seriously wrong with VMware.
     */
    public List<String> getVirtualResourceList() throws VCloudException {
        // TODO Make the VM search more restricted.
        final List<String> resources = new ArrayList<String>();

        for (final Vapp vapp : this.getAllVCloudVapps()) {
            resources.addAll(VCloud.getIdsFromVapp(vapp));
        }
        return resources;
    }

    /**
     * Retrieves a list of all {@link Vapp}s available to the logged in vCloud
     * used.
     * 
     * @return A list of {@link Vapp}s.
     * @throws VCloudException
     *             When an error occurs in the vCloud API.
     */
    List<Vapp> getAllVCloudVapps() throws VCloudException {
        final List<Vapp> resources = new ArrayList<Vapp>();
        final Collection<ReferenceType> orgrefs = this.vcc.getOrgRefs();
        for (final ReferenceType orgref : orgrefs) {
            final Collection<ReferenceType> vdcrefs = Organization
                    .getOrganizationByReference(this.vcc, orgref).getVdcRefs();
            for (final ReferenceType vdcref : vdcrefs) {
                final Collection<ReferenceType> vapprefs = Vdc.getVdcByReference(this.vcc, vdcref)
                        .getVappRefs();
                for (final ReferenceType vappref : vapprefs) {
                    resources.add(Vapp.getVappByReference(this.vcc, vappref));
                }
            }
        }
        return resources;
    }

    /**
     * Returns a list of all {@link VM} ids that are located underneath the
     * given {@link Vapp}. The search is recursive.
     * 
     * @param vappByReference
     *            The root {@link Vapp} to search for {@link VM}s.
     * @return The complete list of {@link VM} ids.
     */
    public static List<String> getIdsFromVapp(final Vapp vappByReference) {
        final List<String> res = new ArrayList<String>();

        if (vappByReference.getReference() != null) {
            VCloud.logger.debug("Getting VM IDs from vApp {}.", vappByReference.getReference()
                    .getId());
        } else {
            VCloud.logger.warn("vApp {} has no reference object.", vappByReference);
        }

        List<VM> childVMs = null;
        List<Vapp> childVapps = null;

        try {
            childVMs = vappByReference.getChildrenVms();
        } catch (final VCloudException e) {
            // TODO can we safely ignore this?
        }
        try {
            childVapps = vappByReference.getChildrenVapps();
        } catch (final VCloudException e) {
            // TODO can we safely ignore this?
        }

        if (childVMs != null) {
            for (final VM vm : childVMs) {
                VCloud.logger.debug("Found VM with ID: {}.", vm.getReference().getId());
                res.add(vm.getReference().getId());
            }
        }

        if (childVapps != null) {
            for (final Vapp vapp : childVapps) {
                res.addAll(VCloud.getIdsFromVapp(vapp));
            }
        }

        return res;
    }

    /**
     * Extracts a list of {@link VM} {@link ReferenceType} IDs from a list of
     * {@link VM} {@link ReferenceType} names.
     * 
     * @param vmIds
     *            The list of all {@link VM} IDs.
     * @param vmNames
     *            The list of {@link VM} names from which the IDs should be
     *            found.
     * @return A list containing the IDs of all {@link VM}s in the names list.
     * @throws NoSuchVMException
     *             When one of the {@link VM}s from the names list does not have
     *             an ID that is present in the ID list.
     * @throws VCloudException
     *             When stuff goes wrong with vCloud.
     */
    public List<String> getVmIdsFromNames(final List<String> vmIds, final List<String> vmNames)
            throws NoSuchVMException, VCloudException {
        ErrorChecker.throwIfNullArgument(vmIds, "vmIds");
        ErrorChecker.throwIfNullArgument(vmNames, "vmNames");
        final List<String> newNames = new ArrayList<String>(vmNames.size());
        for (final String name : vmNames) {
            newNames.add(this.getVmIdFromName(vmIds, name));
        }
        return newNames;
    }

    /**
     * Extracts the ID of a {@link VM} from a list of {@link VM} IDs.
     * 
     * @param vmIds
     *            The list of all {@link VM} IDs.
     * @param vmName
     *            The name of the {@link VM} whose ID should be found.
     * @return The ID of the {@link VM} whose name was given.
     * @throws NoSuchVMException
     *             When the ID of the VM that corresponds with the given name
     *             could not be found.
     * @throws VCloudException
     *             When stuff goes wrong with vCloud.
     */
    public String getVmIdFromName(final List<String> vmIds, final String vmName)
            throws NoSuchVMException, VCloudException {
        ErrorChecker.throwIfNullArgument(vmIds, "vmIds");
        ErrorChecker.throwIfNullArgument(vmName, "vmName");
        for (final String vmId : vmIds) {
            VM vm = null;
            vm = VM.getVMById(this.vcc, vmId);
            if (vm.getReference().getName().equals(vmName)) {
                return vmId;
            }
        }
        throw new NoSuchVMException("VM with name " + vmName + " not found");
    }

    /**
     * Finds the {@link VirtualMachine} that corresponds with the given UUID
     * {@link String}.
     * 
     * @param uuid
     *            The UUID of the {@link VirtualMachine} that should be
     *            retrieved.
     * @return A {@link VirtualMachine} whose UUID matches the given uuid.
     * @throws NoSuchVMException
     *             if the given UUID does not correspond with a known
     *             {@link VirtualMachine}.
     * @throws VCloudException
     *             When things go seriously wrong with VMware.
     */
    public VirtualMachine getVirtualMachineInfo(final String uuid) throws VCloudException,
            NoSuchVMException {
        if (this.getVirtualResourceList().contains(uuid)) {
            final VM vm = VM.getVMById(this.vcc, uuid);
            return VirtualConverter.fromVM(vm);
        }
        throw new NoSuchVMException("Cannot find requested VM");
    }

    /**
     * @return The {@link VcloudClient}.
     */
    public VcloudClient getVirtualCloudClient() {
        return this.vcc;
    }

    /**
     * Wrapper function to simplify creating a new VM.
     * 
     * @param vm
     *            The {@link VirtualMachine} object that represents the VM that
     *            should be copied.
     * @param virtualApp
     *            The {@link VirtualApplication} object that represents the
     *            destination vApp of the new VM.
     * @param hostname
     *            The hostname the new VM should have.
     * @return The vCloud ID of the new VM.
     * @throws VCloudException
     *             If an error occurs in the vCloud API.
     * @throws TimeoutException
     *             If a timeout occurs while waiting for a vCloud API task.
     * @throws NoSuchVMException
     *             When the ID of the new VM cannot be found.
     */
    public String createVM(final VirtualMachine vm, final VirtualApplication virtualApp,
            final String hostname) throws VCloudException, TimeoutException, NoSuchVMException {
        final ReferenceType vmRef = this.getVMFromVirtualMachine(vm).getReference();
        final Vapp vapp = this.getVappFromVirtualApplication(virtualApp);
        this.modifyVapp(vmRef, vapp, hostname);
        return this.findNewVmId(hostname);
    }

    /**
     * Recomposes The {@link Vapp} to include a new virtual machine.
     * 
     * @param vmRef
     *            A {@link ReferenceType} to the new virtual machine.
     * @param vapp
     *            The {@link Vapp} to recompose.
     * @param hostname
     *            The hostname of the new virtual machine.
     * @throws VCloudException
     *             When an error occurs in the vCloud API.
     * @throws TimeoutException
     *             When a timeout occurs.
     */
    synchronized void modifyVapp(final ReferenceType vmRef, final Vapp vapp, final String hostname)
            throws VCloudException, TimeoutException {
        final SourcedCompositionItemParamType vmItem = new SourcedCompositionItemParamType();
        vmRef.setName(hostname);
        vmItem.setSource(vmRef);
        final RecomposeVAppParamsType recomposeType = new RecomposeVAppParamsType();
        final List<SourcedCompositionItemParamType> newItems = recomposeType.getSourcedItem();
        newItems.add(vmItem);

        vapp.recomposeVapp(recomposeType).waitForTask(0);

    }

    /**
     * Find the id of the virtual machine with the given name.
     * 
     * @param hostname
     *            The hostname of the virtual machine.
     * @return The vCloud if of the virtual machine.
     * @throws VCloudException
     *             When an error occurs in the vCloud API.
     * @throws NoSuchVMException
     *             When no matching virtual machine can be found.
     */
    String findNewVmId(final String hostname) throws VCloudException, NoSuchVMException {
        List<String> ids = null;
        ids = this.getVirtualResourceList();
        String res;
        res = this.getVmIdFromName(ids, hostname);
        return res;
    }

    /**
     * Powers on the virtual machine with the given ID.
     * 
     * @param res
     *            The vCloud ID of the virtual machine.
     */
    public void powerOnVM(final String res) {
        try {
            final VM vm = VM.getVMById(this.getVirtualCloudClient(), res);
            final GuestCustomizationSectionType customization = vm.getGuestCustomizationSection();
            customization.setEnabled(true);
            customization.setComputerName(vm.getReference().getName());
            vm.updateSection(customization).waitForTask(0);
            vm.powerOn().waitForTask(0);
            vm.reset().waitForTask(0);
        } catch (final VCloudException | TimeoutException e) {
            VCloud.logger.catching(Level.WARN, e);
        }
    }

    /**
     * Translates a {@link VirtualApplication} to a {@link Vapp}.
     * 
     * @param dest
     *            The {@link VirtualApplication}.
     * @return The matching {@link Vapp}.
     * @throws VCloudException
     *             When an error occurs in the vCloud API.
     */
    Vapp getVappFromVirtualApplication(final VirtualApplication dest) throws VCloudException {
        return Vapp.getVappById(this.vcc, dest.getUniqueIdentifier());
    }

    /**
     * Translates a {@link VirtualMachine} to a {@link VM}.
     * 
     * @param vm
     *            The {@link VirtualMachine}.
     * @return The matching {@link VM}.
     * @throws VCloudException
     *             When an error occurs in the vCloud API.
     */
    VM getVMFromVirtualMachine(final VirtualMachine vm) throws VCloudException {
        return VM.getVMById(this.vcc, vm.getUniqueIdentifier());
    }

    /**
     * @return the vcc
     */
    public VcloudClient getVcc() {
        return this.vcc;
    }

    /**
     * Attempts to turn off a VM. Uses a {@link ReentrantLock} to prevent
     * multiple vApp changes at the same time.
     * 
     * @param vm
     *            The {@link VirtualMachine} to turn off.
     */
    public void killVM(final VirtualMachine vm) {
        VCloud.LOCK.lock();
        try {
            this.killVMNoLock(vm);
        } finally {
            VCloud.LOCK.unlock();
        }
    }

    /**
     * Attempts to turn off a VM.
     * 
     * @param vm
     *            The {@link VirtualMachine} to turn off.
     */
    private void killVMNoLock(final VirtualMachine vm) {
        VM vcloudVM = null;
        try {
            vcloudVM = this.getVMFromVirtualMachine(vm);
        } catch (final VCloudException e) {
            VCloud.logger.catching(e);
        }
        if (vcloudVM != null) {
            VCloud.logger.trace("Stabbing VM\t{} to DEATH.", vm.getUniqueIdentifier());
            try {
                vcloudVM.undeploy(UndeployPowerActionType.POWEROFF).waitForTask(0);
            } catch (final VCloudException | TimeoutException e) {
                VCloud.logger.warn("It was not very effective...");
            }
            try {
                vcloudVM.delete().waitForTask(0);
            } catch (VCloudException | TimeoutException e) {
                VCloud.logger.info("Deleting VM {}.", vm.getUniqueIdentifier());
            }
        }
    }
}
