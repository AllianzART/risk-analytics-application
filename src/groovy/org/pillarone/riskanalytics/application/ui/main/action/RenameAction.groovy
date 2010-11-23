package org.pillarone.riskanalytics.application.ui.main.action

import com.ulcjava.base.application.IAction
import com.ulcjava.base.application.ULCAlert
import com.ulcjava.base.application.ULCTree
import com.ulcjava.base.application.UlcUtilities
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.util.KeyStroke
import org.pillarone.riskanalytics.application.ui.main.model.P1RATModel
import org.pillarone.riskanalytics.application.ui.main.view.NodeNameDialog
import org.pillarone.riskanalytics.application.ui.util.I18NAlert
import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.ResultConfiguration

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class RenameAction extends SelectionTreeAction {

    public RenameAction(ULCTree tree, P1RATModel model) {
        super("Rename", tree, model)
        putValue(IAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0, true));
    }

    public void doActionPerformed(ActionEvent event) {
        /* For opening the cellEditor implement a extension and call startEditingPath on clientSide (remember to convert the TreePath)
        ULCTreeModelAdapter adapter = ULCSession.currentSession().getModelAdapterProvider().getModelAdapter(ITreeModel.class, tree.model)
        tree.invokeUI("startEditingAtPath", [adapter.getDescriptionForPath(tree.getSelectionPath())] as Object[])
        */
        boolean usedInSimulation = false
        def selectedItem = getSelectedItem()
        if (!(selectedItem instanceof ModellingItem)) return
        if (selectedItem instanceof Parameterization || selectedItem instanceof ResultConfiguration) {
            selectedItem.setModelClass(getSelectedModel().class)
            usedInSimulation = selectedItem.isUsedInSimulation()
            if (!usedInSimulation) {
                List runs = SimulationRun.executeQuery(" from ${SimulationRun.class.name} as run where run.parameterization.name = :name ", ["name": selectedItem.name])
                usedInSimulation = runs != null && runs.size() > 0
            }
        }
        if (!usedInSimulation) {
            NodeNameDialog dialog = new NodeNameDialog(UlcUtilities.getWindowAncestor(tree), selectedItem)
            dialog.title = dialog.getText("renameTitle") + " " + selectedItem.name

            dialog.okAction = { model.renameItem(selectedItem, dialog.nameInput.text) }
            dialog.show()
        } else {
            ULCAlert alert = new I18NAlert(UlcUtilities.getWindowAncestor(event.source), "RenamingLocked")
            alert.show()
        }
    }

}
