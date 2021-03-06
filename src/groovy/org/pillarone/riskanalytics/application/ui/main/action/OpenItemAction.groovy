package org.pillarone.riskanalytics.application.ui.main.action

import com.ulcjava.base.application.ULCAlert
import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.UlcUtilities
import com.ulcjava.base.application.event.ActionEvent
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.main.eventbus.event.OpenDetailViewEvent
import org.pillarone.riskanalytics.application.ui.main.view.OpenItemDialog
import org.pillarone.riskanalytics.application.ui.main.view.item.*
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class OpenItemAction extends SelectionTreeAction {

    private static final Log LOG = LogFactory.getLog(OpenItemAction)

    OpenItemAction(ULCTableTree tree) {
        super("Open", tree)
    }

    //PMO-2795 Allow opening up to N items in one go.  Probably 9 is too many.
    //Handy for when you want to open a handful of batches for a qtr run, and you know it's going to take about 3 mins
    //Enough time to go get a coffee and come back.
    public void doActionPerformed(ActionEvent event) {
        List<AbstractUIItem> items = getSelectedUIItems()
        if(items.size() > maxItemsOpenableInOneClick ){
            LOG.info("User tried to open ${items.size()} items, but maxItemsOpenableInOneClick=$maxItemsOpenableInOneClick (nb is sysprop)")
            showInfoAlert( "Too many items selected",
                           "Please select $maxItemsOpenableInOneClick or fewer items to open." )
        } else {
            for( AbstractUIItem item : items ){
                if (item != null) {
                    openItem(item)
                }
            }
        }
    }

    private void openItem(ParameterizationUIItem parameterizationUIItem) {
        Model selectedModel = getSelectedModel()
        if (selectedModel != null) {
            selectedModel = selectedModel.class.newInstance()
            selectedModel.init()
            ModellingItem item = parameterizationUIItem.item
            item.dao.modelClassName = selectedModel.class.name
            synchronized (parameterizationUIItem) {
                item.daoClass.withTransaction { status ->
                    boolean usedInSimulation = parameterizationUIItem.isUsedInSimulation()
                    if (!usedInSimulation || !parameterizationUIItem.newVersionAllowed()) {
                        LOG.info("Opening parameterization ${parameterizationUIItem.nameAndVersion}")
                        riskAnalyticsEventBus.post(new OpenDetailViewEvent(parameterizationUIItem))
                    } else {
                        LOG.info("Parameterization ${parameterizationUIItem.nameAndVersion} cannot be edited.")
                        showOpenItemDialog(selectedModel, parameterizationUIItem)
                    }
                }
            }
        }
    }

    private void openItem(ResourceUIItem item) {
        boolean usedInSimulation = item.usedInSimulation
        if (usedInSimulation) {
            showOpenItemDialog(null, item)
        } else {
            riskAnalyticsEventBus.post(new OpenDetailViewEvent(item))
        }
    }

    private void openItem(BatchUIItem item) {
        riskAnalyticsEventBus.post(new OpenDetailViewEvent(item))
    }

    private void openItem(AbstractUIItem item) {
        Model selectedModel = getSelectedModel()
        if (selectedModel != null) {
            boolean usedInSimulation = false
            if (item instanceof ModellingUIItem) {
                usedInSimulation = item.isUsedInSimulation()
            }
            if (usedInSimulation) {
                showOpenItemDialog(selectedModel, item)
            } else {
                riskAnalyticsEventBus.post(new OpenDetailViewEvent(item))
            }
        } else {
            LOG.info("Found no Model for selected path ${tree?.selectedPath}")
        }
    }

    private void showOpenItemDialog(Model selectedModel, ModellingUIItem item) {
        OpenItemDialog openItemDialog = new OpenItemDialog(tree, selectedModel, riskAnalyticsEventBus, item)
        openItemDialog.init()
        openItemDialog.visible = true
    }

}
