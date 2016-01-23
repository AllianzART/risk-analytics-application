package org.pillarone.riskanalytics.application.ui.main.action
import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.tree.TreePath
import groovy.transform.CompileStatic
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.base.model.ItemGroupNode
import org.pillarone.riskanalytics.application.ui.base.model.ModelNode
import org.pillarone.riskanalytics.application.ui.base.model.ResourceClassNode
import org.pillarone.riskanalytics.application.ui.base.model.ResourceGroupNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.BatchRootNode
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem

@CompileStatic
class TreeDoubleClickAction extends SelectionTreeAction {
    protected static Log LOG = LogFactory.getLog(TreeDoubleClickAction)
    private final OpenItemAction openItemAction

    TreeDoubleClickAction(ULCTableTree tree) {
        super('Open', tree)
        this.openItemAction = new OpenItemAction(tree);
    }

    void doActionPerformed(ActionEvent event) {
        TreePath path = tree.selectedPath
        if(path){
        if (isNodeWeWantToToggleOnDoubleClick(path.lastPathComponent)) {
            toggle(path)
            return
        }
        } else {
            LOG.warn("Got a double click without a path. Maybe CTRL key pressed?")
        }
        if(selectedItem){
            if (selectedItem instanceof ModellingItem) {
                openItemAction.doActionPerformed(event)
            }else{
                LOG.warn("INSANITY: selected item ($selectedItem) is not instance of ModellingItem!")
            }
        }else{
            LOG.warn("Got a double click without a selected item. Maybe CTRL key stuck?")
        }
    }

    private boolean isNodeWeWantToToggleOnDoubleClick(Object node) {
        [
                ModelNode,
                BatchRootNode,
                ResourceGroupNode,
                ItemGroupNode,
                ResourceClassNode
        ].any { Class clazz -> clazz.isInstance(node) }
    }

    private void toggle(TreePath path) {
        tree.isExpanded(path) ? tree.collapsePath(path) : tree.expandPath(path)
    }
}
