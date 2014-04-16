package org.pillarone.riskanalytics.application.ui.main.view

import groovy.beans.Bindable
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.UlcSessionScope
import org.pillarone.riskanalytics.application.ui.base.model.AbstractModellingModel
import org.pillarone.riskanalytics.application.ui.base.model.AbstractPresentationModel
import org.pillarone.riskanalytics.application.ui.base.model.IModelChangedListener
import org.pillarone.riskanalytics.application.ui.base.view.IModelItemChangeListener
import org.pillarone.riskanalytics.application.ui.batch.model.BatchTableListener
import org.pillarone.riskanalytics.application.ui.main.model.IRiskAnalyticsModelListener
import org.pillarone.riskanalytics.application.ui.main.view.item.*
import org.pillarone.riskanalytics.application.ui.search.IModellingItemEventListener
import org.pillarone.riskanalytics.application.ui.search.ModellingItemCache
import org.pillarone.riskanalytics.application.ui.search.ModellingItemEvent
import org.pillarone.riskanalytics.application.ui.simulation.model.IBatchListener
import org.pillarone.riskanalytics.application.ui.simulation.model.INewSimulationListener
import org.pillarone.riskanalytics.application.ui.simulation.model.impl.SimulationConfigurationModel
import org.pillarone.riskanalytics.core.BatchRun
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.annotation.Resource
import java.beans.PropertyChangeListener

@Scope(UlcSessionScope.ULC_SESSION_SCOPE)
@Component
class RiskAnalyticsMainModel extends AbstractPresentationModel {

    private static final Log LOG = LogFactory.getLog(RiskAnalyticsMainModel)

    Map<AbstractUIItem, Object> viewModelsInUse
    def switchActions = []
    private List<IRiskAnalyticsModelListener> modelListeners = []
    private List<BatchTableListener> batchTableListeners = []
    private List<IModelItemChangeListener> modelItemListeners = []
    private List<INewSimulationListener> newSimulationListeners = []
    private List<IBatchListener> batchListeners = []
    private List<IModellingItemEventListener> modellingItemEventListeners = []

    @Bindable
    AbstractUIItem currentItem

    @Resource
    ModellingItemCache modellingItemCache
    private final MyModelItemEventListener listener = new MyModelItemEventListener()

    public RiskAnalyticsMainModel() {
        viewModelsInUse = [:]
        addPropertyChangeListener('currentItem', { def event ->
            switchActions.each {
                boolean b = (this.currentItem instanceof ParameterizationUIItem) || (this.currentItem instanceof SimulationResultUIItem)
                it.setEnabled(b)
                it.selected = b
            }
            notifyChangedWindowTitle(currentItem)
        } as PropertyChangeListener)
    }

    @PostConstruct
    void initialize() {
        modellingItemCache.addItemEventListener(listener)
    }

    @PreDestroy
    void unregister() {
        modellingItemCache.removeItemEventListener(listener)
    }

    void setCurrentItem(AbstractUIItem currentItem) {
        this.currentItem = (currentItem instanceof BatchUIItem) ? null : currentItem
    }

    void saveAllOpenItems() {
        viewModelsInUse.keySet().each { AbstractUIItem item ->
            item.save()
        }
    }

    void removeItems(Model selectedModel, List<AbstractUIItem> modellingItems) {
        closeItems(selectedModel, modellingItems)
        try {
            for (AbstractUIItem item : modellingItems) {
                item.remove()
                item.delete()
            }
        } catch (Exception ex) {
            LOG.error "Deleting Item Failed: ${ex}"
        }
        fireModelChanged()
    }

    AbstractModellingModel getViewModel(AbstractUIItem item) {
        if (viewModelsInUse.containsKey(item)) {
            return viewModelsInUse[item] as AbstractModellingModel
        }
        return item.viewModel as AbstractModellingModel
    }


    void openItem(Model model, AbstractUIItem item) {
        if (!item.loaded) {
            item.load()
        }
        notifyOpenDetailView(model, item)
    }

    void closeItem(Model model, AbstractUIItem abstractUIItem) {
        notifyCloseDetailView(model, abstractUIItem)
        unregisterModel(abstractUIItem)
        if (abstractUIItem instanceof ModellingUIItem) {
            abstractUIItem.removeAllModellingItemChangeListener()
        }
    }


    private void closeItems(Model selectedModel, List<AbstractUIItem> items) {
        for (AbstractUIItem item : items) {
            closeItem(selectedModel, item)
        }
    }

    void addModelItemChangedListener(IModelItemChangeListener listener) {
        modelItemListeners << listener
    }

    void removeModelItemChangedListener(IModelItemChangeListener listener) {
        modelItemListeners.remove(listener)
    }

    void fireModelItemChanged() {
        modelItemListeners.each { IModelItemChangeListener listener -> listener.modelItemChanged() }
    }

