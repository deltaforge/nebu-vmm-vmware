package nl.bitbrains.nebu.vmm.vmware.converter;

import java.util.ArrayList;
import java.util.List;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.vmm.vmware.converter.VirtualConverter;
import nl.bitbrains.nebu.vmm.vmware.entity.VirtualApplication;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vmware.vcloud.api.rest.schema.GuestCustomizationSectionType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VirtualNetworkCard;
import com.vmware.vcloud.sdk.constants.VMStatus;
import com.vmware.vcloud.sdk.constants.VappStatus;

public class TestVirtualMachineConverter {

    @Mock
    VM vm;
    @Mock
    ReferenceType rt;
    @Mock
    VM vm1, vm2, vm3;
    @Mock
    Vapp vapp1, vapp2, vapp3;
    @Mock
    VirtualNetworkCard vNetworkCard;
    @Mock
    GuestCustomizationSectionType customizationSection;
    @Mock
    Vapp vapp;

    private List<VirtualNetworkCard> networkcards;

    final String test = "this-string-is-a-unique-id";

    @Before
    public void setUp() throws VCloudException {
        MockitoAnnotations.initMocks(this);
        this.networkcards = new ArrayList<VirtualNetworkCard>(1);
        this.networkcards.add(this.vNetworkCard);
        Mockito.when(this.vm.getNetworkCards()).thenReturn(this.networkcards);
        Mockito.when(this.vNetworkCard.getIpAddress()).thenReturn("");
    }

    public void mockGuestCustomization(final VM vm) throws VCloudException {
        Mockito.when(vm.getGuestCustomizationSection()).thenReturn(this.customizationSection);
        Mockito.when(this.customizationSection.getComputerName()).thenReturn("");
    }

    public void mockGuestCustomizationException(final VM vm) throws VCloudException {
        Mockito.when(vm.getGuestCustomizationSection()).thenThrow(VCloudException.class);
    }

    public void mockVMRefId(final VM vm, final ReferenceType ref, final String id) {
        Mockito.when(this.vm.getReference()).thenReturn(ref);
        Mockito.when(ref.getId()).thenReturn(id);
    }

    public void mockVMStatus(final VM vm, final VMStatus status) {
        Mockito.when(vm.getVMStatus()).thenReturn(status);
    }

    public void mockVappRefId(final Vapp vapp, final ReferenceType ref, final String id) {
        Mockito.when(this.vapp.getReference()).thenReturn(ref);
        Mockito.when(ref.getId()).thenReturn(id);
    }

    public void mockVappRefName(final Vapp vapp, final ReferenceType ref, final String name) {
        Mockito.when(this.vapp.getReference()).thenReturn(ref);
        Mockito.when(ref.getName()).thenReturn(name);
    }

    public void mockVappStatus(final Vapp vapp, final VappStatus status) {
        Mockito.when(this.vapp.getVappStatus()).thenReturn(status);
    }

    @Test(expected = NullPointerException.class)
    public void testNullInput() {
        VirtualConverter.fromVM(null);
    }

    @Test
    public void testGetVmReference() throws VCloudException {
        this.mockGuestCustomization(this.vm);
        final String id = "id";
        this.mockVMRefId(this.vm, this.rt, id);

        VirtualConverter.fromVM(this.vm);
        Mockito.verify(this.vm).getReference();
    }

    @Test
    public void testGetVmReferenceId() throws VCloudException {
        this.mockGuestCustomization(this.vm);
        Mockito.when(this.vm.getReference()).thenReturn(this.rt);
        Mockito.when(this.rt.getId()).thenReturn(this.test);

        final VirtualMachine vima = VirtualConverter.fromVM(this.vm);

        Assert.assertEquals(this.test, vima.getUniqueIdentifier());
    }

    @Test
    public void testVMNoHostName() throws VCloudException {
        final String id = "id";
        this.mockVMRefId(this.vm, this.rt, id);
        this.mockGuestCustomizationException(this.vm);
        final VirtualMachine vima = VirtualConverter.fromVM(this.vm);
        Assert.assertNotNull(vima);
    }

    @Test
    public void testVMPoweredOn() throws VCloudException {
        final String id = "id";
        this.mockVMRefId(this.vm, this.rt, id);
        this.mockGuestCustomizationException(this.vm);
        this.mockVMStatus(this.vm, VMStatus.POWERED_ON);
        final VirtualMachine vima = VirtualConverter.fromVM(this.vm);

        Assert.assertEquals(VirtualMachine.Status.ON, vima.getStatus());
    }

    @Test
    public void testVMPoweredOff() throws VCloudException {
        final String id = "id";
        this.mockVMRefId(this.vm, this.rt, id);
        this.mockGuestCustomizationException(this.vm);
        this.mockVMStatus(this.vm, VMStatus.POWERED_OFF);
        final VirtualMachine vima = VirtualConverter.fromVM(this.vm);

        Assert.assertEquals(VirtualMachine.Status.OFF, vima.getStatus());
    }

    @Test
    public void testVappToVirtualApplicationId() {
        final String id = "id";
        this.mockVappRefId(this.vapp, this.rt, id);
        this.mockVappStatus(this.vapp, VappStatus.UNKNOWN);

        final VirtualApplication viap = VirtualConverter.vApp2VirtualApplication(this.vapp);

        Assert.assertEquals(id, viap.getUniqueIdentifier());
    }

    @Test
    public void testVappToVirtualApplicationName() {
        final String name = "name";
        this.mockVappRefId(this.vapp, this.rt, name);
        this.mockVappStatus(this.vapp, VappStatus.UNKNOWN);
        this.mockVappRefName(this.vapp, this.rt, name);

        final VirtualApplication viap = VirtualConverter.vApp2VirtualApplication(this.vapp);

        Assert.assertEquals(name, viap.getName());
    }

    @Test
    public void testVappToVirtualApplicationStatusOn() {
        final VappStatus status = VappStatus.POWERED_ON;
        final String id = "id";
        this.mockVappRefId(this.vapp, this.rt, id);
        this.mockVappStatus(this.vapp, status);

        final VirtualApplication viap = VirtualConverter.vApp2VirtualApplication(this.vapp);

        Assert.assertEquals(VirtualApplication.Status.ON, viap.getStatus());
    }

    @Test
    public void testVappToVirtualApplicationStatusOff() {
        final VappStatus status = VappStatus.POWERED_OFF;
        final String id = "id";
        this.mockVappRefId(this.vapp, this.rt, id);
        this.mockVappStatus(this.vapp, status);

        final VirtualApplication viap = VirtualConverter.vApp2VirtualApplication(this.vapp);

        Assert.assertEquals(VirtualApplication.Status.OFF, viap.getStatus());
    }

    @Test
    public void testVappToVirtualApplicationStatusUnknown() {
        final VappStatus status = VappStatus.UNKNOWN;
        final String id = "id";
        this.mockVappRefId(this.vapp, this.rt, id);
        this.mockVappStatus(this.vapp, status);

        final VirtualApplication viap = VirtualConverter.vApp2VirtualApplication(this.vapp);

        Assert.assertEquals(VirtualApplication.Status.UNKNOWN, viap.getStatus());
    }
}
