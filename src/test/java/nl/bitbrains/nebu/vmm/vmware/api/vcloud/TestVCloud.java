package nl.bitbrains.nebu.vmm.vmware.api.vcloud;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.UnmarshalException;

import nl.bitbrains.nebu.common.VirtualMachine;
import nl.bitbrains.nebu.vmm.vmware.api.VMStartTask;
import nl.bitbrains.nebu.vmm.vmware.api.vcloud.FakeSSLSocketFactory;
import nl.bitbrains.nebu.vmm.vmware.api.vcloud.VCloud;
import nl.bitbrains.nebu.vmm.vmware.entity.VirtualApplication;
import nl.bitbrains.nebu.vmm.vmware.exception.NoSuchVMException;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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

import com.vmware.vcloud.api.rest.schema.GuestCustomizationSectionType;
import com.vmware.vcloud.api.rest.schema.RecomposeVAppParamsType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.VirtualNetworkCard;
import com.vmware.vcloud.sdk.admin.extensions.VcloudAdminExtension;
import com.vmware.vcloud.sdk.constants.UndeployPowerActionType;
import com.vmware.vcloud.sdk.constants.VMStatus;
import com.vmware.vcloud.sdk.constants.VappStatus;

@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ VM.class, FakeSSLSocketFactory.class, VMStartTask.Builder.class,
        Organization.class, Vdc.class, Vapp.class })
@PowerMockIgnore({ "javax.management.*" })
public class TestVCloud {

    @Mock
    private VcloudClient vcc;
    @Mock
    private VcloudAdminExtension vae;
    @Mock
    private Vapp vapp, vapp2;
    @Mock
    private VM vm, vm2;
    @Mock
    private ReferenceType rt, rt2;
    @Mock
    private Organization org;
    @Mock
    private Vdc vdc;
    @Mock
    private VirtualNetworkCard vNetworkCard;
    @Mock
    private GuestCustomizationSectionType customizationSection;
    @Mock
    private VirtualMachine vima;
    @Mock
    private VirtualApplication viap;
    @Mock
    private Task task;

