package org.pillarone.riskanalytics.application.ui.util

import com.ulcjava.base.application.BorderFactory
import com.ulcjava.base.application.ClientContext
import com.ulcjava.base.application.ULCBoxPane
import com.ulcjava.base.application.ULCComponent
import com.ulcjava.base.application.util.ULCIcon
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.image.BufferedImage
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.parameterization.model.EnumParameterizationTableTreeNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.MultiDimensionalParameterizationTableTreeNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterizationClassifierTableTreeNode
import org.pillarone.riskanalytics.application.ui.util.NumberParser
import org.pillarone.riskanalytics.application.util.LocaleResources

class UIUtils {

    private static Log LOG = LogFactory.getLog(UIUtils)

    private static FontMetrics sFontMetrics
    public static String ICON_DIRECTORY = "/org/pillarone/riskanalytics/application/icons/"

    static int calculateTreeWidth(node) {
        return calculateTreeWidth(node, 0)
    }

    static int calculateColumnWidth(node, int columnIndex) {
        return calculateColumnWidth(node, 0, 0, columnIndex)
    }

    static NumberParser getNumberParser() {
        return new NumberParser(ClientContext.locale)
    }


    private static FontMetrics getFontMetrics() {
        if (sFontMetrics == null) {
            BufferedImage bufferedImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_BGR)
            Graphics graphics = bufferedImage.getGraphics()
            sFontMetrics = graphics.getFontMetrics()
        }

        return sFontMetrics
    }

    private static int calculateTreeWidth(node, int columnIndex) {
        return calculateColumnWidth(node, 25, 25, columnIndex)
    }

    protected static int calculateColumnWidth(MultiDimensionalParameterizationTableTreeNode node, initialOffset, offset, int columnIndex) {
        return 0 // this node should not rule for the columnWidth, therefor we return 0
    }

    protected static int calculateColumnWidth(EnumParameterizationTableTreeNode node, initialOffset, offset, int columnIndex) {
        return determineColumnWidthForValues(node)
    }

    protected static int calculateColumnWidth(ParameterizationClassifierTableTreeNode node, initialOffset, offset, int columnIndex) {
        return determineColumnWidthForValues(node)
    }

    protected static int calculateColumnWidth(node, initialOffset, offset, int columnIndex) {
        def nodeMax = 0

        (0..<node.childCount).each {
            def childMax = calculateColumnWidth(node.getChildAt(it), initialOffset, initialOffset + offset, columnIndex)
            nodeMax = Math.max(childMax, nodeMax)
        }

        int columnWidth = 0

        def value = node.getValueAt(columnIndex)

        if (value instanceof List) {
            columnWidth = 0
        } else {
            columnWidth = getFontMetrics().stringWidth(value.toString()) + offset
        }
        return Math.max(columnWidth, nodeMax)
    }

    private static int determineColumnWidthForValues(node) {
        int columnWidth = 0
        node.values.each {
            int enumMax = getFontMetrics().stringWidth(it.toString())
            columnWidth = Math.max(columnWidth, enumMax)
        }
        return columnWidth
    }

    public static ULCIcon getIcon(String fileName) {
        URL url = new UIUtils().class.getResource(ICON_DIRECTORY + fileName)
        if (url) {
            return new ULCIcon(url)
        }
    }


    public static ULCBoxPane spaceAround(ULCComponent comp, int top, int left, int bottom, int right) {
        ULCBoxPane deco = new ULCBoxPane()
        deco.border = BorderFactory.createEmptyBorder(top, left, bottom, right)
        deco.add comp
        return deco
    }

    public static boolean isUnixOs() {
        String osName = ClientContext.getSystemProperty("os.name").toUpperCase()
        return "UNIX" == osName || "LINUX" == osName || "SOLARIS" == osName
    }

    public static Locale getClientLocale() {
        Locale locale = Locale.default

        try {
            locale = ClientContext.locale
        } catch (Exception e) {
            LOG.warn "Unable to detect client locale. Using default."
        }
        return locale
    }

    public static final  String getText(Class objClass, String key) {
        return LocaleResources.getString(objClass.simpleName+"." + key);
    }


}