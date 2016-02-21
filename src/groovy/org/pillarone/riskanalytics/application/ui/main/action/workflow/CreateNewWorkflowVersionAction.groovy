package org.pillarone.riskanalytics.application.ui.main.action.workflow
import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.event.ActionEvent
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.base.model.ItemNode
import org.pillarone.riskanalytics.application.ui.main.action.SelectionTreeAction
import org.pillarone.riskanalytics.application.ui.main.view.item.ModellingUIItem
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.VersionNumber
import org.pillarone.riskanalytics.core.util.Configuration
import org.pillarone.riskanalytics.core.workflow.Status
import org.pillarone.riskanalytics.core.workflow.WorkflowException

class CreateNewWorkflowVersionAction extends AbstractWorkflowAction {
    protected static Log LOG = LogFactory.getLog(CreateNewWorkflowVersionAction)
    protected static final List<Integer> oneOrTwo = [1,2]

    // forbid meddling by non-workflow-owners via -DCreateNewWorkflowVersion.promiscuous=false
    private final static boolean promiscuous =                  //breaks tests when false so by default it's set to true
        Configuration.coreGetAndLogStringConfig("CreateNewWorkflowVersionPromiscuous","true").equalsIgnoreCase("true")

    CreateNewWorkflowVersionAction(ULCTableTree tree) {
        super("NewWorkflowVersion", tree)
    }


    // Checks to prevent :
    // - creating new workflow off someone else's model
    // - creating new workflow off not-the-latest version
    // - creating new workflow off not-a-sandbox adoptee
    //
    @Override
    void doActionPerformed(ActionEvent event) {
        Parameterization parameterization = getSelectedItem()

        // PMO-2765 Forbid meddling via -DCreateNewMajorVersion.promiscuous=false
        //
        if( !promiscuous  && ownerCanVetoUser(parameterization?.creator)){

            String msg = "${parameterization?.creator.username} owns ${parameterization?.getNameAndVersion()}. \n"+
                         "(Hint: Save your own copy to work on.)"
            LOG.info(msg)
            showInfoAlert( "Cannot Create New Workflow Version", msg )
            LOG.info("Hint: -DCreateNewWorkflowVersion.promiscuous=true will allow non-owner meddling ")
            return
        }

        SortedSet allVersions = new TreeSet(VersionNumber.getExistingVersions(parameterization))
        if (parameterization.versionNumber != allVersions.last()) {
            String w = "Later version of '$parameterization.nameAndVersion' exists: ${allVersions.last()}."
            showErrorAlert("Problem creating new version",w,true)
            return
        }

        List<ItemNode> selectedItems = getAllSelectedObjectsSimpler()
        int selectedItemCount = selectedItems.size()

        // The isEnabled() should prevent arriving here unless exactly 1 or 2 items selected
        //
        if ( !oneOrTwo.contains(selectedItemCount)) {
            throw new IllegalStateException("CreateNewWorkflowVersionAction invoked with $selectedItemCount items!")
        }

        // Check only P14n items selected - should never fail as menu only added on PRODUCTION workflows <shrug>
        // TODO: Ditch check when satisfied nothing crazy going on..
        ItemNode notP14n = selectedItems.find { !(it.itemNodeUIItem?.item instanceof Parameterization)}
        if( notP14n ){
            String msg = "'${notP14n?.name}' NOT a P14n "+
                    "(it's a ${notP14n?.itemNodeUIItem?.item?.class?.simpleName}).\n"+
                    "Hint: Select a production workflow to bump (& optional sandbox model to adopt)."
            showInfoAlert( "Non P14n Selected", msg, true )
            return
        }

        // Ensure only one model class involved
        //
        if(selectedItemCount > 1){

            Class first = selectedItems.get(0).itemNodeUIItem?.item.modelClass;
            Class second= selectedItems.get(1)?.itemNodeUIItem?.item.modelClass;
            if( ! first?.equals(second) ){
                String msg = "Can't adopt sandbox p14n from one Model Class into a workflow in another Model Class"
                showInfoAlert( "Multiple model classes involved", msg, true )
                return
            }
        }

        // Check exactly one production workflow P14n selected
        //
        int numWorkflows = selectedItems.count { (it.itemNodeUIItem?.item as Parameterization).status != Status.NONE}
        if( numWorkflows != 1 ){
            String msg = "Need ONE workflow but ${numWorkflows} are selected.\n" +
                    "Hint: Select ONE workflow version to bump (& optional sandbox model to adopt)."
            showInfoAlert( "Exactly ONE workflow needed", msg, true )
            return
        }

        // Check max 1 sandbox P14n selected, and record it for base class to use.
        //
        optionalSandboxModel = null
        for( ItemNode it : selectedItems){
            Parameterization p14N = it.itemNodeUIItem?.item as Parameterization
            if( p14N.status == Status.NONE ){
                if(optionalSandboxModel == null){
                    optionalSandboxModel = p14N
                }else{
                    showInfoAlert( "Only ONE sandbox model allowed here",
                                    "Cannot adopt multiple sandbox models as new workflow version.\n" +
                                    "Hint: Select MAX 1 optional sandbox model to.",
                                    true )
                    return
                }
            }
        }

        // Over to the base class where we need add logic for optional selected sandbox to adopt
        super.doActionPerformed(event)
    }

    protected List<Integer> validSelectionCounts() { return oneOrTwo }

    @Override
    Status toStatus() {
        return Status.DATA_ENTRY
    }


}
