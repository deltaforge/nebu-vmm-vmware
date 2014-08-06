package nl.bitbrains.nebu.vmm.vmware.entity;

import java.util.ArrayList;
import java.util.List;

import nl.bitbrains.nebu.common.interfaces.IBuilder;
import nl.bitbrains.nebu.common.interfaces.Identifiable;
import nl.bitbrains.nebu.common.util.ErrorChecker;

/**
 * Builder class for the {@link VMTemplate}.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VMTemplateBuilder implements IBuilder<VMTemplate> {

    private String uuid;
    private String name;
    private String template;
    private String scrVApp;
    private List<String> destVApps;

    /**
     * Simple constructor.
     */
    public VMTemplateBuilder() {
        this.reset();
    }

    /**
     * Resets the builder.
     */
    public final void reset() {
        this.uuid = null;
        this.name = null;
        this.template = null;
        this.scrVApp = null;
        this.destVApps = new ArrayList<String>();
    }

    /**
     * @param uuid
     *            to build with.
     * @return this for fluency
     */
    public final VMTemplateBuilder withUuid(final String uuid) {
        ErrorChecker.throwIfNullArgument(uuid, Identifiable.UUID_NAME);
        this.uuid = uuid;
        return this;
    }

    /**
     * @param name
     *            to build with
     * @return this for fluency.
     */
    public final VMTemplateBuilder withName(final String name) {
        ErrorChecker.throwIfNullArgument(name, "name");
        this.name = name;
        return this;
    }

    /**
     * @param template
     *            to build with.
     * @return this for fluency
     */
    public final VMTemplateBuilder withTemplate(final String template) {
        ErrorChecker.throwIfNullArgument(template, "template");
        this.template = template;
        return this;
    }

    /**
     * @param vapp
     *            to build with
     * @return this for fluency.
     */
    public final VMTemplateBuilder withSrcVapp(final String vapp) {
        ErrorChecker.throwIfNullArgument(vapp, "vapp");
        this.scrVApp = vapp;
        return this;
    }

    /**
     * @param vapp
     *            to build with
     * @return this for fluency.
     */
    public final VMTemplateBuilder withDestVapp(final String vapp) {
        ErrorChecker.throwIfNullArgument(vapp, "vapp");
        this.destVApps.add(vapp);
        return this;
    }

    /**
     * @param vapps
     *            List of vCloud Vapp IDs where new VMs of this template are
     *            allowed to be deployed.
     * @return this for fluency.
     */
    public final VMTemplateBuilder withDestVapps(final List<String> vapps) {
        ErrorChecker.throwIfNullArgument(vapps, "vapps");
        for (final String vapp : vapps) {
            this.withDestVapp(vapp);
        }
        return this;
    }

    /**
     * @return the build {@link VMTemplate} object.
     */
    public final VMTemplate build() {
        ErrorChecker.throwIfNotSet(this.uuid, Identifiable.UUID_NAME);
        final VMTemplate template = new VMTemplate(this.uuid, this.name, this.template,
                this.scrVApp, this.destVApps);
        this.reset();
        return template;
    }

}