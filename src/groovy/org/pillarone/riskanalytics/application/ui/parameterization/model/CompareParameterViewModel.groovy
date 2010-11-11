package org.pillarone.riskanalytics.application.ui.parameterization.model

import com.ulcjava.base.application.tabletree.ITableTreeModel
import org.pillarone.riskanalytics.application.ui.base.model.AbstractModellingModel
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.simulation.item.ModelStructure
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.parameter.ParameterHolder
import org.pillarone.riskanalytics.core.simulation.item.parameter.StringParameterHolder

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class CompareParameterViewModel extends AbstractModellingModel {
    private CompareParameterizationTableTreeModel paramterTableTreeModel

    public CompareParameterViewModel(Model model, List<Parameterization> parameterizations, ModelStructure structure) {
        super(model, parameterizations, structure);
    }

    protected ITableTreeModel buildTree() {
        Parameterization firstParameterization = getFirstObject()
        aggregateParameters(firstParameterization)
        builder = new CompareParameterizationTreeBuilder(model, structure, firstParameterization, getItems())
        periodCount = builder.minPeriod
        paramterTableTreeModel = new CompareParameterizationTableTreeModel(builder, getItems())
        paramterTableTreeModel.simulationModel = model
        paramterTableTreeModel.readOnly = false

        return paramterTableTreeModel

    }

    private void aggregateParameters(Parameterization firstParameterization) {
        getItems().eachWithIndex {Parameterization parameterization, int index ->
            if (index > 0) {
                parameterization.getParameterHolders().each {ParameterHolder parameterHolder ->
                    def list = firstParameterization.getParameters().findAll {ParameterHolder parameter ->
                        parameter.path == parameterHolder.path
                    }
                    if (!list || list.size() == 0) {
                        firstParameterization.addParameter new StringParameterHolder(parameterHolder.path, parameterHolder.periodIndex, "")
                    }
                }
            }
        }
    }

    public int getColumnCount() {
        return paramterTableTreeModel.getColumnCount()
    }

    private List getItems() {
        return (item.get(0) instanceof Parameterization) ? item : item*.item
    }

    private Object getFirstObject() {
        return (item.get(0) instanceof Parameterization) ? item.get(0) : item.get(0).item
    }


}
