package org.pillarone.riskanalytics.application.ui.util

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.util.LocaleResources
import org.pillarone.riskanalytics.core.parameter.Parameter
import org.pillarone.riskanalytics.application.ui.base.model.ComponentTableTreeNode
import org.pillarone.riskanalytics.application.ui.base.model.SimpleTableTreeNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterObjectParameterTableTreeNode
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.simulation.item.parameter.ParameterHolder
import org.pillarone.riskanalytics.core.simulation.item.parameter.ParameterObjectParameterHolder

public class I18NUtils {

    static final String PACKET_BUNDLE_FILENAME = "org.pillarone.modelling.packets.I18NPacketResources"

    static final String MODEL_PACKAGE = "models."
    static final Log LOG = LogFactory.getLog(I18NUtils)
    static boolean testMode = false
    static ResourceBundle testResourceBundle = null

    public static findParameterTypeDisplayName(String type) {
        String value = null
        try {
            ResourceBundle bundle = LocaleResources.getBundle(type + "Resources")
            value = bundle.getString("displayName")
        } catch (java.util.MissingResourceException e) {
            LOG.warn("resource for ${type} not found. Key: displayName")
        }
        return value
    }

    public static String findParameterDisplayName(ComponentTableTreeNode componentNode, String subPath) {
        Component component = componentNode.component
        String value = null
        String parmKey = subPath.replaceAll(":", ".")
        try {
            ResourceBundle bundle = findResourceBundle(component.getClass())
            value = bundle.getString(parmKey)
        } catch (java.util.MissingResourceException e) {
            return findParameterDisplayNameBySuperClass(component.getClass().getSuperclass(), parmKey)
        }
        return value
    }

    public static String findDisplayNameByParentComponent(def simpleTableTreeNode, String parmKey) {
        if (simpleTableTreeNode != null && simpleTableTreeNode instanceof SimpleTableTreeNode && simpleTableTreeNode.parent != null && simpleTableTreeNode.parent.properties.keySet().contains("component") && simpleTableTreeNode.parent.component != null)
            return findParameterDisplayNameBySuperClass(simpleTableTreeNode.parent.component.getClass(), parmKey)
        return null

    }

    /**
     * iterate the superclasses and get the resource
     * null if doesn't exist
     */
    public static String findParameterDisplayNameBySuperClass(Class componentClass, String parmKey) {
        String value
        try {
            if (componentClass != null && componentClass.name != "org.pillarone.riskanalytics.core.components.Component") {
                ResourceBundle bundle = findResourceBundle(componentClass)
                value = bundle.getString(parmKey)
            }
        } catch (java.util.MissingResourceException e) {
            Class superClass = componentClass.getSuperclass()
            if (superClass != null && superClass.name != "org.pillarone.riskanalytics.core.components.Component") {
                value = findParameterDisplayNameBySuperClass(superClass, parmKey)
            }
            else {
                LOG.warn("resource for ${componentClass.getSimpleName()} not found. Key: ${parmKey}")
            }
        }
        return value
    }

    public static String findResultParameterDisplayName(def simpleTableTreeNode, String parmKey) {
        String value = null
        if (simpleTableTreeNode instanceof SimpleTableTreeNode && simpleTableTreeNode.parent != null && simpleTableTreeNode.parent instanceof ComponentTableTreeNode) {
            Component component = simpleTableTreeNode.parent.component
            try {
                ResourceBundle bundle = findResourceBundle(component.getClass())
                value = bundle.getString(parmKey)
            } catch (java.util.MissingResourceException e) {
                value = findResultParameterDisplayName(simpleTableTreeNode.parent, parmKey)
            }
        }
        return value
    }


    public static String findParameterDisplayName(ParameterObjectParameterTableTreeNode node, String subPath) {
        ParameterObjectParameterHolder parameter = node.parameter.find {it -> it != null }
        String parameterType = parameter.classifier.getClass().name
        int lastIndex = parameterType.lastIndexOf('.') + 1
        parameterType = parameterType.substring(0, lastIndex) + parameterType.substring(lastIndex, lastIndex + 1).toLowerCase() + parameterType.substring(lastIndex + 1)
        String value = null
        String parmKey = subPath.replaceAll(":", ".")
        try {
            ResourceBundle bundle = LocaleResources.getBundle(parameterType + "Resources")
            value = bundle.getString(parmKey)
        } catch (java.util.MissingResourceException e) {
            LOG.warn("resource for ${parameterType} not found. Key: ${parmKey}")
        }
        return value
    }

    public static String findEnumDisplayName(String enumType, String enumValue) {
        String value
        try {
            ResourceBundle bundle = LocaleResources.getBundle(enumType + "Resources")
            value = bundle.getString(enumValue)
        } catch (java.util.MissingResourceException e) {
            LOG.warn("resource for ${enumType} not found. Key: ${enumValue}")
        }
        return value
    }


