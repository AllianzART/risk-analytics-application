package org.pillarone.riskanalytics.application.ui.main.action.workflow
import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.event.ActionEvent
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.VersionNumber
import org.pillarone.riskanalytics.core.util.Configuration
import org.pillarone.riskanalytics.core.workflow.Status
import org.pillarone.riskanalytics.core.workflow.WorkflowException

class CreateNewWorkflowVersionAction extends AbstractWorkflowAction {
    protected static Log LOG = LogFactory.getLog(CreateNewWorkflowVersionAction)

    // forbid meddling by non-workflow-owners via -DCreateNewWorkflowVersion.promiscuous=false
    private final static boolean promiscuous =                  //breaks tests when false so by default it's set to true
        Configuration.coreGetAndLogStringConfig("CreateNewWorkflowVersionPromiscuous","true").equalsIgnoreCase("true")

    CreateNewWorkflowVersionAction(ULCTableTree tree) {
        super("NewWorkflowVersion", tree)
    }

    @Override
    void doActionPerformed(ActionEvent event) {
        Parameterization parameterization = getSelectedItem()

        // PMO-2765 Juan described (20140425 chat) other users been creating new versions of his models w/o asking.
        //
        if( !promiscuous  ){  //forbid meddling via -DCreateNewMajorVersion.promiscuous=false

            if( ownerCanVetoUser(parameterization?.creator) ){
                String msg = "${parameterization?.creator.username} owns ${parameterization?.getNameAndVersion()}. \n(Hint: Save your own copy to work on.)"
                LOG.info(msg)
                LOG.info("Hint: -DCreateNewWorkflowVersion.promiscuous=true will allow non-owner meddling ")
                showInfoAlert( "Cannot Create New Workflow Version", msg )
                return
            }
        }

        SortedSet allVersions = new TreeSet(VersionNumber.getExistingVersions(parameterization))
        if (parameterization.versionNumber != allVersions.last()) {
            throw new WorkflowException( parameterization.getNameAndVersion(),
                                         toStatus(),
                                         "Cannot create a new version. A newer version already exists: ${allVersions.last()}"   )
        }
        super.doActionPerformed(event)
    }



    @Override
    Status toStatus() {
        return Status.DATA_ENTRY
    }


}
