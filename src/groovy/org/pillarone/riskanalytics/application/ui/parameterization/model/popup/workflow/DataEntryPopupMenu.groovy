package org.pillarone.riskanalytics.application.ui.parameterization.model.popup.workflow

import com.ulcjava.base.application.ULCTableTree
import org.pillarone.riskanalytics.application.ui.main.action.workflow.RejectWorkflowAction
import org.pillarone.riskanalytics.application.ui.main.action.workflow.SendToProductionAction
import org.pillarone.riskanalytics.application.ui.main.action.workflow.SendToReviewAction
import org.pillarone.riskanalytics.application.ui.main.view.EnabledCheckingMenuItem
import org.pillarone.riskanalytics.application.ui.parameterization.model.popup.ParameterizationPopupMenu
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterizationNode
import org.pillarone.riskanalytics.core.util.Configuration

/**
 * Allianz Risk Transfer  ATOM
 * User: bzetterstrom
 */
class DataEntryPopupMenu extends ParameterizationPopupMenu {
    private final static boolean allowDataEntryToProduction =
        Configuration.coreGetAndLogStringConfig("AllowDataEntryToProduction","true").equalsIgnoreCase("true")

    DataEntryPopupMenu(final ULCTableTree tree, ParameterizationNode node) {
        super(tree, node)
    }

    @Override
    protected boolean hasDeleteAction() { return true }

    @Override
    protected boolean addMenuItemsForWorkflowState(ULCTableTree tree, ParameterizationNode node) {
        add(new EnabledCheckingMenuItem(new SendToReviewAction(tree)));
        if( allowDataEntryToProduction ){
            add(new EnabledCheckingMenuItem(new SendToProductionAction(tree)));
        }
        return true;
    }

}