    public static String findComponentDisplayNameInModelBundle(String componentPath) {
        String name = null
        String modelName = componentPath[0..(componentPath.indexOf(":") - 1)]
        String componentSubPath = componentPath[(componentPath.indexOf(":") + 1)..(componentPath.length() - 1)]
        componentSubPath = componentSubPath.replaceAll(":", ".")
        try {
            name = getModelResourceBundle(modelName).getString(componentSubPath)
        } catch (java.util.MissingResourceException e) {
            LOG.warn("resource for ${modelName} not found. Key: ${componentSubPath}")
        }
        return name
    }

    public static String findComponentDisplayNameInComponentBundle(Component component) {
        String name = null
        try {
            name = findResourceBundle(component.getClass()).getString("displayName")
        } catch (java.util.MissingResourceException e) {
            LOG.warn("resource for ${component.getClass().getSimpleName()} not found")
        }
        return name
    }

    public static String findComponentDisplayNameByTreeNode(ComponentTableTreeNode node) {
        String name = null
        try {
            if (node instanceof ComponentTableTreeNode && node?.parent instanceof ComponentTableTreeNode)
                name = findResourceBundle(((ComponentTableTreeNode) node?.parent)?.component?.class).getString(node?.name)
        } catch (java.util.MissingResourceException e) {
            LOG.warn("resource for ComponentTableTreeNode  not found")
        }
        return name
    }



    public static String findResultDisplayName(String path, Component component) {
        path = path[(path.indexOf(":") + 1)..(path.length() - 1)]
        String resultKey = path.replaceAll(":", ".")
        String name = null
        try {
            name = findResourceBundle(component.getClass()).getString(resultKey)
        } catch (java.util.MissingResourceException e) {
            LOG.warn("resource for ${component.getClass().getSimpleName()} not found. Key: ${resultKey}")
        }
        return name
    }

    public static String findDisplayNameByPacket(String parmKey) {
        String value = null
        try {
            ResourceBundle bundle = LocaleResources.getBundle(PACKET_BUNDLE_FILENAME)
            value = bundle.getString(parmKey)
        } catch (java.util.MissingResourceException e) {
            LOG.warn("resource for $PACKET_BUNDLE_FILENAME not found. Key: ${parmKey}")
        }
        return value
    }

    private static String findDisplayNameByPacketSuperClass(Class packetClass, String parmKey) {
        String value
        try {
            if (packetClass != null) {
                ResourceBundle bundle = findResourceBundle(packetClass)
                value = bundle.getString(parmKey)
            }
        } catch (java.util.MissingResourceException e) {
            Class superClass = packetClass.getSuperclass()
            if (superClass != null) {
                value = findDisplayNameByPacketSuperClass(superClass, parmKey)
            }
            else {
                LOG.warn("resource for ${packetClass.getSimpleName()} not found. Key: ${parmKey}")
            }
        }
        return value
    }


    protected static ResourceBundle findResourceBundle(Class class2) {
        ResourceBundle bundle
        try {
            bundle = getResourceBundle(class2)
        } catch (java.util.MissingResourceException e) {
            Class superClass = class2.getSuperclass()
            if (superClass != null && class2.getSuperclass().name != "org.pillarone.riskanalytics.core.components.Component") {
                bundle = findResourceBundle(class2.getSuperclass())
            }
            else {
                throw e
            }
        }
        return bundle
    }

    protected static ResourceBundle getModelResourceBundle(String modelName) {
        if (testMode) {
            return testResourceBundle
        }
        String resourceBundleName = MODEL_PACKAGE + modelName[0].toLowerCase() + modelName[1..(modelName.length() - 1)] + "." + modelName + "ModelResources"
        return LocaleResources.getBundle(resourceBundleName)
    }

    protected static ResourceBundle getResourceBundle(Class class2) {
        if (testMode) {
            return testResourceBundle
        }
        String packageName = class2.getPackage().name
        String className = class2.getSimpleName()
        String resourceBundleName = packageName + "." + className[0].toLowerCase(LocaleResources.getLocale()) + className[1..(className.length() - 1)] + "Resources"
        return LocaleResources.getBundle(resourceBundleName)
    }

    public static String formatDisplayName(String value) {
        if (value == null) {
            value = ""
        }

        if (value.startsWith("sub")) {
            value = value.substring(3)
        }
        if (value.startsWith("parm")) {
            value = value.substring(4)
        }
        if (value.startsWith("out")) {
            value = value.substring(3)
        }

        StringBuffer displayNameBuffer = new StringBuffer()

        value.eachWithIndex {String it, int index ->
            if (!it.equals(it.toLowerCase())) {
                if (index > 0) {
                    displayNameBuffer << " " + it.toLowerCase()
                } else {
                    displayNameBuffer << it.toLowerCase()
                }
            } else {
                displayNameBuffer << it
            }
        }
        return displayNameBuffer.toString()
    }

    public static String getPropertyDisplayName(Model model, String propertyName) {
        String value=null
        try {
            ResourceBundle bundle= getResourceBundle(model.class )
            value = bundle.getString(propertyName)
        } catch (Exception ) {
        }
        return value

    }
}
