package org.pillarone.riskanalytics.application.ui.base.action

import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.ULCTableTree
import groovy.transform.CompileStatic


@CompileStatic
class GoToParentNodeAction extends ExceptionSafeAction {

    private ULCTableTree tree

    GoToParentNodeAction(ULCTableTree tree) {
        this.tree = tree
    }

    @Override
    void doActionPerformed(ActionEvent event) {
        tree.getSelectionModel().setSelectionPath(tree.selectedPath.parentPath)
    }
}
