package org.pillarone.riskanalytics.application.ui.parameterization.model

import com.ulcjava.base.application.tabletree.AbstractTableTreeModel
import com.ulcjava.base.application.tabletree.ITableTreeNode
import com.ulcjava.base.application.util.Color
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.pillarone.riskanalytics.core.simulation.item.parameter.ParameterObjectParameterHolder

/**
 * User: Fouad Jaada
 */

public class CompareParameterizationTableTreeModel extends AbstractTableTreeModel {
    private List<Parameterization> parameterizations
    ParameterizationTreeBuilder builder
    Model simulationModel
    ITableTreeNode root

    private List valueChangedListeners = []
    Boolean readOnly = false
    Map nonValidValues = [:]
    int minPeriod = -1
    List differentsNode = []


    public CompareParameterizationTableTreeModel() {
    }

    public CompareParameterizationTableTreeModel(ParameterizationTreeBuilder builder, List<Parameterization> parameterizations) {
        this.parameterizations = parameterizations
        this.builder = builder
        this.root = builder.root
        minPeriod = ParameterizationUtilities.getMinPeriod(parameterizations)
    }


    public int getColumnCount() {
        return parameterizations.size() * minPeriod + 1;
    }

    public String getColumnName(int column) {
        if (column == 0) {
            return "Name"
        }

        if (builder != null) {
            return builder.item.getPeriodLabel(getPeriodIndex(column)) + ": " + getParameterizationName(column)
        }
        return null
    }

    protected int getParameterizationIndex(int column) {
        if (column == 0)
            return 0
        return (column - 1) % (getColumnCount() - 1)
    }

    protected String getParameterizationName(int column) {
        Parameterization parameterization = parameterizations.get((column - 1) % parameterizations.size())
        return parameterization.name + " v" + parameterization.versionNumber
    }


    protected int getPeriodIndex(int column) {
        return (column - 1).intdiv(parameterizations.size())
    }

    public int getParameterizationsSize() {
        return parameterizations.size()
    }

    public boolean isDifferent(Object node) {
        boolean different = false
        for (int i = 1; i < getColumnCount(); i += getParameterizationsSize()) {
            def refObject = getValueAt(node, i)
            for (int j = 1; j < 2 || j < getParameterizationsSize(); j++) {
                def object = getValueAt(node, i + j)
                if (refObject != object) {
                    different = true
                    differentsNode << node
                    break;
                }
            }
        }
        return different
    }

    public Object getValueAt(Object node, int i) {
        def value
        if (nonValidValues[[node, i]] != null) {
            value = nonValidValues[[node, i]]
        } else {
            value = node.getValueAt(i)
        }
        return value
    }

    public Object getRoot() {
        return root;
    }

    public Object getChild(Object parent, int i) {
        return parent.getChildAt(index)
    }

    public int getChildCount(Object node) {
        return node.childCount
    }

    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0
    }

    public int getIndexOfChild(Object parent, Object child) {
        return parent.getIndex(child)
    }


}

class ParameterizationUtilities {

    public static final Color ERROR_BG = new Color(255, 153, 153)

    public static int getMinPeriod(List<Parameterization> parameterizations) {
        int minPeriod = parameterizations.get(0).periodCount
        for (Parameterization p: parameterizations) {
            if (p.periodCount < minPeriod)
                minPeriod = p.periodCount
        }
        return minPeriod
    }

    public static boolean isParameterObjectParameter(Map parametersMap) {
        boolean isPOP = false
        parametersMap.each {k, v ->
            v.each {
                if (it instanceof ParameterObjectParameterHolder) {
                    isPOP = true
                }
            }
        }
        return isPOP
    }



    public static List getParameterList(Map parametersMap) {
        List result
        parametersMap.each {k, List parameters ->
            parameters.each {
                if (it != null) {
                    result = parameters
                }
            }
        }
        return result
    }

    public static List getParameters(List<Simulation> simulations) {
        List parameters = []
        simulations.each {Simulation simulation ->
            Parameterization parameterization = simulation.parameterization
            if (!parameterization.isLoaded())
                parameterization.load()
            parameters << parameterization
        }
        return parameters
    }


}