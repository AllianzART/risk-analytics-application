package org.pillarone.riskanalytics.application.ui.base.view

import groovy.transform.CompileStatic
import org.pillarone.riskanalytics.application.dataaccess.function.IFunction
import org.pillarone.riskanalytics.application.ui.main.view.RiskAnalyticsMainModel
import org.pillarone.riskanalytics.application.ui.result.view.IFunctionListener

@CompileStatic
abstract public class AbstractModellingFunctionView extends AbstractModellingTreeView implements IFunctionListener {

    AbstractModellingFunctionView(model, RiskAnalyticsMainModel mainModel) {
        super(model, mainModel)
    }

    void refreshNodes() {
        removeColumns()
        addColumns()
        nodeChanged()
    }

    void functionAdded(IFunction function) {
    }

    void functionRemoved(IFunction function) {
    }
}