package org.pillarone.riskanalytics.application.ui.parameterization.model

import org.pillarone.riskanalytics.application.ui.util.I18NUtilities
import org.pillarone.riskanalytics.core.components.DynamicComposedComponent
import org.pillarone.riskanalytics.core.simulation.item.parameter.ParameterObjectParameterHolder
import org.pillarone.riskanalytics.core.simulation.item.ParametrizedItem

class ParameterObjectParameterTableTreeNode extends ParameterizationTableTreeNode {


    public ParameterObjectParameterTableTreeNode(String path, ParametrizedItem item) {
        super(path, item);
    }

    public boolean isCellEditable(int column) {
        false
    }

    public void setValueAt(Object value, int column) {

    }

    public Object doGetExpandedCellValue(int column) {
        ""
    }

    public String getDisplayName() {
        String value = super.getDisplayName()
        if (value == null) {
            value = lookUp(value, "")
        }
        return value
    }

    private String lookUp(String value, String tooltip) {
        String displayName
        if (!(parent instanceof DynamicComposedComponent)) {
            ParameterObjectParameterHolder parameter = parametrizedItem.getParameterHoldersForAllPeriods(parameterPath).find { it -> it != null }
            if(!parameter){
                return '>>null<<' //AR-266 prevent gratuitous crash
            }
            displayName = I18NUtilities.findParameterTypeDisplayName(parameter.classifier.class, tooltip)
        }
        return displayName
    }

    public String getToolTip() {
        if (!cachedToolTip) {
            String value = name
            cachedToolTip = lookUp(value, TOOLTIP)
            if (!cachedToolTip)
                cachedToolTip = super.getToolTip()
        }
        return cachedToolTip
    }


}