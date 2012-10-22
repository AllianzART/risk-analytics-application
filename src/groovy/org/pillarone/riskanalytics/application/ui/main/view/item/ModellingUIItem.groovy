package org.pillarone.riskanalytics.application.ui.main.view.item

import com.ulcjava.base.application.tabletree.AbstractTableTreeModel
import com.ulcjava.base.application.tabletree.DefaultTableTreeModel
import com.ulcjava.base.application.tabletree.ITableTreeNode
import com.ulcjava.base.application.tree.TreePath
import org.pillarone.riskanalytics.application.dataaccess.item.ModellingItemFactory
import org.pillarone.riskanalytics.application.ui.main.view.RiskAnalyticsMainModel
import org.pillarone.riskanalytics.application.ui.util.ExceptionSafe
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.core.simulation.item.IModellingItemChangeListener
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.pillarone.riskanalytics.application.ui.main.view.MarkItemAsUnsavedListener
import org.apache.commons.lang.builder.HashCodeBuilder
import org.pillarone.riskanalytics.application.ui.base.model.ItemNode
import com.ulcjava.base.application.ULCWindow
import org.pillarone.riskanalytics.core.simulation.item.Resource

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
abstract class ModellingUIItem extends AbstractUIItem {
    ModellingItem item

    public ModellingUIItem(RiskAnalyticsMainModel mainModel, Model simulationModel, ModellingItem item) {
        super(mainModel, simulationModel)
        this.item = item
    }


    @Override
    public boolean isLoaded() {
        return ((ModellingItem) item).isLoaded()
    }

    @Override
    public void load(boolean completeLoad) {
        ((ModellingItem) item).load(completeLoad)
    }

    @Override
    public void unload() {
        ((ModellingItem) item).unload()
    }

    public boolean isUsedInSimulation() {
        return ((ModellingItem) item).isUsedInSimulation()
    }

    @Override
    String createTitle() {
        String title = "$item.name v${item.versionNumber.toString()}".toString()
        if (item.changed)
            title += MarkItemAsUnsavedListener.UNSAVED_MARK
        return title
    }

    public boolean deleteDependingResults(Model model) {
        return UIItemUtils.deleteDependingResults(mainModel, model, this)
    }

    public ModellingUIItem createNewVersion(Model selectedModel, boolean openNewVersion = true) {
        ModellingItem modellingItem = null
        item.daoClass.withTransaction {status ->
            if (!item.isLoaded())
                item.load()
            modellingItem = ModellingItemFactory.incrementVersion(item)
        }
        mainModel.fireModelChanged()
        AbstractUIItem modellingUIItem = UIItemFactory.createItem(modellingItem, selectedModel, mainModel)
        navigationTableTreeModel.addNodeForItem(modellingUIItem)
        if (openNewVersion)
            mainModel.openItem(selectedModel, modellingUIItem)
        return modellingUIItem
    }

    Object getItem() {
        return item
    }

    @Override
    public boolean remove() {
        if (ModellingItemFactory.delete(item)) {
            ModellingUIItem openedItem = mainModel.getAbstractUIItem(item)
            if (openedItem)
                mainModel.closeItem(model, openedItem)
            navigationTableTreeModel.removeNodeForItem(this)
            ModellingItemFactory.remove(item)
            mainModel.fireModelChanged()
            if (item instanceof Simulation) mainModel.fireRowDeleted(item)
            return true
        }
        return false
    }

    @Override
    void rename(String newName) {
        item.daoClass.withTransaction {status ->
            if (!item.isLoaded())
                item.load()
            ITableTreeNode itemNode = navigationTableTreeModel.findNodeForItem(navigationTableTreeModel.root, this)

            itemNode.userObject = newName

            navigationTableTreeModel.nodeChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(itemNode) as Object[]))
            renameAllChildren(itemNode, name)
            mainModel.fireModelChanged()
        }
    }

    private void renameAllChildren(ITableTreeNode itemNode, String name) {
        if (((ItemNode) itemNode).abstractUIItem instanceof ResultUIItem) return
        itemNode.childCount.times {
            ItemNode childNode = itemNode.getChildAt(it)
            ((ModellingUIItem) childNode.abstractUIItem).rename(name)
        }
    }


    @Override
    void save() {
        ExceptionSafe.protect {
            item.save()
        }
        mainModel.fireModelChanged()
        mainModel.fireModelItemChanged()
        navigationTableTreeModel.itemChanged(item)
    }



    public void addItem(ModellingUIItem modellingUIItem, String name) {
        modellingUIItem.item.daoClass.withTransaction {status ->
            if (!modellingUIItem.isLoaded())
                modellingUIItem.load()
            ModellingItem newItem = ModellingItemFactory.copyItem(modellingUIItem.item, name)
            newItem.id = null
            mainModel.fireModelChanged()
            Model modelInstance = modellingUIItem.model
            if (!(newItem instanceof Resource)) { //re-create model (PMO-1961) - do nothing if it's a resource
                modelInstance = newItem?.modelClass?.newInstance()
                modelInstance?.init()
            }
            navigationTableTreeModel.addNodeForItem(UIItemFactory.createItem(newItem, modelInstance, mainModel))
        }
    }



    public void importItem() {
        mainModel.fireModelChanged()
        navigationTableTreeModel.addNodeForItem(this)
    }

    @Override
    void removeAllModellingItemChangeListener() {
        item.removeAllModellingItemChangeListener()
    }

    @Override
    public void addModellingItemChangeListener(IModellingItemChangeListener listener) {
        item.addModellingItemChangeListener(listener)
    }

    @Override
    boolean isChanged() {
        return item.changed
    }

    String getName() {
        return item.name
    }

    @Override
    String getNameAndVersion() {
        return getName() + (versionable ? " v" + item.versionNumber.toString() : "")
    }



    public Model getModel() {
        if (!this.@model) {
            this.model = item.modelClass.newInstance()
            this.model.init()
        }
        return this.@model
    }

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof ModellingUIItem)) return false
        return item.modelClass == obj.item.modelClass && item.name == obj.item.name && item.versionNumber.toString() == obj.item.versionNumber.toString()
    }

    @Override
    int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder()
        hcb.append(item.modelClass.toString())
        hcb.append(item.modelClass.name)
        hcb.append(item.versionNumber.toString())
        return hcb.toHashCode()
    }


}
