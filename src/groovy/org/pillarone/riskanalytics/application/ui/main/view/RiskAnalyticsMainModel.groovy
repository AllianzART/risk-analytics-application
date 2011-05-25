package org.pillarone.riskanalytics.application.ui.main.view

import com.ulcjava.base.application.ULCPollingTimer
import com.ulcjava.base.application.tabletree.AbstractTableTreeModel
import com.ulcjava.base.application.tabletree.DefaultTableTreeModel
import com.ulcjava.base.application.tabletree.ITableTreeNode
import com.ulcjava.base.application.tree.TreePath
import groovy.beans.Bindable
import org.apache.log4j.Logger
import org.pillarone.riskanalytics.application.ui.base.model.AbstractModellingModel
import org.pillarone.riskanalytics.application.ui.base.model.AbstractPresentationModel
import org.pillarone.riskanalytics.application.ui.base.model.IModelChangedListener
import org.pillarone.riskanalytics.application.ui.base.model.ItemGroupNode
import org.pillarone.riskanalytics.application.ui.base.view.IModelItemChangeListener
import org.pillarone.riskanalytics.application.ui.batch.action.PollingBatchSimulationAction
import org.pillarone.riskanalytics.application.ui.batch.model.BatchTableListener
import org.pillarone.riskanalytics.application.ui.main.model.IContentModel
import org.pillarone.riskanalytics.application.ui.main.model.IP1RATModelListener
import org.pillarone.riskanalytics.application.ui.simulation.model.impl.SimulationConfigurationModel
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.application.ui.main.view.item.*

class RiskAnalyticsMainModel extends AbstractPresentationModel {

    List<AbstractUIItem> openItems
    Map<AbstractUIItem, IContentModel> viewModelsInUse
    AbstractTableTreeModel selectionTreeModel
    ULCPollingTimer pollingBatchSimulationTimer
    PollingBatchSimulationAction pollingBatchSimulationAction
    def switchActions = []
    private List modelListeners = []
    List batchTableListeners = []
    List modelItemlisteners = []
    //selectedItem needs to be updated when tabs are changed etc.
    @Bindable AbstractUIItem currentItem

    public static boolean deleteActionIsRunning = false
    static final Logger LOG = Logger.getLogger(RiskAnalyticsMainModel)

    public RiskAnalyticsMainModel() {
        viewModelsInUse = [:]
    }

    void saveAllOpenItems() {
        viewModelsInUse.keySet().each {AbstractUIItem item ->
            item.save()
        }
    }

    public void renameItem(AbstractUIItem item, String name) {
        item.rename(name)
    }

    private void renameAllChildren(ITableTreeNode itemNode, String name) {
        //todo fja refactoring move the code to selectionTreeModel.renameAllChildren...
        itemNode.childCount.times {
            def childNode = itemNode.getChildAt(it)
            closeItem(itemNode.item.modelClass.newInstance(), childNode.item)

            childNode.userObject = name
            selectionTreeModel.nodeChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(childNode) as Object[]))

