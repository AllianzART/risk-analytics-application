package org.pillarone.riskanalytics.application.ui.main.action
import com.ulcjava.base.application.IAction
import com.ulcjava.base.application.ULCAlert
import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.UlcUtilities
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.util.KeyStroke
import org.pillarone.riskanalytics.application.ui.main.view.NodeNameDialog
import org.pillarone.riskanalytics.application.ui.main.view.item.ModellingUIItem
import org.pillarone.riskanalytics.application.ui.util.I18NAlert
import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.ResultConfiguration
import org.pillarone.riskanalytics.core.simulation.item.VersionNumber
import org.pillarone.riskanalytics.core.workflow.Status

// AR-227 Functionality was broken in various ways
// 1) Existence of sim for an item in a different model tree would block renaming the item
// 2) When it tried to rename a subtree of items it forgot there might be items excluded by search filter
// 3) Even when all items visible in tree, if you clicked on any but the top one, it would not rename higher versions
// 4) Stupidly hit the DB for simulations even when it could quit early eg for workflow items
// ...
// As testing is so slow, for now I have only fixed things for P14ns, not ResultConfigs - they still rename the old way.


/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class RenameAction extends SingleItemAction {

    public RenameAction(ULCTableTree tree) {
        super("Rename", tree)
        putValue(IAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0, true));
    }

    public void doActionPerformed(ActionEvent event) {

        if( quitWithAlertIfCalledWhenDisabled() ){ // SingleItemAction constraint
            return
        }

        /* For opening the cellEditor implement a extension and call startEditingPath on clientSide (remember to convert the TreePath)
        ULCTreeModelAdapter adapter = ULCSession.currentSession().getModelAdapterProvider().getModelAdapter(ITreeModel.class, tree.model)
        tree.invokeUI("startEditingAtPath", [adapter.getDescriptionForPath(tree.getSelectionPath())] as Object[])
        */
        int     sizeofFamily = 0
        ModellingUIItem selectedItem = getSelectedUIItem()
        if (selectedItem.item instanceof ResultConfiguration) {
            if (selectedItem.usedInSimulation || nameUsedInSimulation(selectedItem.item) ) { // AR-227 broken as before
                ULCAlert alert = new I18NAlert(UlcUtilities.getWindowAncestor(event.source), "RenamingLocked")
                alert.show()
                return
            }
        } else if (selectedItem.item instanceof Parameterization) {
            Parameterization p14n = (selectedItem.item as Parameterization)

            // Don't waste time on sandbox checks if it's a workflow
            //
            if( p14n.versionNumber.workflow ){
                showInfoAlert("Cannot rename Workflow models",
                        "Sorry, ${selectedItem.nameAndVersion} is a Workflow model.", true)
                return
            }

            // Don't waste time on sandbox-family checks if selected item used in sim
            //
            if (selectedItem.usedInSimulation) {
                ULCAlert alert = new I18NAlert(UlcUtilities.getWindowAncestor(event.source), "RenamingLocked")
                alert.show()
                return
            }

            // Avoid renaming partial model family - it's all or nothing
            //
            VersionNumber latestVersion = VersionNumber.getHighestNonWorkflowVersion(p14n)
            if ( p14n.versionNumber != latestVersion ) { // Cannot be null as we verified p14n is sandbox model

                String s = "Hint: to rename all versions (if no sims exist), select LATEST version (i.e. v${latestVersion}) of '${p14n.nameAndVersion}'."
                showInfoAlert("Cannot rename partial model family", s, true)

                return
            }

            // Don't rename family if sims exist for any members
            //
            if ( siblingsUsedInSimulation(p14n) ) {

                String s = "Cannot rename ${p14n.nameAndVersion}'s model family while sims exist.\n"+
                           "Hint: Delete the sims if they are not needed. Contact IT Apps if you need help."
                showInfoAlert("Simulations exist for other version(s)", s, true)

                return
            }

        }

        NodeNameDialog dialog = new NodeNameDialog(UlcUtilities.getWindowAncestor(tree), selectedItem)
        dialog.title = dialog.getText("renameTitle") + " " + selectedItem.name

        dialog.okAction = { String name -> selectedItem.rename(name) }
        dialog.show()
    }

    protected boolean nameUsedInSimulation(ResultConfiguration item) {
        List runs = SimulationRun.executeQuery("from ${SimulationRun.name} as run where run.resultConfiguration.name = :name ", ["name": item.name])
        return runs != null && runs.size() > 0
    }

    protected boolean nameUsedInSimulation(Parameterization item) {
        List runs = SimulationRun.executeQuery("from ${SimulationRun.name} as run where run.parameterization.name = :name ", ["name": item.name])
        return runs != null && runs.size() > 0
    }

    // Does the model family have any versions used in a sim - if so we cannot rename it
    //
    protected boolean siblingsUsedInSimulation(Parameterization item) {
        // Nothing is saved by doing this pre-check - it costs a DB access anyway, so skip it and
        // in many cases we reduce #DB calls to just 1..
        //
//        if(VersionNumber.getExistingVersionsSameAuditCategory(item).size() < 2){
//            return false
//        }

        if(item.versionNumber?.workflow){
            List<SimulationRun> runsFromWorkflows = SimulationRun.executeQuery(
                    "from ${SimulationRun.name} as run "+
                            "where run.parameterization.name = ? "+
                            "and run.parameterization.modelClassName = ? "+
                            "and run.parameterization.itemVersion like 'R%' ",
                    [item.name, item.modelClass.name])
            return runsFromWorkflows?.size() > 0
        }
        List<SimulationRun> runsFromSandboxes = SimulationRun.executeQuery(
                "from ${SimulationRun.name} as run "+
                        "where run.parameterization.name = ? "+
                        "and run.parameterization.modelClassName = ? "+
                        "and run.parameterization.itemVersion not like 'R%' ",       // nb: !!'not'!!
                [item.name, item.modelClass.name])
        return runsFromSandboxes?.size() > 0
    }

}
