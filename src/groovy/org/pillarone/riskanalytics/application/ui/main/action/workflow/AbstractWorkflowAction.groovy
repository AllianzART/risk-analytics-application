package org.pillarone.riskanalytics.application.ui.main.action.workflow
import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.tree.TreePath
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.dataaccess.item.ModellingItemFactory
import org.pillarone.riskanalytics.application.ui.base.model.ItemNode
import org.pillarone.riskanalytics.application.ui.comment.view.NewCommentView
import org.pillarone.riskanalytics.application.ui.main.action.SelectionTreeAction
import org.pillarone.riskanalytics.application.ui.main.action.SingleItemAction
import org.pillarone.riskanalytics.application.ui.main.view.NewVersionCommentDialog
import org.pillarone.riskanalytics.application.ui.main.view.item.ModellingUIItem
import org.pillarone.riskanalytics.application.ui.util.ExceptionSafe
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.parameter.comment.Tag
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.workflow.Status
import org.pillarone.riskanalytics.core.workflow.StatusChangeService


abstract class AbstractWorkflowAction extends SelectionTreeAction {
    protected static Log LOG = LogFactory.getLog(AbstractWorkflowAction)

    private StatusChangeService service = getService()
    protected Parameterization optionalSandboxModel = null //AR-190

    // Ugly constructor because TreeDoubleClickAction somehow calls OpenItemAction which somehow calls
    // e.g. CreateNewMajorVersion (our subclass) with a single string ctor.
    //
    public AbstractWorkflowAction(String name, ULCTableTree tree = null) {
        super(name, tree)
    }

    @Override
    boolean isEnabled() {
        return super.isEnabled()//generic checks like user roles
    }

    // This method is shared by subclasses
    //
    void doActionPerformed(ActionEvent event) {

        if( quitWithAlertIfCalledWhenDisabled() ){
            return
        }

        // AR-190 whether a sandboxModelToAdopt has been selected along with top w/f version
        // And if so call the 3-arg changeStatus()
        //
        Parameterization item = getSelectedItem()
        if(optionalSandboxModel != null ){
            if(!optionalSandboxModel.isLoaded()){
                optionalSandboxModel.load()
            }
            // May need to hunt workflow
            //
            if( item == optionalSandboxModel ){ // as sadly sometimes sandbox is seen as 'selected item'
                TreePath otherItemNode = tree?.getSelectedPaths().find {
                    (it.lastPathComponent instanceof  ItemNode) &&
                    (it.lastPathComponent.itemNodeUIItem.item != optionalSandboxModel) &&
                    (it.lastPathComponent.itemNodeUIItem.item instanceof Parameterization &&
                      (it.lastPathComponent.itemNodeUIItem.item as Parameterization).status != Status.NONE
                    )
                }
                if( ! otherItemNode ){
                    // Houston, we have a problem
                    showErrorAlert("Where is the workflow?", "Can only see sandbox ${optionalSandboxModel.nameAndVersion}", true)
                    return
                }
                item = otherItemNode?.lastPathComponent?.itemNodeUIItem?.item
            }
        }
        if (!item.isLoaded()) {
            item.load()
        }
        Status toStatus = toStatus()

        if (toStatus == Status.DATA_ENTRY) {
            Closure changeStatusAction = { String commentText ->
                ExceptionSafe.protect {
//                    ModellingUIItem uiItem = getSelectedUIItem()
//                    if (!uiItem.isLoaded()) {
//                        uiItem.load()
//                    }
                    Parameterization parameterization = optionalSandboxModel ?
                        changeStatus(item, toStatus, optionalSandboxModel)   :
                        changeStatus(item, toStatus)                         ;
                    Tag versionTag = Tag.findByName(NewCommentView.VERSION_COMMENT)
                    if(commentText){
                        parameterization.addTaggedComment("v${parameterization.versionNumber}: ${commentText}", versionTag)
                    }else{
                        LOG.info("Skipped adding of version comment on ${parameterization.nameAndVersion}")
                    }
                    parameterization.save()
                }
            }
            //
            //
            NewVersionCommentDialog versionCommentDialog =
                optionalSandboxModel ? new NewVersionCommentDialog(
                                            changeStatusAction,
                                            "Adopting ${optionalSandboxModel.nameAndVersion} as new workflow version",
                                            UIUtils.getText(NewVersionCommentDialog, "createNewVersionFromSandbox") )
                                     : new NewVersionCommentDialog(changeStatusAction)
            ;
            versionCommentDialog.show()
        } else {
            changeStatus(item, toStatus)
        }

    }

    protected Parameterization changeStatus(Parameterization item, Status toStatus) {
        Parameterization parameterization = service.changeStatus(item, toStatus)
        parameterization.save()
        ParameterizationDAO dao = parameterization.dao as ParameterizationDAO
        parameterization = (Parameterization) ModellingItemFactory.getParameterization(dao)
        parameterization.load()
        return parameterization
    }

    // AR-190 new api on StatusChangeService that uses supplied sandbox model to clone
    //
    protected Parameterization changeStatus(Parameterization item, Status toStatus, Parameterization sandboxModelToAdopt) {
        Parameterization parameterization = service.changeStatus(item, toStatus, sandboxModelToAdopt)
        parameterization.save()
        ParameterizationDAO dao = parameterization.dao as ParameterizationDAO
        parameterization = (Parameterization) ModellingItemFactory.getParameterization(dao)
        parameterization.load()
        return parameterization
    }
    abstract Status toStatus()

    StatusChangeService getService() {
        try {
            return StatusChangeService.getService()
        } catch (Exception ex) {
            LOG.warn("StatusChangeService.getService() threw: ", ex)
        }
        return null
    }


}