            renameAllChildren(childNode, name)
        }
    }

    public void removeItems(Model selectedModel, ItemGroupNode itemGroupNode, List<AbstractUIItem> modellingItems) {
        closeItems(selectedModel, modellingItems)
        selectionTreeModel.removeAllGroupNodeChildren(itemGroupNode)
        try {
            for (AbstractUIItem item: modellingItems) {
                item.remove()
                item.delete()
            }//for
        } catch (Exception ex) {
            LOG.error "Deleting Item Failed: ${ex}"
        }
        fireModelChanged()
    }

    public AbstractModellingModel getViewModel(AbstractUIItem item) {
        if (viewModelsInUse.containsKey(item)) {
            return viewModelsInUse[item]
        }
        return item.getViewModel()
    }


    public void openItem(Model model, AbstractUIItem item) {
        if (!item.isLoaded())
            item.load()
        notifyOpenDetailView(model, item)
    }

    public void closeItem(Model model, AbstractUIItem abstractUIItem) {
        notifyCloseDetailView(model, abstractUIItem)
        viewModelsInUse.remove(abstractUIItem)
        unregisterModel(abstractUIItem)
        abstractUIItem.removeAllModellingItemChangeListener()

    }



    private void closeItems(Model selectedModel, List<AbstractUIItem> items) {
        for (AbstractUIItem item: items) {
            closeItem(selectedModel, item)
        }
    }


    public void addModelItemChangedListener(IModelItemChangeListener listener) {
        modelItemlisteners << listener
    }

    public void removeModelItemChangedListener(IModelItemChangeListener listener) {
        modelItemlisteners.remove(listener)
    }

    public void fireModelItemChanged() {
        modelItemlisteners.each {IModelItemChangeListener listener -> listener.modelItemChanged()}
    }

    public void addBatchTableListener(BatchTableListener batchTableListener) {
        batchTableListeners << batchTableListener
    }

    public void fireRowAdded() {
        batchTableListeners.each {BatchTableListener batchTableListener -> batchTableListener.fireRowAdded()}
    }

    public void fireRowDeleted(Object item) {
        batchTableListeners.each {BatchTableListener batchTableListener ->
            batchTableListener.fireRowDeleted(item.getSimulationRun())
        }
    }


    public void registerModel(AbstractUIItem item, def model) {
        viewModelsInUse[item] = model
        if (model instanceof IModelChangedListener) {
            addModelChangedListener(model)
        }
    }

    private def unregisterModel(AbstractUIItem item) {
        def viewModel = viewModelsInUse.remove(item)
        if (viewModel != null) {
            if (viewModel instanceof SimulationConfigurationModel) {
                viewModel.actionsPaneModel.removeSimulationListener(this)
                removeModelChangedListener(viewModel.settingsPaneModel)
            }
            if (viewModel instanceof IModelChangedListener) {
                removeModelChangedListener(viewModel)
            }
        }

    }

    void addModelListener(IP1RATModelListener listener) {
        if (!modelItemlisteners.contains(listener))
            modelListeners << listener
    }

    void notifyOpenDetailView(Model model, Object item) {
        modelListeners.each {IP1RATModelListener listener ->
            listener.openDetailView(model, item)
        }
    }


    void notifyCloseDetailView(Model model, AbstractUIItem item) {
        modelListeners.each {IP1RATModelListener listener ->
            listener.closeDetailView(model, item)
        }
    }

    void notifyChangedDetailView(Model model, AbstractUIItem item) {
        setCurrentItem(item)
        modelListeners.each {IP1RATModelListener listener ->
            listener.changedDetailView(model, item)
        }
    }

    public void setCurrentItem(def currentItem) {
        this.currentItem = (currentItem instanceof BatchUIItem) ? null : currentItem
        //todo fja refactoring of action
        switchActions.each {
            boolean b = (this.currentItem instanceof ParameterizationUIItem) || (this.currentItem instanceof ResultUIItem)
            it.setEnabled(b)
            it.selected = b
        }
    }

    ModellingUIItem getAbstractUIItem(ModellingItem modellingItem) {
        ModellingUIItem item = null
        viewModelsInUse.keySet().findAll {it instanceof ModellingUIItem}.each {ModellingUIItem openedUIItem ->
            if (modellingItem.id == openedUIItem.item.id) {
                item = openedUIItem
            }
        }
        return item
    }

    public void startPollingTimer(PollingBatchSimulationAction pollingBatchSimulationAction) {
        if (pollingBatchSimulationAction == null)
            pollingBatchSimulationAction = new PollingBatchSimulationAction()
        try {
            pollingBatchSimulationAction.addSimulationListener this
            pollingBatchSimulationTimer = new ULCPollingTimer(2000, pollingBatchSimulationAction)
            pollingBatchSimulationTimer.repeats = true
            pollingBatchSimulationTimer.syncClientState = false
            pollingBatchSimulationTimer.start()
        } catch (NullPointerException ex) {}
    }

}
