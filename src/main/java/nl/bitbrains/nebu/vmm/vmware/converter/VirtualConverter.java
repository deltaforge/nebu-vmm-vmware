package nl.bitbrains.nebu.vmm.vmware.converter;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.common.VirtualMachineBuilder;
import nl.bitbrains.nebu.common.VirtualMachine.Status;
import nl.bitbrains.nebu.vmm.vmware.entity.VirtualApplication;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.constants.VMStatus;
import com.vmware.vcloud.sdk.constants.VappStatus;

/**
 * Helper class to convert VMware {@link VM} objects to Nebu
 * {@link VirtualMachine} objects.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public final class VirtualConverter {

    /**
     * Logger for this class.
     */
    private static Logger logger = LogManager.getLogger();

    /**
     * Private constructor. This class only has static methods.
     */
    private VirtualConverter() {
    }

    /**
     * Creates a {@link VirtualMachine} from a {@link VM}.
     * 
     * @param vm
     *            The {@link VM} that goes in.
     * @return The {@link VirtualMachine} that goes out.
     */
    public static VirtualMachine fromVM(final VM vm) {
        // TODO Should we add some actual data to the VM?
        final VirtualMachine vima = new VirtualMachineBuilder()
                .withUuid(vm.getReference().getId()).build();
        try {
            vima.setHostname(vm.getGuestCustomizationSection().getComputerName());
        } catch (final VCloudException e) {
            VirtualConverter.logger.catching(Level.WARN, e);
        }
        vima.setStatus(vm.getVMStatus() == VMStatus.POWERED_ON ? VirtualMachine.Status.ON
                : VirtualMachine.Status.OFF);
        return vima;
    }

    /**
     * Translates a {@link Vapp} to a {@link VirtualApplication}.
     * 
     * @param vapp
     *            The {@link Vapp}.
     * @return The new matching {@link VirtualApplication}.
     */
    public static VirtualApplication vApp2VirtualApplication(final Vapp vapp) {
        final VirtualApplication result = new VirtualApplication(vapp.getReference().getId());
        result.setStatus(VirtualConverter.vAppStatus2VirtualApplicationStatus(vapp.getVappStatus()));
        result.setName(vapp.getReference().getName());
        return result;
    }

    /**
     * Translates a {@link VappStatus} to a {@link Status}.
     * 
     * @param status
     *            The {@link VappStatus}.
     * @return A new matching {@link Status}
     */
    private static VirtualApplication.Status vAppStatus2VirtualApplicationStatus(
            final VappStatus status) {
        VirtualApplication.Status result;
        switch (status) {
        case POWERED_ON:
            result = VirtualApplication.Status.ON;
            break;
        case POWERED_OFF:
            result = VirtualApplication.Status.OFF;
            break;
        default:
            result = VirtualApplication.Status.UNKNOWN;
        }
        return result;
    }

    /**
     * Creates the name of an objects in vSphere based on the name and ID in
     * vCloud.
     * 
     * @param vcloudname
     *            The name of the object in vCloud.
     * @param vcloudid
     *            The ID of the object in vCloud.
     * @return The name of the object in vSphere.
     */
    public static String buildVsphereName(final String vcloudname, final String vcloudid) {
        final String[] idparts = vcloudid.split(":");
        return vcloudname + " (" + idparts[idparts.length - 1] + ")";
    }
}
