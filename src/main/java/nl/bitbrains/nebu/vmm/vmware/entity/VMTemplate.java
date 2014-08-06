package nl.bitbrains.nebu.vmm.vmware.entity;

import java.util.List;

import nl.bitbrains.nebu.common.interfaces.Identifiable;

/**
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VMTemplate implements Identifiable {

    /**
     * UUID of this {@link VMTemplate}.
     */
    private String uuid;

    /**
     * Name of this {@link VMTemplate}.
     */
    private String name;

    /**
     * The VM Template id in vCloud.
     */
    private String template;

    /**
     * The ID of the Virtual Application where the vCloud VM Template is
     * located.
     */
    private String scrVApp;

    /**
     * The IDs of the Virtual Application where new VMs from this template are
     * allowed to be launched.
     */
    private List<String> destVApps;

    /**
     * @param uuid
     *            to set.
     * @param name
     *            to set.
     * @param template
     *            to set.
     * @param scrVApp
     *            to set.
     * @param destVApps
     *            to set.
     */
    protected VMTemplate(final String uuid, final String name, final String template,
            final String scrVApp, final List<String> destVApps) {
        this.uuid = uuid;
        this.name = name;
        this.template = template;
        this.scrVApp = scrVApp;
        this.destVApps = destVApps;
    }

    /**
     * Sets the UUID of this {@link VMTemplate}.
     * 
     * @param uuid
     *            The new UUID.
     */
    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getUniqueIdentifier() {
        return this.uuid;
    }

    /**
     * @return the name
     */
    public final String getName() {
        return this.name;
    }

    /**
     * @param name
     *            the name to set
     */
    public final void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the template
     */
    public String getTemplate() {
        return this.template;
    }

    /**
     * @param template
     *            the template to set
     */
    public void setTemplate(final String template) {
        this.template = template;
    }

    /**
     * @return the scrVApp
     */
    public final String getScrVApp() {
        return this.scrVApp;
    }

    /**
     * @param scrVApp
     *            the scrVApp to set
     */
    public final void setScrVApp(final String scrVApp) {
        this.scrVApp = scrVApp;
    }

    /**
     * @return the destVApps
     */
    public List<String> getDestVApps() {
        return this.destVApps;
    }

    /**
     * @param destVApps
     *            the destVApps to set
     */
    public final void setDestVApps(final List<String> destVApps) {
        this.destVApps = destVApps;
    }

}
