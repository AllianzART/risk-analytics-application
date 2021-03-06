package org.pillarone.riskanalytics.application.ui.base.action

import com.ulcjava.base.application.ULCAlert
import com.ulcjava.base.application.UlcUtilities
import com.ulcjava.base.application.tabletree.ITableTreeNode
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterViewModel
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.components.Component
import com.ulcjava.base.application.ULCWindow
import org.pillarone.riskanalytics.application.ui.base.view.DynamicComponentNameDialog
import org.pillarone.riskanalytics.application.ui.util.ComponentUtils
import org.pillarone.riskanalytics.application.ui.base.model.ComponentTableTreeNode
import org.pillarone.riskanalytics.application.ui.base.model.DynamicComposedComponentTableTreeNode
import org.pillarone.riskanalytics.core.components.NonUniqueComponentNameException

/**
 * @author: fouad.jaada (at) intuitive-collaboration (dot) com
 */

public class TreeNodeDuplicator extends TreeNodeAction {

    private static Log LOG = LogFactory.getLog(TreeNodeDuplicator)

    public TreeNodeDuplicator(def tree, ParameterViewModel model) {
        super(tree, model, "Duplicate");
    }


    protected void doAction(String newName, ParameterViewModel model, ITableTreeNode node, tree, boolean withComments) {
        if (model.paramterTableTreeModel.readOnly) return
        if (node instanceof ComponentTableTreeNode) {
            ITableTreeNode parent = node.parent
            if (parent instanceof DynamicComposedComponentTableTreeNode) {
                String oldPath = ComponentUtils.removeModelFromPath(node.path, model.model)
                String newPath = ComponentUtils.removeModelFromPath(node.parent.path, model.model) + ":$newName"
                // AR-207 Don't try to create exact duplicate, dummy!
                //
                if( !newPath.equals(oldPath) &&
                    !model.parametrizedItem.notDeletedParameterHolders.find { it.path.contains(newPath+':')} ){
                    Component component = node.parent.component.createDefaultSubComponent()
                    component.name = newName
                    model.parametrizedItem.copyComponent(oldPath, newPath, component, withComments)
                } else {
                    String msg = "Component named '${newName.substring(3)}' already exists (not deleted..)" +
                                 "\n(NB PillarOne coerces names to its own convention.)"
                    LOG.warn(msg)
                    UIUtils.showAlert(
                            UlcUtilities.getWindowAncestor(tree),
                            "Pls supply a new name",
                            msg,
                            ULCAlert.WARNING_MESSAGE
                    )
                }
            }
        }
    }

    public boolean isEnabled() {
        return super.isEnabled() && !model.paramterTableTreeModel.readOnly;
    }

    @Override
    public DynamicComponentNameDialog getInputNameDialog(ULCWindow parent, String displayName) {
        DynamicComponentNameDialog dialog = new DynamicComponentNameDialog(parent, displayName)
        dialog.withComments.setVisible(true)
        return dialog
    }

    @Override
    protected boolean isEditable(def Object node) {
        return super.isEditable(node.parent)
    }
}