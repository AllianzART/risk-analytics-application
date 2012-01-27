package org.pillarone.riskanalytics.application.ui.resultnavigator.model

import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.core.dataaccess.ResultAccessor
import org.joda.time.DateTime
import com.ulcjava.base.application.IComboBoxModel
import com.ulcjava.base.application.DefaultComboBoxModel

/**
 * Data model underlying the panel for selecting the period and the statistics key figure.
 *
 * @author martin.melchior
 */
class KeyfigureSelectionModel {

    IComboBoxModel keyfigureModel
    Number keyfigureParameter
    IComboBoxModel periodSelectionModel

    private int numOfIterations
    private int numOfPeriods
    private DateTime startPeriod
    private DateTime endPeriod

    KeyfigureSelectionModel(SimulationRun run) {
        numOfIterations = run.iterations
        numOfPeriods = run.periodCount
        startPeriod = run.startTime
        endPeriod = run.endTime

        keyfigureModel = new DefaultComboBoxModel(StatisticsKeyfigure.getNames())
        periodSelectionModel = new DefaultComboBoxModel(0..<numOfPeriods)
    }

    StatisticsKeyfigure getKeyfigure() {
        return StatisticsKeyfigure.getEnumValue((String) keyfigureModel.selectedItem)
    }

    int getPeriod() {
        return (int) periodSelectionModel.selectedItem
    }
}
