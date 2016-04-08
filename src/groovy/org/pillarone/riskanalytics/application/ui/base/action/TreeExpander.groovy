package org.pillarone.riskanalytics.application.ui.base.action

import com.canoo.ulc.community.fixedcolumntabletree.server.ULCFixedColumnTableTree
import com.ulcjava.base.application.IAction
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.tree.TreePath
import com.ulcjava.base.application.util.KeyStroke
import org.pillarone.riskanalytics.application.ui.util.ExceptionSafe

class TreeExpander extends ResourceBasedAction {

    ULCFixedColumnTableTree tree

    public TreeExpander(ULCFixedColumnTableTree tree) {
        super("ExpandNode")
        this.tree = tree
        putValue(IAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK, false));
    }

    public void doActionPerformed(ActionEvent event) {
        ExceptionSafe.protect {
            TreePath[] paths = tree.getSelectedPaths()
            trace("Expand paths: ${paths?.lastPathComponent?.name}")

            def rowHeaderTableTree = tree.getRowHeaderTableTree()
            def viewPortTableTree = tree.getViewPortTableTree()
            if (paths[0].lastPathComponent == tree.rowHeaderTableTree.model.root) {
                tree.expandAll()
            } else if (rowHeaderTableTree.isExpanded(paths[0])) {
                rowHeaderTableTree.setRowSelection(rowHeaderTableTree.getSelectedRow() + 1);
                rowHeaderTableTree.scrollCellToVisible(rowHeaderTableTree.getSelectedPath(), rowHeaderTableTree.getModel().getTreeColumn());
                viewPortTableTree.setRowSelection(viewPortTableTree.getSelectedRow() + 1);
                viewPortTableTree.scrollCellToVisible(viewPortTableTree.getSelectedPath(), viewPortTableTree.getModel().getTreeColumn());
            }else {
                tree.expandPaths(paths, true)
            }
        }
    }

}
