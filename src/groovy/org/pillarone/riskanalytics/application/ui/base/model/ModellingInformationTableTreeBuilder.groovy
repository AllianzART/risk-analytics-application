package org.pillarone.riskanalytics.application.ui.base.model

import com.ulcjava.base.application.tabletree.AbstractTableTreeModel
import com.ulcjava.base.application.tabletree.DefaultMutableTableTreeNode
import com.ulcjava.base.application.tabletree.DefaultTableTreeModel
import com.ulcjava.base.application.tabletree.ITableTreeNode
import com.ulcjava.base.application.tree.TreePath
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.dataaccess.item.ModellingItemFactory
import org.pillarone.riskanalytics.application.ui.main.view.RiskAnalyticsMainModel
import org.pillarone.riskanalytics.application.ui.parameterization.model.BatchRootNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.BatchRunNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.ParameterizationNode
import org.pillarone.riskanalytics.application.ui.parameterization.model.WorkflowParameterizationNode
import org.pillarone.riskanalytics.application.ui.result.model.SimulationNode
import org.pillarone.riskanalytics.application.ui.resulttemplate.model.ResultConfigurationNode
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.BatchRun
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.output.batch.BatchRunner
import org.pillarone.riskanalytics.core.workflow.Status
import static org.pillarone.riskanalytics.application.ui.base.model.TableTreeBuilderUtils.*
import org.pillarone.riskanalytics.application.ui.main.view.item.*
import org.pillarone.riskanalytics.core.simulation.item.*

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class ModellingInformationTableTreeBuilder {

    DefaultMutableTableTreeNode root
    ModellingInformationTableTreeModel model
    RiskAnalyticsMainModel mainModel
    static Log LOG = LogFactory.getLog(ModellingInformationTableTreeBuilder)

    static final int PARAMETERIZATION_NODE_INDEX = 0
    static final int RESULT_CONFIGURATION_NODE_INDEX = 1
    static final int SIMULATION_NODE_INDEX = 2

    public ModellingInformationTableTreeBuilder(AbstractTableTreeModel model) {
        this.model = model;
        root = new DefaultMutableTableTreeNode("root")
    }

    public ModellingInformationTableTreeBuilder(ModellingInformationTableTreeModel model, RiskAnalyticsMainModel mainModel) {
        this.model = model;
        this.mainModel = mainModel
        root = new DefaultMutableTableTreeNode("root")
    }

    public def buildTreeNodes() {
        getAllModelClasses().each {Class modelClass ->
            Model model = modelClass.newInstance()
            model.init()
            ITableTreeNode modelNode = getModelNode(model)
            DefaultMutableTableTreeNode parametrisationsNode = modelNode.getChildAt(PARAMETERIZATION_NODE_INDEX)
            DefaultMutableTableTreeNode resultConfigurationsNode = modelNode.getChildAt(RESULT_CONFIGURATION_NODE_INDEX)
            DefaultMutableTableTreeNode simulationsNode = modelNode.getChildAt(SIMULATION_NODE_INDEX)

            getItemMap(getItemsForModel(modelClass, Parameterization), false).values().each { List<Parameterization> it ->
                parametrisationsNode.add(createItemNodes(model, it))
            }
            getItemMap(getItemsForModel(modelClass, Parameterization), true).values().each { List<Parameterization> it ->
                parametrisationsNode.add(createItemNodes(model, it))
            }

            getItemMap(getItemsForModel(modelClass, ResultConfiguration), false).values().each {
                resultConfigurationsNode.add(createItemNodes(model, it))
            }

            List simulationsForModel = getItemsForModel(modelClass, Simulation)
            if (simulationsForModel.size() == 0) {
                simulationsNode.leaf = true
            }
            simulationsForModel.each {
                try {
                    simulationsNode.add(createNode(model, it))
                } catch (Throwable t) {
                    LOG.error "Could not create node for ${it.toString()}", t
                }
            }
            root.add(modelNode)
        }
        root.add(createBatchNode())
    }

    public List getItemsForModel(Class modelClass, Class clazz) {
        switch (clazz) {
            case Parameterization: return ModellingItemFactory.getParameterizationsForModel(modelClass)
            case ResultConfiguration: return ModellingItemFactory.getResultConfigurationsForModel(modelClass)
            case Simulation: return ModellingItemFactory.getActiveSimulationsForModel(modelClass)
            default: return []
        }
    }

    public List getAllModelClasses() {
        return ModelStructure.findAllModelClasses()
    }

    private ITableTreeNode getModelNode(Model model) {
        DefaultMutableTableTreeNode modelNode = null

        for (int i = 0; i < root.childCount && modelNode == null; i++) {
            ItemNode candidate = root.getChildAt(i)
            if (candidate.getItemClass() == model.class) {
                modelNode = candidate
            }
        }

        if (modelNode == null) {
            modelNode = new ModelNode(new ModelUIItem(mainModel, model))
            DefaultMutableTableTreeNode parameterizationsNode = new ItemGroupNode(UIUtils.getText(ModellingInformationTableTreeModel.class, "Parameterization"), Parameterization, mainModel)
            DefaultMutableTableTreeNode resultConfigurationsNode = new ItemGroupNode(UIUtils.getText(ModellingInformationTableTreeModel.class, "ResultTemplates"), ResultConfiguration, mainModel)
            DefaultMutableTableTreeNode simulationsNode = new ItemGroupNode(UIUtils.getText(ModellingInformationTableTreeModel.class, "Results"), Simulation, mainModel)
            modelNode.add(parameterizationsNode)
            modelNode.add(resultConfigurationsNode)
            modelNode.add(simulationsNode)
        }

        return modelNode
    }

    private Map getItemMap(items, boolean workflow) {
        Map map = [:]
        if (workflow) {
            items = items.findAll { it.versionNumber.toString().startsWith("R")}
        } else {
            items = items.findAll { !it.versionNumber.toString().startsWith("R")}
        }
        items.each {
            def list = map.get(it.name)
            if (!list) {
                list = []
                list.add(it)
                map.put(it.name, list)
            } else {
                list.add(it)
            }
        }
        map
    }

    private def createItemNodes(Model selectedModel, List items) {
        def tree = []
        tree.addAll(items)
        tree.sort {a, b -> b.versionNumber <=> a.versionNumber }

        def root = createNode(selectedModel, tree.first())
        tree.remove(tree.first())
        root.leaf = tree.empty

        def secondLevelNodes = tree.findAll { it.versionNumber.level == 1}
        secondLevelNodes.each {
            def node = createNode(selectedModel, it)
            createSubNodes(tree, node, selectedModel)
            root.add(node)
        }

        root
    }

    private void createSubNodes(def tree, ItemNode node, Model selectedModel) {
        def currentLevelNodes = tree.findAll {ModellingItem it ->
            it.versionNumber.isDirectChildVersionOf(node.versionNumber)
        }
        node.leaf = currentLevelNodes.size() == 0
        currentLevelNodes.each {
            def newNode = createNode(selectedModel, it)
            node.add(newNode)
            createSubNodes(tree, newNode, selectedModel)
        }
    }

    public void order(def comparator) {
        root.childCount.times {childIndex ->
            def modelNode = root.getChildAt(childIndex)
            if (modelNode instanceof ModelNode) {
                DefaultMutableTableTreeNode parameterizationNode = modelNode.getChildAt(PARAMETERIZATION_NODE_INDEX)
                List nodes = []
                parameterizationNode.childCount.times { parameterizationnodeIndex ->
                    ParameterizationNode node = parameterizationNode.getChildAt(parameterizationnodeIndex)
                    nodes << node
                }

                parameterizationNode.removeAllChildren()
                model.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(parameterizationNode) as Object[]))

                nodes.sort(comparator)
                nodes.each {
                    parameterizationNode.add(it)
                }
                model.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(parameterizationNode) as Object[]))
            }
        }
    }

    public void refresh() {
        root.removeAllChildren()
        buildTreeNodes()
        model.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(root) as Object[]))
    }

    public ITableTreeNode refresh(AbstractUIItem item) {
        def node = findNodeForItem(findModelNode(root, item), item)
        model?.nodeChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(node) as Object[]))
        return node
    }

    //todo fja add selected model as arg

    public void addNodeForItem(Simulation item, Model selectedModel = null) {
        DefaultMutableTableTreeNode groupNode = findGroupNode(item, findModelNode(root, item))
        groupNode.leaf = false
        insertNodeInto(createNode(selectedModel, item), groupNode)
    }

    //todo fja add selected model as arg

    public def addNodeForItem(ModellingUIItem modellingUIItem, Model selectedModel = null) {
        ITableTreeNode groupNode = findGroupNode(modellingUIItem, findModelNode(root, modellingUIItem))
        createAndInsertItemNode(groupNode, modellingUIItem)
        model.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(groupNode) as Object[]))
        return groupNode
    }

    //todo fja add selected model as arg

    public def addNodeForItem(BatchUIItem batchRun, Model selectedModel = null) {
        ITableTreeNode groupNode = findBatchRootNode(root)
        createAndInsertItemNode(groupNode, batchRun)
        model.nodesWereInserted(new TreePath(DefaultTableTreeModel.getPathToRoot(groupNode) as Object[]), [groupNode.childCount - 1] as int[])
        return groupNode
    }


    public void removeAllGroupNodeChildren(ItemGroupNode groupNode) {
        groupNode.removeAllChildren()
        model.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(groupNode) as Object[]))
    }

    public void removeNodeForItem(ModellingUIItem modellingUIItem) {
        ITableTreeNode groupNode = findGroupNode(modellingUIItem, findModelNode(root, modellingUIItem))
        def itemNode = findNodeForItem(groupNode, modellingUIItem)
        if (!itemNode) return
        if (itemNode instanceof SimulationNode) {
            itemNode.removeAllChildren()
        } else {
            if (itemNode.childCount > 0) {
                def parent = itemNode.parent
                def firstChild = itemNode.getChildAt(0)
                parent.add(firstChild)
                def children = []
                for (int i = 0; i < itemNode.childCount; i++) {
                    children << itemNode.getChildAt(i)
                }
                if (children.size() > 0) {
                    firstChild.leaf = false
                }
                children.each {
                    firstChild.add(it)
                }
                itemNode.removeAllChildren()
            }
        }
        removeNodeFromParent(itemNode)
    }

    public void removeNodeForItem(BatchUIItem batchUIItem) {
        ITableTreeNode groupNode = findBatchRootNode(root)
        ITableTreeNode itemNode = findNodeForItem(groupNode, batchUIItem)
        removeNodeFromParent(itemNode)
    }

    public void refreshBatchNode() {
        ITableTreeNode batchNode = findBatchRootNode()

        removeNodeFromParent(batchNode)
        model.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(root) as Object[]))
        root.add(createBatchNode())
        model.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(root) as Object[]))
    }

    protected def createAndInsertItemNode(DefaultMutableTableTreeNode node, ModellingUIItem modellingUIItem) {
        boolean parameterNameFound = false
        for (int i = 0; i < node.childCount; i++) {
            if (modellingUIItem.name.equals(node.getChildAt(i).abstractUIItem.name)) {
                parameterNameFound = true
                if (modellingUIItem.isVersionable() && modellingUIItem.item.versionNumber.level > 1) {
                    insertSubversionItemNode(node.getChildAt(i), createNode(modellingUIItem))
                } else {
                    DefaultMutableTableTreeNode childNode = node.getChildAt(i)
                    DefaultMutableTableTreeNode newNode = createNode(modellingUIItem)
                    def children = []
                    childNode.childCount.times {
                        children << childNode.getChildAt(it)
                    }
                    children.each {newNode.add(it)}
                    childNode.removeAllChildren()
                    childNode.leaf = true
                    if (childNode.abstractUIItem.isVersionable() && childNode.abstractUIItem.item.versionNumber.level == 1) {
                        newNode.insert(childNode, 0)
                    } else {
                        insertSubversionItemNode(newNode, childNode)
                    }
                    node.add(newNode)
                    return
                }
            }
        }

        if (!parameterNameFound) {
            def newNode = createNode(modellingUIItem)
            newNode.leaf = true
            node.leaf = false
            insertNodeInto(newNode, node)
        }
    }


    private def createAndInsertItemNode(DefaultMutableTableTreeNode node, BatchUIItem batchUIItem) {
        DefaultMutableTableTreeNode newNode = createNode(batchUIItem)
        node.add(newNode)
    }


    private void insertSubversionItemNode(DefaultMutableTableTreeNode node, DefaultMutableTableTreeNode newItemNode) {
        node.childCount.times {
            DefaultMutableTableTreeNode childNode = node.getChildAt(it)
            if (newItemNode.abstractUIItem.isVersionable() && newItemNode.abstractUIItem.item.versionNumber.toString().startsWith(childNode.abstractUIItem.item.versionNumber.toString())) {
                if (newItemNode.abstractUIItem.item.versionNumber.isDirectChildVersionOf(childNode.abstractUIItem.item.versionNumber)) {
                    childNode.leaf = false
                    newItemNode.leaf = true
                    childNode.insert(newItemNode, childNode.childCount)
                } else {
                    insertSubversionItemNode(childNode, newItemNode)
                }
            }
        }
    }

    private ITableTreeNode createNode(String name) {
        new DefaultMutableTableTreeNode(name)
    }

    private ITableTreeNode createNode(Model selectedModel, Parameterization item) {
        Model selectedModelInstance = getNewModelInstance(selectedModel)
        return createNode(new ParameterizationUIItem(mainModel, model, selectedModelInstance, item))
    }

    private ITableTreeNode createNode(ParameterizationUIItem parameterizationUIItem) {
        ParameterizationNode node = parameterizationUIItem.item.status == Status.NONE ? new ParameterizationNode(parameterizationUIItem) : new WorkflowParameterizationNode(parameterizationUIItem)
        ((ModellingInformationTableTreeModel) model).putValues(node)
        return node
    }

    private ITableTreeNode createNode(Model selectedModel, ResultConfiguration item) {
        Model selectedModelInstance = getNewModelInstance(selectedModel)
        return createNode(new ResultConfigurationUIItem(mainModel, model, selectedModelInstance, item))
    }

    private ITableTreeNode createNode(ResultConfigurationUIItem resultConfigurationUIItem) {
        ResultConfigurationNode node = new ResultConfigurationNode(resultConfigurationUIItem)
        ((ModellingInformationTableTreeModel) model).putValues(node)
        return node
    }

    private ITableTreeNode createNode(BatchUIItem batchUIItem) {
        return new BatchRunNode(batchUIItem)
    }

    private ITableTreeNode createNode(Model selectedModel, BatchRun batchRun) {
        return new BatchRunNode(new BatchUIItem(mainModel, model, batchRun))
    }

    private ITableTreeNode createNode(Model selectedModel, Simulation item) {
        SimulationNode node = null
        Model selectedModelInstance = getNewModelInstance(selectedModel)
        try {
            node = new SimulationNode(UIItemFactory.createItem(item, selectedModelInstance, mainModel, model))
            if (!item.isLoaded()) {
                item.load()
            }
            def paramsNode = createNode(selectedModelInstance, item.parameterization)
            paramsNode.leaf = true
            def templateNode = createNode(selectedModelInstance, item.template)
            templateNode.leaf = true

            node.add(paramsNode)
            node.add(templateNode)
            ((ModellingInformationTableTreeModel) model).putValues(node)
        } catch (Exception ex) {
            println "create simulation exception : ${ex}"
        }
        return node
    }

    protected ITableTreeNode createBatchNode(Model selectedModel) {
        BatchRootNode batchesNode = new BatchRootNode("Batches", mainModel)
        List<BatchRun> batchRuns = getAllBatchRuns()
        batchRuns?.each {BatchRun batchRun ->
            batchesNode.add(createNode(selectedModel, batchRun))
        }
        return batchesNode
    }

    protected List<BatchRun> getAllBatchRuns() {
        BatchRunner.getService().getAllBatchRuns()
    }

    private void insertNodeInto(DefaultMutableTableTreeNode newNode, DefaultMutableTableTreeNode parent) {
        parent.insert(newNode, parent.childCount)
        model.nodesWereInserted(new TreePath(DefaultTableTreeModel.getPathToRoot(parent) as Object[]), [parent.childCount - 1] as int[])
    }

    protected void removeNodeFromParent(DefaultMutableTableTreeNode itemNode) {
        DefaultMutableTableTreeNode parent = itemNode.getParent()
        parent.remove(parent.getIndex(itemNode))
        model.nodeChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(parent) as Object[]))
        model.nodeStructureChanged(new TreePath(DefaultTableTreeModel.getPathToRoot(parent) as Object[]))
    }

    Model getNewModelInstance(Model selectedModel) {
        Model selectedModelInstance = selectedModel.class.newInstance()
        selectedModelInstance.init()
        return selectedModelInstance
    }

}