    private VCloud vcloud;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(VM.class);
        PowerMockito.mockStatic(FakeSSLSocketFactory.class);
        PowerMockito.mockStatic(VMStartTask.Builder.class);
        MockitoAnnotations.initMocks(this);
        this.vcloud = new VCloud(this.vcc);
    }

    private void mockVappRefIdStatus(final Vapp vapp, final ReferenceType ref, final String id,
            final VappStatus status) {
        Mockito.when(vapp.getReference()).thenReturn(ref);
        Mockito.when(vapp.getVappStatus()).thenReturn(status);
        Mockito.when(ref.getId()).thenReturn(id);
    }

    private void mockVappRefIdStatus(final VM vm, final ReferenceType ref, final String id,
            final VMStatus status) {
        Mockito.when(vm.getReference()).thenReturn(ref);
        Mockito.when(vm.getVMStatus()).thenReturn(status);
        Mockito.when(ref.getId()).thenReturn(id);
    }

    private void mockForCreateVM(final String refName) throws VCloudException {
        PowerMockito.mockStatic(VM.class);
        PowerMockito.mockStatic(Vapp.class);
        PowerMockito.mockStatic(Organization.class);
        PowerMockito.mockStatic(Vdc.class);

        Mockito.when(VM.getVMById(Matchers.any(VcloudClient.class), Matchers.anyString()))
                .thenReturn(this.vm);
        Mockito.when(Vapp.getVappById(Matchers.any(VcloudClient.class), Matchers.anyString()))
                .thenReturn(this.vapp);

        Mockito.when(this.vm.getReference()).thenReturn(this.rt);
        Mockito.when(this.vapp.getReference()).thenReturn(this.rt);

        Mockito.when(this.vapp.recomposeVapp(Matchers.any(RecomposeVAppParamsType.class)))
                .thenReturn(this.task);

        Mockito.when(Organization.getOrganizationByReference(Matchers.eq(this.vcc),
                                                             Matchers.any(ReferenceType.class)))
                .thenReturn(this.org);
        Mockito.when(Vdc.getVdcByReference(Matchers.eq(this.vcc), Matchers.any(ReferenceType.class)))
                .thenReturn(this.vdc);
        Mockito.when(Vapp.getVappByReference(Matchers.eq(this.vcc),
                                             Matchers.any(ReferenceType.class)))
                .thenReturn(this.vapp);
        Mockito.when(this.vapp.getReference()).thenReturn(this.rt);

        final Collection<ReferenceType> refs = new ArrayList<ReferenceType>();
        refs.add(this.rt);
        Mockito.when(this.vcc.getOrgRefs()).thenReturn(refs);
        Mockito.when(this.org.getVdcRefs()).thenReturn(refs);
        Mockito.when(this.vdc.getVappRefs()).thenReturn(refs);

        Mockito.when(this.rt.getName()).thenReturn(refName);

        final List<VM> vmlist = new ArrayList<VM>(1);
        vmlist.add(this.vm);

        Mockito.when(this.vapp.getChildrenVms()).thenReturn(vmlist);
    }

    @Test
    public void testConstructor() {
        Assert.assertSame(this.vcc, this.vcloud.getVirtualCloudClient());
    }

    @Test
    public void testConstructorFakeURL() {
        final String url = "this-is-not-an-url";
        final boolean res = new VCloud().init("", "", url);

        Assert.assertFalse(res);
    }

    @Test
    @Ignore
    public void testRegisterScheme() {
        this.vcloud.init("", "", "");

        Mockito.verify(this.vcc).registerScheme(Matchers.any(String.class),
                                                Matchers.anyInt(),
                                                (SSLSocketFactory) Matchers.any());
    }

    @Test
    public void testRegisterSchemeSSLException() throws KeyManagementException,
            UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        Mockito.when(FakeSSLSocketFactory.getInstance()).thenThrow(new KeyManagementException());

        Assert.assertFalse(this.vcloud.init("", "", ""));
    }

    @Test
    public void testRegisterSchemeLoginException() throws VCloudException {
        Mockito.doThrow(new VCloudException("")).when(this.vcc)
                .login(Matchers.anyString(), Matchers.anyString());

        Assert.assertFalse(this.vcloud.init("", "", ""));
    }

    @Test
    public void testVirtualResourceList() throws VCloudException {
        final List<String> vmlist = this.vcloud.getVirtualResourceList();

        Assert.assertTrue(vmlist.isEmpty());
    }

    private void mockGetVirtualResourceList(final List<VM> vmlist) throws VCloudException {
        PowerMockito.mockStatic(Organization.class);
        PowerMockito.mockStatic(Vdc.class);
        PowerMockito.mockStatic(Vapp.class);

        Mockito.when(Organization.getOrganizationByReference(Matchers.eq(this.vcc),
                                                             Matchers.any(ReferenceType.class)))
                .thenReturn(this.org);
        Mockito.when(Vdc.getVdcByReference(Matchers.eq(this.vcc), Matchers.any(ReferenceType.class)))
                .thenReturn(this.vdc);
        Mockito.when(Vapp.getVappByReference(Matchers.eq(this.vcc),
                                             Matchers.any(ReferenceType.class)))
                .thenReturn(this.vapp);
        Mockito.when(this.vapp.getReference()).thenReturn(this.rt);

        final Collection<ReferenceType> refs = new ArrayList<ReferenceType>();
        refs.add(this.rt);
        Mockito.when(this.vcc.getOrgRefs()).thenReturn(refs);
        Mockito.when(this.org.getVdcRefs()).thenReturn(refs);
        Mockito.when(this.vdc.getVappRefs()).thenReturn(refs);

        Mockito.when(this.vapp.getChildrenVms()).thenReturn(vmlist);
    }

    private void mockGetAllVapps(final int num) throws VCloudException {
        PowerMockito.mockStatic(Organization.class);
        PowerMockito.mockStatic(Vdc.class);
        PowerMockito.mockStatic(Vapp.class);

        Mockito.when(Organization.getOrganizationByReference(Matchers.eq(this.vcc),
                                                             Matchers.any(ReferenceType.class)))
                .thenReturn(this.org);
        Mockito.when(Vdc.getVdcByReference(Matchers.eq(this.vcc), Matchers.any(ReferenceType.class)))
                .thenReturn(this.vdc);
        Mockito.when(Vapp.getVappByReference(Matchers.eq(this.vcc),
                                             Matchers.any(ReferenceType.class)))
                .thenReturn(this.vapp);

        final Collection<ReferenceType> refs = new ArrayList<ReferenceType>();
        final Collection<ReferenceType> vapprefs = new ArrayList<ReferenceType>();
        refs.add(this.rt);
        for (int i = 0; i < num; i++) {
            vapprefs.add(this.rt);
        }
        Mockito.when(this.vcc.getOrgRefs()).thenReturn(refs);
        Mockito.when(this.org.getVdcRefs()).thenReturn(refs);
        Mockito.when(this.vdc.getVappRefs()).thenReturn(vapprefs);
    }

    @Test
    public void testVirtualResourceListMocked() throws VCloudException {
        this.mockGetVirtualResourceList(new ArrayList<VM>());

        Assert.assertEquals(0, this.vcloud.getVirtualResourceList().size());
    }

    @Test(expected = NoSuchVMException.class)
    public void testVirtualMachineInfo() throws NoSuchVMException, VCloudException,
            UnmarshalException {
        final String uuid = "this-uuid-definately-does-not-exist";
        this.vcloud.getVirtualMachineInfo(uuid);
    }

    @Test
    public void testVirtualMachineInfoMocked() throws NoSuchVMException, VCloudException,
            UnmarshalException {
        final String uuid = "this-uuid-is-real";
        final String name = "name";
        final List<VirtualNetworkCard> networkcards = new ArrayList<VirtualNetworkCard>();
        networkcards.add(this.vNetworkCard);

        Mockito.when(this.vm.getNetworkCards()).thenReturn(networkcards);
        Mockito.when(this.vNetworkCard.getIpAddress()).thenReturn("0.0.0.0");
        Mockito.when(this.vm.getReference()).thenReturn(this.rt);
        Mockito.when(this.vm.getGuestCustomizationSection()).thenReturn(this.customizationSection);
        Mockito.when(this.customizationSection.getComputerName()).thenReturn(name);
        Mockito.when(this.rt.getId()).thenReturn(uuid);
        Mockito.when(VM.getVMById(Matchers.any(VcloudClient.class), Matchers.anyString()))
                .thenReturn(this.vm);

        final ArrayList<VM> vmlist = new ArrayList<VM>();
        vmlist.add(this.vm);

        this.mockGetVirtualResourceList(vmlist);
        PowerMockito.mockStatic(VM.class);
        Mockito.when(VM.getVMById(Matchers.eq(this.vcc), Matchers.eq(uuid))).thenReturn(this.vm);

        final VirtualMachine virtualMachine = this.vcloud.getVirtualMachineInfo(uuid);
        Assert.assertNotNull(virtualMachine);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetVmIdFromNameNullIds() throws NoSuchVMException, VCloudException {
        this.vcloud.getVmIdFromName(null, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetVmIdFromNameNullName() throws NoSuchVMException, VCloudException {
        this.vcloud.getVmIdFromName(new ArrayList<String>(), null);
    }

    @Test(expected = NoSuchVMException.class)
    public void testGetVmIdFromNameNoSuchVm() throws NoSuchVMException, VCloudException {
        final String vmname = "vmname";
        this.vcloud.getVmIdFromName(new ArrayList<String>(), vmname);
    }

    @Test(expected = NoSuchVMException.class)
    public void testGetVmIdFromNameNoSuchVmNonEmptyList() throws NoSuchVMException, VCloudException {
        final String vmname = "vmname";
        final String notvmname1 = "not-vmname-1";
        final String notvmname2 = "not-vmname-2";
        final List<String> vmids = new ArrayList<String>();
        vmids.add(notvmname1);
        vmids.add(notvmname2);
        Mockito.when(VM.getVMById(Matchers.any(VcloudClient.class), Matchers.anyString()))
                .thenReturn(this.vm);
        Mockito.when(this.vm.getReference()).thenReturn(this.rt);
        Mockito.when(this.rt.getName()).thenReturn("");
        this.vcloud.getVmIdFromName(vmids, vmname);
    }

    @Test
    public void testGetVmIdFromNameVmNonEmptyList() throws NoSuchVMException, VCloudException {
        final String vmname = "vmname";
        final String notvmname1 = "not-vmname-1";
        final List<String> vmids = new ArrayList<String>();
        vmids.add(notvmname1);
        vmids.add(vmname);
        Mockito.when(VM.getVMById(Matchers.any(VcloudClient.class), Matchers.anyString()))
                .thenReturn(this.vm);
        Mockito.when(VM.getVMById(Matchers.any(VcloudClient.class), Matchers.eq(vmname)))
                .thenReturn(this.vm2);
        Mockito.when(this.vm.getReference()).thenReturn(this.rt);
        Mockito.when(this.vm2.getReference()).thenReturn(this.rt2);
        Mockito.when(this.rt.getName()).thenReturn("");
        Mockito.when(this.rt2.getName()).thenReturn(vmname);
        this.vcloud.getVmIdFromName(vmids, vmname);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetVmIdsFromNamesNullIds() throws NoSuchVMException, VCloudException {
        this.vcloud.getVmIdsFromNames(null, new ArrayList<String>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetVmIdsFromNamesNullNames() throws NoSuchVMException, VCloudException {
        this.vcloud.getVmIdsFromNames(new ArrayList<String>(), null);
    }

    @Test
    public void testGetVmIdsFromNamesEmptyLists() throws NoSuchVMException, VCloudException {
        final List<String> ids = this.vcloud.getVmIdsFromNames(new ArrayList<String>(),
                                                               new ArrayList<String>());

        Assert.assertTrue(ids.isEmpty());
    }

    @Test
    public void testGetVmIdsFromNames() throws NoSuchVMException, VCloudException {
        final String vmname = "vmname";
        final String vmid = "vmid";
        final String notvmname1 = "not-vmname-1";
        final List<String> names = new ArrayList<String>();
        names.add(vmname);
        final List<String> vmids = new ArrayList<String>();
        vmids.add(notvmname1);
        vmids.add(vmid);
        Mockito.when(VM.getVMById(Matchers.any(VcloudClient.class), Matchers.anyString()))
                .thenReturn(this.vm);
        Mockito.when(VM.getVMById(Matchers.any(VcloudClient.class), Matchers.eq(vmid)))
                .thenReturn(this.vm2);
        Mockito.when(this.vm.getReference()).thenReturn(this.rt);
        Mockito.when(this.vm2.getReference()).thenReturn(this.rt2);
        Mockito.when(this.rt.getName()).thenReturn("");
        Mockito.when(this.rt2.getName()).thenReturn(vmname);

        final List<String> ids = this.vcloud.getVmIdsFromNames(vmids, names);

        Assert.assertFalse(ids.isEmpty());
        Assert.assertEquals(vmid, ids.get(0));
    }

    @Test
    public void testNoVapps() throws VCloudException {
        final String id = "id";
        this.mockGetAllVapps(0);
        this.mockVappRefIdStatus(this.vapp, this.rt, id, VappStatus.UNKNOWN);

        final List<VirtualApplication> viaps = this.vcloud.getAllVapps();

        Assert.assertEquals(0, viaps.size());
    }

    @Test
    public void testTwoVapps() throws VCloudException {
        final String id = "id";
        this.mockGetAllVapps(2);
        this.mockVappRefIdStatus(this.vapp, this.rt, id, VappStatus.UNKNOWN);

        final List<VirtualApplication> viaps = this.vcloud.getAllVapps();

        Assert.assertEquals(2, viaps.size());
    }

    @Test
    public void testGetIdsFromVappChildrenVmsException() throws VCloudException {
        Mockito.when(this.vapp.getChildrenVms()).thenThrow(VCloudException.class);

        Assert.assertEquals(0, VCloud.getIdsFromVapp(this.vapp).size());
    }

    @Test
    public void testGetIdsFromVappChildrenVappsException() throws VCloudException {
        Mockito.when(this.vapp.getChildrenVapps()).thenThrow(VCloudException.class);

        Assert.assertEquals(0, VCloud.getIdsFromVapp(this.vapp).size());
    }

    @Test
    public void testGetIdsFromVappNestedVapps() throws VCloudException {
        final List<Vapp> vapplist = new ArrayList<Vapp>(1);
        final List<VM> vmlist = new ArrayList<VM>(1);
        final String id = "id";
        final VMStatus status = VMStatus.UNKNOWN;
        vapplist.add(this.vapp2);
        vmlist.add(this.vm);

        this.mockVappRefIdStatus(this.vm, this.rt, id, status);
        Mockito.when(this.vapp2.getChildrenVms()).thenReturn(vmlist);
        Mockito.when(this.vapp.getChildrenVapps()).thenReturn(vapplist);

        Assert.assertEquals(id, VCloud.getIdsFromVapp(this.vapp).get(0));
    }

    @Test
    public void testCreateVM() throws VCloudException, TimeoutException, NoSuchVMException {
        final String hostname = "hostname";
        this.mockForCreateVM(hostname);

        this.vcloud.createVM(this.vima, this.viap, hostname);
    }

    private void mockForPowerOn(final String refname) throws VCloudException {
        PowerMockito.mockStatic(VM.class);
        Mockito.when(VM.getVMById(Matchers.any(VcloudClient.class), Matchers.anyString()))
                .thenReturn(this.vm);

        Mockito.when(this.vm.getGuestCustomizationSection()).thenReturn(this.customizationSection);
        Mockito.when(this.vm.getReference()).thenReturn(this.rt);
        Mockito.when(this.rt.getName()).thenReturn(refname);
        Mockito.when(this.vm.updateSection(Matchers.eq(this.customizationSection)))
                .thenReturn(this.task);
        Mockito.when(this.vm.powerOn()).thenReturn(this.task);
        Mockito.when(this.vm.reset()).thenReturn(this.task);
    }

    @Test
    public void testPowerOnVMVerifyPowerOn() throws VCloudException {
        final String refname = "name";
        this.mockForPowerOn(refname);

        this.vcloud.powerOnVM("");

        Mockito.verify(this.vm).powerOn();
    }

    @Test
    public void testPowerOnVMVerifyReset() throws VCloudException {
        final String refname = "name";
        this.mockForPowerOn(refname);

        this.vcloud.powerOnVM("");

        Mockito.verify(this.vm).reset();
    }

    @Test
    public void testPowerOnVMVerifyUpdateSection() throws VCloudException {
        final String refname = "name";
        this.mockForPowerOn(refname);

        this.vcloud.powerOnVM("");

        Mockito.verify(this.vm).updateSection(Matchers.eq(this.customizationSection));
    }

    @Test
    public void testPowerOnVMThrowsVCloudException() throws VCloudException {
        final String refname = "name";
        this.mockForPowerOn(refname);
        Mockito.when(VM.getVMById(Matchers.any(VcloudClient.class), Matchers.anyString()))
                .thenThrow(new VCloudException(""));

        this.vcloud.powerOnVM("");
    }

    @Test
    public void testKillVMWhenVMIsNull() {
        this.vcloud.killVM(this.vima);
    }

    private void mockForKillVM() throws VCloudException {
        PowerMockito.mockStatic(VM.class);
        Mockito.when(VM.getVMById(Matchers.any(VcloudClient.class), Matchers.anyString()))
                .thenReturn(this.vm);
        Mockito.when(this.vm.undeploy(Matchers.any(UndeployPowerActionType.class)))
                .thenReturn(this.task);
        Mockito.when(this.vm.delete()).thenReturn(this.task);
    }

    @Test
    public void testKillVMWhenVMThrowsException() throws VCloudException {
        Mockito.when(VM.getVMById(Matchers.any(VcloudClient.class), Matchers.anyString()))
                .thenThrow(new VCloudException(""));

        this.vcloud.killVM(this.vima);
    }

    @Test
    public void testKillVMVerifyUndeploy() throws VCloudException {
        this.mockForKillVM();

        this.vcloud.killVM(this.vima);

        Mockito.verify(this.vm).undeploy(Matchers.any(UndeployPowerActionType.class));
    }

    @Test
    public void testKillVMVerifyDelete() throws VCloudException {
        this.mockForKillVM();

        this.vcloud.killVM(this.vima);

        Mockito.verify(this.vm).delete();
    }

    @Test
    public void testKillVMUndeployThrowsException() throws VCloudException {
        this.mockForKillVM();
        Mockito.when(this.vm.undeploy(Matchers.any(UndeployPowerActionType.class)))
                .thenThrow(new VCloudException(""));

        this.vcloud.killVM(this.vima);

        Mockito.verify(this.vm).undeploy(Matchers.any(UndeployPowerActionType.class));
    }

    @Test
    public void testKillVMDeleteThrowsException() throws VCloudException {
        this.mockForKillVM();
        Mockito.when(this.vm.delete()).thenThrow(new VCloudException(""));

        this.vcloud.killVM(this.vima);

        Mockito.verify(this.vm).delete();
    }
}
