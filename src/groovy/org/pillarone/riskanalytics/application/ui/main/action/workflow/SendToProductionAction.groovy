package org.pillarone.riskanalytics.application.ui.main.action.workflow

import com.ulcjava.base.application.ULCTableTree
import groovy.transform.CompileStatic
import org.pillarone.riskanalytics.core.user.UserManagement
import org.pillarone.riskanalytics.core.workflow.Status

@CompileStatic
class SendToProductionAction extends AbstractWorkflowAction {

    SendToProductionAction(ULCTableTree tree) {
        super("SendToProduction", tree);
    }

    @Override
    boolean isEnabled() {
        return
            getAllSelectedObjectsSimpler().size() == 1 &&
            super.isEnabled()//generic checks like user roles
    }

    Status toStatus() {
        Status.IN_PRODUCTION
    }

    @Override
    protected List allowedRoles() {
        return [UserManagement.REVIEWER_ROLE]
    }
}