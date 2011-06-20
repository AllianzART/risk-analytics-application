package org.pillarone.riskanalytics.application.ui.main.view.item

import com.ulcjava.testframework.operator.ComponentByNameChooser
import com.ulcjava.testframework.operator.ULCFrameOperator
import models.application.ApplicationModel
import org.pillarone.riskanalytics.application.dataaccess.item.ModellingItemFactory
import org.pillarone.riskanalytics.application.fileimport.ResultStructureImportService
import org.pillarone.riskanalytics.application.ui.main.view.RiskAnalyticsMainModel
import org.pillarone.riskanalytics.core.fileimport.ModelStructureImportService
import org.pillarone.riskanalytics.core.fileimport.ParameterizationImportService
import org.pillarone.riskanalytics.core.fileimport.ResultConfigurationImportService
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.output.DBCleanUpService
import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.core.simulation.item.Simulation

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class StochasticResultUIItemTests extends AbstractSimulationUIItemTest {

    public void testView() {
        ULCFrameOperator frameOperator = new ULCFrameOperator(new ComponentByNameChooser("test"))
        assertNotNull frameOperator
//        Thread.sleep 5000
    }

    @Override
    AbstractUIItem createUIItem() {
        new DBCleanUpService().cleanUp()
        new ParameterizationImportService().compareFilesAndWriteToDB(['ApplicationParameters'])
        new ResultConfigurationImportService().compareFilesAndWriteToDB(['ApplicationResultConfiguration'])
        new ModelStructureImportService().compareFilesAndWriteToDB(['ApplicationStructure'])
        new ResultStructureImportService().compareFilesAndWriteToDB(['ApplicationDefaultResultTree'])

        ModellingItemFactory.clear()

        init()

        SimulationRun run1 = createResults("run1", 1)

        Simulation simulation1 = new Simulation("run1")
        simulation1.load()

        Model model = new ApplicationModel()

        RiskAnalyticsMainModel mainModel = new RiskAnalyticsMainModel()
        StochasticResultUIItem uiItem = new StochasticResultUIItem(mainModel, model, simulation1)
        return uiItem
    }


}
