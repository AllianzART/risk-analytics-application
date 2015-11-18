package org.pillarone.riskanalytics.application.ui.base.action

import com.ulcjava.base.application.IAction
import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.UlcUtilities
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.shared.FileChooserConfig
import net.sf.jmimemagic.Magic
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.document.ShowDocumentStrategyFactory
import org.pillarone.riskanalytics.application.reports.IReportableNode
import org.pillarone.riskanalytics.application.ui.base.model.ItemNode
import org.pillarone.riskanalytics.application.ui.main.action.SelectionTreeAction
import org.pillarone.riskanalytics.application.ui.util.I18NAlert
import org.pillarone.riskanalytics.core.report.IReportData
import org.pillarone.riskanalytics.core.report.IReportModel
import org.pillarone.riskanalytics.core.report.ReportFactory
import org.pillarone.riskanalytics.core.report.UnsupportedReportParameterException
import org.pillarone.riskanalytics.core.report.impl.ModellingItemReportData
import org.pillarone.riskanalytics.core.report.impl.ReportDataCollection
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.util.Configuration

public class CreateReportAction extends SelectionTreeAction {

    private static Log LOG = LogFactory.getLog(CreateReportAction)
    private static boolean warnExperimentalReports = (Configuration.coreGetAndLogStringConfig("warnExperimentalReports", "false")=="true");


    IReportModel reportModel
    ReportFactory.ReportFormat reportFormat

    CreateReportAction(IReportModel reportModel, ReportFactory.ReportFormat reportFormat, ULCTableTree tree) {
        super("GenerateReport", tree)
        this.reportModel = reportModel
        this.reportFormat = reportFormat
        putValue(IAction.NAME, reportModel.getName() + " " + reportFormat.getRenderedFormatSuchAsPDF())
    }

    // Temporary till we've debugged it ..
    // If it's Parameter Summary Report on A2M model throw up a warning alert: Experimental!
    //
    private void warnParameterSummaryA2Experimental(IReportData reportData){
        // Workaroudn may be to use class name compared to string values?
        //
        // OF COURSE NOW WE HIT THE SNAG THAT APP DOESNT KNOW ABOUT REPORTS PLUGIN...
//        if(reportModel instanceof com.allianz.art.reports.ArtisanParameterSummaryReportModel){
            if(reportData instanceof ModellingItemReportData){
                ModellingItem item = (reportData as ModellingItemReportData).item;
                // OF COURSE NOW WE HIT THE SNAG THAT APP DOESNT KNOW ABOUT MODELS PLUGIN...
//                if( item.modelClass instanceof Artisan2Model ){
                    showInfoAlert(
                        "Experimental Functionality",
                        "Parameter Summary Report on Artisan2Model p14ns is under development.\n"+
                        "Your feedback (to Faz or Paolo please) would be appreciated!",
                        true
                    )
//                }
            }
//        }

    }
    @Override
    void doActionPerformed(ActionEvent event) {
        IReportData reportData = getReportData()
        if(warnExperimentalReports){
            warnParameterSummaryA2Experimental(reportData)
        }
        try {
            LOG.info("Creating ${reportModel.getDefaultReportFileNameWithoutExtension(reportData)}") //throws if >1 items
            byte[] report = ReportFactory.createReport(reportModel, reportData, reportFormat)
            saveReport(report, reportData)
        } catch (UnsupportedReportParameterException e) {
            LOG.warn "Unsupported input to report: ${e}"
            new I18NAlert(UlcUtilities.getWindowAncestor(event.source), "UnsupportedReportInput", [e.getMessage()]).show()
        } catch( Exception e){
            showErrorAlert("Unexpected error", e.getMessage(), true );
            throw e
        }
    }

    /**
     *
     * @return An IReportData. If the list of reporting items is of lenth 1, return the simpler
     * single object, otherwise use the composite pattern and return a list of IReportData's
     *
     */
    public IReportData getReportData() {
        List<ItemNode> selectedItems = getReportingModellingNodes()
        Collection<ModellingItem> modellingItems = selectedItems.collect { itemNode ->
                if (itemNode instanceof IReportableNode) {
                    itemNode.modellingItemsForReport()
                }
        }.flatten()
        Collection<IReportData> reportData = new ArrayList<IReportData>()
        for (ModellingItem aModellingItem in modellingItems) {
            reportData << new ModellingItemReportData(aModellingItem)
        }
//        If there is only one entry this is a special case. Return an individual modelling item report data.
        if (reportData.size() == 1) {
            return reportData.get(0)
        }
        return new ReportDataCollection(reportData)
    }

    private void saveReport(byte[] output, IReportData reportData) {
        FileChooserConfig config = new FileChooserConfig()
        config.dialogTitle = "Save Report As"
        config.dialogType = FileChooserConfig.SAVE_DIALOG
        config.FILES_ONLY
        String fileName = reportModel.getDefaultReportFileNameWithoutExtension(reportData) + "." + reportFormat.getFileExtension()
        fileName = fileName.replace(":", "")
        fileName = fileName.replace("/", "")
        fileName = fileName.replace("*", "")
        fileName = fileName.replace("?", "")
        fileName = fileName.replace("\"", "")
        fileName = fileName.replace("<", "")
        fileName = fileName.replace(">", "")
        fileName = fileName.replace("|", "")
        config.selectedFile = fileName

        ShowDocumentStrategyFactory.getInstance().showDocument(fileName, output, Magic.getMagicMatch(output).getMimeType())
    }


}
