package nl.bitbrains.nebu.vmm.vmware.entity;

import java.text.ParseException;
import java.util.List;

import nl.bitbrains.nebu.common.factories.IdentifiableFactory;
import nl.bitbrains.nebu.common.util.xml.XMLFactory;

import org.jdom2.Attribute;
import org.jdom2.Element;

/**
 * Converts {@link VMTemplate} objects to and from XML.
 * 
 * @author Jesse Donkervliet, Tim Hegeman, and Stefan Hugtenburg
 * 
 */
public class VMTemplateFactory extends IdentifiableFactory implements XMLFactory<VMTemplate> {

    public static final String TAG_ELEMENT_ROOT = "vmm";
    public static final String ATTRIBUTE_TYPE = "type";
    public static final String ATTRIBUTE_TYPE_VMWARE_VALUE = "vmware";
    public static final String TAG_NAME = "name";
    public static final String TAG_TEMPLATE = "template";
    public static final String TAG_SCRVAPP = "scrvapp";
    public static final String TAG_DESVAPP = "desvapps";
    public static final String TAG_VAPP = "vapp";

    /**
     * Empty default constructor.
     */
    public VMTemplateFactory() {

    }

    /**
     * Converts the {@link VMTemplate} to XML.
     * 
     * @param object
     *            to convert to XML.
     * @return the created XML element.
     */
    public final Element toXML(final VMTemplate object) {
        final Element elem = super.createRootXMLElement(object, VMTemplateFactory.TAG_ELEMENT_ROOT);
        elem.setAttribute(VMTemplateFactory.ATTRIBUTE_TYPE,
                          VMTemplateFactory.ATTRIBUTE_TYPE_VMWARE_VALUE);
        elem.addContent(new Element(VMTemplateFactory.TAG_NAME).setText(object.getName()));
        elem.addContent(new Element(VMTemplateFactory.TAG_TEMPLATE).setText(object.getTemplate()));
        elem.addContent(new Element(VMTemplateFactory.TAG_SCRVAPP)
                .setAttribute(IdentifiableFactory.TAG_ID, object.getScrVApp()));
        final Element depvapps = new Element(VMTemplateFactory.TAG_DESVAPP);
        for (final String vapp : object.getDestVApps()) {
            depvapps.addContent(new Element(VMTemplateFactory.TAG_VAPP)
                    .setAttribute(IdentifiableFactory.TAG_ID, vapp));
        }
        elem.addContent(depvapps);
        return elem;
    }

    /**
     * Creates a {@link VMTemplate} from XML.
     * 
     * @param xml
     *            element to base the object on.
     * @return the created {@link VMTemplate}.
     * @throws ParseException
     *             if XML is not valid.
     */
    public final VMTemplateBuilder fromXML(final Element xml) throws ParseException {
        super.throwIfInvalidRoot(xml, VMTemplateFactory.TAG_ELEMENT_ROOT);
        final Attribute idAttribute = xml.getAttribute(IdentifiableFactory.TAG_ID);
        final VMTemplateBuilder builder = new VMTemplateBuilder();
        if (idAttribute != null) {
            builder.withUuid(idAttribute.getValue());
        }

        builder.withName(xml.getChildTextTrim(VMTemplateFactory.TAG_NAME))
                .withTemplate(xml.getChildTextTrim(VMTemplateFactory.TAG_TEMPLATE))
                .withSrcVapp(xml.getChild(VMTemplateFactory.TAG_SCRVAPP)
                        .getAttributeValue(IdentifiableFactory.TAG_ID));
        final Element desvapp = xml.getChild(VMTemplateFactory.TAG_DESVAPP);
        final List<Element> desvappchildren = desvapp.getChildren(VMTemplateFactory.TAG_VAPP);
        for (final Element vapp : desvappchildren) {
            builder.withDestVapp(vapp.getAttributeValue(IdentifiableFactory.TAG_ID));
        }
        return builder;
    }
}
