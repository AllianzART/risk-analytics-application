package org.pillarone.riskanalytics.application.ui.chart.action

import com.ulcjava.base.application.ClientContext
import com.ulcjava.base.application.ULCAlert
import com.ulcjava.base.application.ULCWindow
import com.ulcjava.base.application.UlcUtilities
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.util.serializable.IFileChooseHandler
import com.ulcjava.base.application.util.serializable.IFileStoreHandler
import com.ulcjava.base.shared.FileChooserConfig
import org.pillarone.riskanalytics.application.ui.base.action.ResourceBasedAction
import org.pillarone.riskanalytics.application.ui.chart.view.ChartView
import org.pillarone.riskanalytics.application.ui.util.ExcelExporter
import org.pillarone.riskanalytics.application.util.LocaleResources

class ChartDataExportAction extends ResourceBasedAction {
    ChartView view


    public ChartDataExportAction(ChartView view) {
        super("ChartDataExportAction")
        this.view = view
    }

    public void doActionPerformed(ActionEvent event) {
        ExcelExporter exporter = new ExcelExporter()
        FileChooserConfig config = new FileChooserConfig()
        config.dialogTitle = getText("dialogTitle")
        config.dialogType = FileChooserConfig.SAVE_DIALOG
        config.FILES_ONLY
        config.selectedFile = "${view.model.title}.xls"

        ULCWindow ancestor = UlcUtilities.getWindowAncestor(view.content)
        ClientContext.chooseFile([
                onSuccess: {filePaths, fileNames ->
                    String selectedFile = filePaths[0]

                    ClientContext.storeFile([prepareFile: {OutputStream stream ->
                        try {
                            exporter.export view.model.dataTable
                            exporter.addTab "Simulation Settngs", view.model.simulationSettings
                            exporter.writeWorkBook stream
                        } catch (UnsupportedOperationException t) {
                            new ULCAlert(ancestor, "Export failed", t.message, "Ok").show()
                        } catch (Throwable t) {
                            new ULCAlert(ancestor, "Export failed", t.message, "Ok").show()
                            throw t
                        } finally {
                            stream.close()
                        }
                    }, onSuccess: {path, name ->
                    }, onFailure: {reason, description ->
                        new ULCAlert(ancestor, "Export failed", description, "Ok").show()
                    }] as IFileStoreHandler, selectedFile)

                },
                onFailure: {reason, description ->
                }] as IFileChooseHandler, config, ancestor)

    }

    /**
     * Utility method to get resource bundle entries for this class
     *
     * @param key
     * @return the localized value corresponding to the key
     */
    protected String getText(String key) {
        return LocaleResources.getString("ChartDataExportAction." + key);
    }

}