package org.pillarone.riskanalytics.application.dataaccess.function

import groovy.transform.CompileStatic
import org.pillarone.riskanalytics.application.ui.result.model.ResultTableTreeNode
import org.pillarone.riskanalytics.core.dataaccess.ResultAccessor
import org.pillarone.riskanalytics.core.output.PostSimulationCalculation
import org.pillarone.riskanalytics.core.output.QuantilePerspective
import org.pillarone.riskanalytics.core.output.SimulationRun

@CompileStatic
class VarFunction extends AbstractQuantilePerspectiveBasedFunction<Double> {

    final String name ='VaR'
    private double varValue

    VarFunction(double varValue, QuantilePerspective quantilePerspective) {
        super(quantilePerspective)
        this.varValue = varValue
    }

    Double getParameter() {
        return varValue
    }

    @Override
    Double evaluateResult(SimulationRun simulationRun, int periodIndex, ResultTableTreeNode node) {
        return ResultAccessor.getVar(simulationRun, periodIndex, node.path, node.collector, node.field, varValue, quantilePerspective)
    }

    @Override
    String getKeyFigureName() {
        return quantilePerspective == QuantilePerspective.LOSS ? PostSimulationCalculation.PERCENTILE : PostSimulationCalculation.PERCENTILE_PROFIT
    }

}
