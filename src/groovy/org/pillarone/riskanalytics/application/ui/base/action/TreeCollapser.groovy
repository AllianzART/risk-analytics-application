package org.pillarone.riskanalytics.application.ui.base.action

import com.canoo.ulc.community.fixedcolumntabletree.server.ULCFixedColumnTableTree
import com.ulcjava.base.application.IAction
import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.tree.TreePath
import com.ulcjava.base.application.util.KeyStroke
import groovy.transform.CompileStatic
import org.pillarone.riskanalytics.application.ui.util.ExceptionSafe

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
@CompileStatic
class TreeCollapser extends ResourceBasedAction {

    final ULCFixedColumnTableTree tree

    public TreeCollapser(ULCFixedColumnTableTree tree) {
        super("CollapseNode")
        this.tree = tree
        putValue(IAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true));
    }

    public void doActionPerformed(ActionEvent event) {
        ExceptionSafe.protect {
            TreePath[] paths = tree.getSelectedPaths()
            def rowHeaderTableTree = tree.getRowHeaderTableTree()
            TreePath pathToCheckIfRoot = rowHeaderTableTree.isRootVisible()? paths[0] : paths[0].parentPath
            if (!rowHeaderTableTree.isExpanded(paths[0]) && !isRootNode(pathToCheckIfRoot)) {
                rowHeaderTableTree.getSelectionModel().setSelectionPath(paths[0].getParentPath());
                tree.getViewPortTableTree().getSelectionModel().setSelectionPath(paths[0].getParentPath());
            } else {
                collapsePaths(paths, true)
            }
        }
    }

    private boolean isRootNode(TreePath path) {
        path.lastPathComponent == tree.rowHeaderTableTree.model.root
    }

    private void collapseAll(TreePath[] paths) {
        ULCTableTree rowView = (ULCTableTree) tree.getRowHeaderView();
        rowView.collapseAll()
        ULCTableTree viewPortView = (ULCTableTree) tree.getViewPortView();
        viewPortView.collapseAll()
        expandPaths(paths, false)
    }

    private void collapsePaths(TreePath[] paths, boolean includingDescendants) {
        tree.getRowHeaderTableTree().collapsePaths(paths, includingDescendants);
        tree.getViewPortTableTree().collapsePaths(paths, includingDescendants);
        expandPaths(paths, false)
    }

    protected void expandPaths(TreePath[] paths, boolean includingDescendants) {
        tree.getRowHeaderTableTree().expandPaths(paths, includingDescendants);
        tree.getViewPortTableTree().expandPaths(paths, includingDescendants);
    }

    @Override
    String toString() {
        tree.getSelectedPaths()
    }
}

@CompileStatic
class Collapser extends TreeCollapser {

    public Collapser(ULCFixedColumnTableTree tree) {
        super(tree)
    }

    protected void expandPaths(TreePath[] paths, boolean includingDescendants) {

    }

}