    void addBatchListener(IBatchListener listener) {
        batchListeners << listener
    }

    void removeBatchListener(IBatchListener listener) {
        batchListeners.remove(listener)
    }

    void addModellingItemEventListener(IModellingItemEventListener listener) {
        modellingItemEventListeners << listener
    }

    void removeModellingItemEventListener(IModellingItemEventListener listener) {
        modellingItemEventListeners.remove(listener)
    }

    void fireModelItemEvent(ModellingItemEvent modellingItemEvent) {
        modellingItemEventListeners.each { it.onEvent(modellingItemEvent) }
    }

    void fireBatchAdded(BatchRun batchRun) {
        batchListeners.each { it.newBatchAdded(batchRun) }
    }

    void addBatchTableListener(BatchTableListener batchTableListener) {
        batchTableListeners << batchTableListener
    }

    void fireRowAdded(Simulation addedRun) {
        batchTableListeners.each { BatchTableListener batchTableListener -> batchTableListener.fireRowAdded(addedRun) }
    }

    void fireRowDeleted(Simulation item) {
        batchTableListeners.each { BatchTableListener batchTableListener ->
            batchTableListener.fireRowDeleted(item)
        }
    }

    void registerModel(AbstractUIItem item, def model) {
        viewModelsInUse[item] = model
        if (model instanceof SimulationConfigurationModel) {
            //TODO the viewModel itself should implement the interfaces. Then this could be generic
            addModelChangedListener(model.simulationProfilePaneModel.simulationActionsPaneModel)
            addModellingItemEventListener(model.settingsPaneModel)
            addNewSimulationListener(model)
        }
        if (model instanceof IModelChangedListener) {
            addModelChangedListener(model)
        }
        if (model instanceof IBatchListener) {
            addBatchListener(model)
        }
    }

    private def unregisterModel(AbstractUIItem item) {
        def viewModel = viewModelsInUse.remove(item)
        if (viewModel != null) {
            if (viewModel instanceof SimulationConfigurationModel) {
                //TODO the viewModel itself should implement the interfaces. Then this could be generic
                removeModelChangedListener(viewModel.simulationProfilePaneModel.simulationActionsPaneModel)
                removeModellingItemEventListener(viewModel.settingsPaneModel)
                removeNewSimulationListener(viewModel)
            }
            if (viewModel instanceof IModelChangedListener) {
                removeModelChangedListener(viewModel)
            }
            if (viewModel instanceof IBatchListener) {
                removeBatchListener(viewModel)
            }
        }
    }

    void addModelListener(IRiskAnalyticsModelListener listener) {
        if (!modelListeners.contains(listener)) {
            modelListeners << listener
        }
    }

    void notifyOpenDetailView(Model model, AbstractUIItem item) {
        modelListeners.each { IRiskAnalyticsModelListener listener ->
            listener.openDetailView(model, item)
        }
    }

    void notifyOpenDetailView(Model model, ModellingItem item) {
        modelListeners.each { IRiskAnalyticsModelListener listener ->
            listener.openDetailView(model, item)
        }
    }

    void notifyCloseDetailView(Model model, AbstractUIItem item) {
        modelListeners.each { IRiskAnalyticsModelListener listener ->
            listener.closeDetailView(model, item)
        }
    }

    void notifyChangedDetailView(Model model, AbstractUIItem item) {
        setCurrentItem(item)
        modelListeners.each { IRiskAnalyticsModelListener listener ->
            listener.changedDetailView(model, item)
        }
    }

    void notifyChangedWindowTitle(AbstractUIItem abstractUIItem) {
        modelListeners.each { IRiskAnalyticsModelListener listener ->
            listener.windowTitle = abstractUIItem
        }
    }

    void addNewSimulationListener(INewSimulationListener newSimulationListener) {
        newSimulationListeners << newSimulationListener
    }

    void removeNewSimulationListener(INewSimulationListener newSimulationListener) {
        newSimulationListeners.remove(newSimulationListener)
    }

    void fireNewSimulation(Simulation simulation) {
        newSimulationListeners.each { INewSimulationListener newSimulationListener ->
            newSimulationListener.newSimulation(simulation)
        }
    }

    ModellingUIItem getAbstractUIItem(ModellingItem modellingItem) {
        ModellingUIItem item = null
        viewModelsInUse.keySet().findAll { it instanceof ModellingUIItem }.each { ModellingUIItem openedUIItem ->
            if (modellingItem.class == openedUIItem.item.class && modellingItem.id == openedUIItem.item.id) {
                item = openedUIItem
            }
        }
        return item
    }

    boolean isItemOpen(AbstractUIItem item) {
        viewModelsInUse.containsKey(item)
    }

    private class MyModelItemEventListener implements IModellingItemEventListener {
        @Override
        void onEvent(ModellingItemEvent event) {
            fireModelItemEvent(event)
        }
    }
}
