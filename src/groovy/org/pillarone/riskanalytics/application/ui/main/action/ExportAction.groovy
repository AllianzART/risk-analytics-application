package org.pillarone.riskanalytics.application.ui.main.action
import com.ulcjava.base.application.*
import com.ulcjava.base.application.util.IFileChooseHandler
import com.ulcjava.base.application.util.IFileStoreHandler
import com.ulcjava.base.shared.FileChooserConfig

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.poi.ss.usermodel.Sheet

import org.pillarone.riskanalytics.application.dataaccess.item.ModellingItemFactory
import org.pillarone.riskanalytics.application.ui.util.DateFormatUtils
import org.pillarone.riskanalytics.application.ui.util.ExcelExporter
import org.pillarone.riskanalytics.application.ui.util.I18NAlert
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.application.util.prefs.UserPreferences
import org.pillarone.riskanalytics.application.util.prefs.UserPreferencesFactory

import org.pillarone.riskanalytics.core.ParameterizationDAO
import org.pillarone.riskanalytics.core.dataaccess.ResultAccessor
import org.pillarone.riskanalytics.core.dataaccess.ResultPathDescriptor
import org.pillarone.riskanalytics.core.output.ResultConfigurationDAO
import org.pillarone.riskanalytics.core.output.SingleValueResultPOJO
import org.pillarone.riskanalytics.core.output.SimulationRun
import org.pillarone.riskanalytics.core.output.SingleValueResult
import org.pillarone.riskanalytics.core.simulation.item.*
import org.pillarone.riskanalytics.core.util.Configuration
import org.pillarone.riskanalytics.core.util.IConfigObjectWriter
import org.springframework.transaction.TransactionStatus

import java.util.regex.Pattern
/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
abstract class ExportAction extends SelectionTreeAction {
    protected static final int ONE_MEG = 1024 * 1024
    protected static final long EXPORT_TRANSFER_MAX_BYTES = Configuration.coreGetAndLogIntConfig("EXPORT_TRANSFER_MAX_BYTES", 200 * ONE_MEG)

    UserPreferences userPreferences
    protected String fileExtension = 'xlsx'
    Log LOG = LogFactory.getLog(ExportAction)

    public ExportAction(ULCTableTree tree, String title, String fileExtension = 'xlsx') {
        super(title, tree)
        userPreferences = UserPreferencesFactory.getUserPreferences()
        this.fileExtension = fileExtension
    }

    public ExportAction(String title) {
        super(title)
        userPreferences = UserPreferencesFactory.getUserPreferences()
    }

    protected void exportAll(List<ModellingItem> items) {
        if (!items || items.isEmpty()) return
        switch (items[0].class) {
            case Simulation.class: exportSimulations(items); break;
            default: exportItems(items); break;
        }
    }

    protected void exportSimulations(List<ModellingItem> items) {
        ULCWindow ancestor = getAncestor()
        if (!validate(items)) {
            ULCAlert alert = new I18NAlert(UlcUtilities.getWindowAncestor(tree), "resultExcelExportError")
            alert.show()
        } else {
            FileChooserConfig config = new FileChooserConfig()
            config.dialogTitle = "Excel Export"
            if( this.class.simpleName.equals("CsvExportAction") ){
                config.dialogTitle =  "CSV Export"
            }
            config.dialogType = FileChooserConfig.SAVE_DIALOG
            config.fileSelectionMode = items.size() > 1 ? FileChooserConfig.DIRECTORIES_ONLY : FileChooserConfig.FILES_ONLY
            config.setCurrentDirectory(userPreferences.getUserDirectory(UserPreferences.EXPORT_DIR_KEY))
            if (items.size() == 1) {
                config.selectedFile = getFileName(items[0])
            }


            ClientContext.chooseFile([
                    onSuccess: { filePaths, fileNames ->
                        userPreferences.setUserDirectory(UserPreferences.EXPORT_DIR_KEY, filePaths[0])
                        items.each { ModellingItem item ->
                            item.load()
                            exportItem(item, items.size(), filePaths, ancestor)
                        }
                    },
                    onFailure: { reason, description ->
                        if (reason != IFileChooseHandler.CANCELLED) {
                            LOG.error description
                            showAlert("exportError")
                        }
                    }] as IFileChooseHandler, config, ancestor)
        }

    }

    protected String getFileName(def item) {
        validateFileName("${getName(item)}.$fileExtension".replaceAll(':', '-'))
    }


    protected void exportItem(Simulation item, int itemCount, filePaths, ULCWindow ancestor) { // Export (not CSV Export)
        ExcelExporter exporter = new ExcelExporter()
        SingleValueResult.withTransaction { trx ->
            String selectedFile = itemCount > 1 ? "${filePaths[0]}/${getFileName(item)}" : filePaths[0]
            selectedFile = selectedFile.endsWith(".$fileExtension") ? selectedFile : "${selectedFile}.$fileExtension"

            item.load()
            SimulationRun simulationRun = item.simulationRun
//            List rawData = ResultAccessor.getAllResults( simulationRun ) //Got OutOfMemoryError: GC overhead limit exceeded
            try {
                // PMO-2822 Instead of acquiring the big list first then trying to stream to client..
                // Pass output stream into the ResultAccessor and feed the exporter's exportResults()
                // in loop, quitting early if file is too large ?
                // :( Nope, because passing in the ExcelExporter would make core depend on app foo bar yuk yuk..
                //
                ClientContext.storeFile([prepareFile: { OutputStream stream ->
                    // Okay, pull the guts of the getAllResults here, and do the work in ra-app..
                    //
                    LOG.info("Building Excel from Result paths before streaming to client")
                    Sheet sheet = exporter.createExcelSheet()

                    List<ResultPathDescriptor> paths = ResultAccessor.getDistinctPaths(simulationRun)
                    int sheetDataRowIndex = 0;
                    for (ResultPathDescriptor descriptor in paths) {
                        double[] values = ResultAccessor.getValues(simulationRun, descriptor.period, descriptor.path.pathName, descriptor.collector.collectorName, descriptor.field.fieldName)
                        for (int i=0; i < values.length; i ++){ // Dumb to repeatedly call size() on a java array!
                            if( sheetDataRowIndex < ONE_MEG - 1 ){ // nb header row
                                SingleValueResultPOJO pojo = new org.pillarone.riskanalytics.core.output.SingleValueResultPOJO (
                                        path: descriptor.path,
                                        field: descriptor.field,
                                        collector: descriptor.collector,
                                        period: descriptor.period,
                                        simulationRun: simulationRun,
                                        value: values[i],
                                        iteration:i
                                )
                                exporter.writeOneRowToSheet( sheet, pojo, sheetDataRowIndex++ )
                            } else {
                                String msg = "Sadly generated file exceeds number of rows Excel can open ($ONE_MEG)"
                                showWarnAlert("File too big", msg, true)
                                throw new IllegalArgumentException(msg)
                            }
                        }
                    }
                    exporter.addTab("Simulation settings", getSimulationSettings(simulationRun))
                    LOG.info("Built Excel object without crashing; now stream it to client")
                    exporter.writeWorkBook stream
                }, onSuccess                        : { path, name -> showInfoAlert("Successfully exported Excel file", "Filename: $name saved in folder:\n $path", true)
                }, onFailure                        : { reason, description ->
                    if (reason == IFileStoreHandler.FAILED) {
                        LOG.error description
                        showAlert("exportError")
                    }
                }] as IFileStoreHandler, selectedFile, EXPORT_TRANSFER_MAX_BYTES, false)
            } catch (IllegalArgumentException iae) {
                LOG.error("Export failed: " + iae.message, iae)
                showAlert("tooManyRowsError")
            } catch (IllegalStateException t) {
                if(t.message.startsWith(ResultAccessor.MISSING_DIR_PREFIX)){
                    showWarnAlert("Cant find iterations folder", t.message )
                } else {
                    LOG.error("Export failed: " + t.message, t)
                    showAlert("exportError")
                }
            } catch (Throwable t) {
                LOG.error("Export failed: " + t.message, t)
                showAlert("exportError")
            }
        }
    }

    protected void exportItem(ModellingItem item, int itemCount, filePaths, ULCWindow ancestor) {
        item.load()
        IConfigObjectWriter writer = item.getWriter()
        String selectedFile = getFileName(itemCount, filePaths, item)
        LOG.info " selectedFile : $selectedFile "
        try {
            ClientContext.storeFile([prepareFile: { OutputStream stream ->
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(stream))
                writer.write(getConfigObject(item), bw)
            }, onSuccess                        : { path, name ->
                LOG.info " $selectedFile exported"
            }, onFailure                        : { reason, description ->
                if (reason == IFileStoreHandler.FAILED) {
                    LOG.error description
                    showAlert("exportError")
                }
            }] as IFileStoreHandler, selectedFile, Long.MAX_VALUE, false)
        } catch (Throwable t) {
            LOG.error("Export failed: " + t.message, t)
            showAlert("exportError")
        }
    }


    ULCWindow getAncestor() {
        ULCWindow ancestor = UlcUtilities.getWindowAncestor(tree)
        return ancestor
    }

    protected List<List<String>> getSimulationSettings(SimulationRun simulationRun, String delimiter = null) {
        String sep = ":"
        if(delimiter != null){
            sep += delimiter
        }
        SimulationRun.withTransaction { status ->
            simulationRun = SimulationRun.get(simulationRun.id)
            Parameterization parameterization = ModellingItemFactory.getParameterization(simulationRun?.parameterization)
            Class modelClass = parameterization.modelClass
            Simulation simulation = ModellingItemFactory.getSimulation(simulationRun)
            simulation.load()

            List data = []
            data << ["Setting name", "Setting value", "Version"]
            data << [UIUtils.getText(ExportAction.class, "SimulationName"     ) + sep, "$simulation.name"]
            data << [UIUtils.getText(ExportAction.class, "Comment"            ) + sep, simulation.comment]
            data << [UIUtils.getText(ExportAction.class, "Model"              ) + sep, "${simulation.modelClass.name}", "${simulation.modelVersionNumber}"]
            data << [UIUtils.getText(ExportAction.class, "Parameterization"   ) + sep, "$simulation.parameterization.name", "${simulation.parameterization.versionNumber.toString()}"]
            data << [UIUtils.getText(ExportAction.class, "Template"           ) + sep, "$simulation.template.name", "${simulation.template.versionNumber.toString()}"]
            data << [UIUtils.getText(ExportAction.class, "Structure"          ) + sep, "$simulation.structure.name", "${simulation.structure.versionNumber.toString()}"]
            data << [UIUtils.getText(ExportAction.class, "NumberOfPeriods"    ) + sep, simulation.periodCount]
            data << [UIUtils.getText(ExportAction.class, "NumberOfIterations" ) + sep, simulation.numberOfIterations]
            data << [UIUtils.getText(ExportAction.class, "RandomSeed"         ) + sep, simulation.randomSeed]
            data << [UIUtils.getText(ExportAction.class, "SimulationEndDate"  ) + sep, DateFormatUtils.formatDetailed(simulation.end)]
            return data
        }
    }

    protected def exportItems(List<ModellingItem> items) {
        int itemCount = items.size()
        FileChooserConfig config = new FileChooserConfig()
        config.dialogTitle = "Export"
        config.dialogType = FileChooserConfig.SAVE_DIALOG
        config.fileSelectionMode = itemCount > 1 ? FileChooserConfig.DIRECTORIES_ONLY : FileChooserConfig.FILES_ONLY
        config.setCurrentDirectory(userPreferences.getUserDirectory(UserPreferences.EXPORT_DIR_KEY))
        if (items.size() == 1) {
            config.selectedFile = "${getName(items[0])}.groovy"
        }

        ULCWindow ancestor = getAncestor()
        ClientContext.chooseFile([
                onSuccess: { filePaths, fileNames ->
                    userPreferences.setUserDirectory(UserPreferences.EXPORT_DIR_KEY, getFolderName(itemCount, filePaths))
                    items.each { def item ->
                        exportItem(item, itemCount, filePaths, ancestor)
                    }
                },
                onFailure: { reason, description ->
                    if (reason != IFileChooseHandler.CANCELLED) {
                        LOG.error description
                        showAlert("exportError")
                    }

                }] as IFileChooseHandler, config, ancestor)
    }


    protected def showAlert(String errorName) {
        ULCAlert alert = new I18NAlert(ancestor, errorName)
        alert.show()
    }

    String getFileName(int itemCount, filePaths, ModellingItem item) {
        String selectedFile = itemCount > 1 ? "${filePaths[0]}${getFileSeparator()}${getName(item)}.groovy" : filePaths[0]
        return validateFileName(selectedFile)
    }

    protected String getName(def item) {
        return item.name.replaceAll(escapeIfNeeded(getFileSeparator()), '_')
    }

    private String escapeIfNeeded(String string) {
        '\\'.equals(string) ? "\\$string" : string
    }

    String getFolderName(int itemCount, filePaths) {
        String selectedFile = itemCount > 1 ? "${filePaths[0]}" : filePaths[0].substring(0, filePaths[0].lastIndexOf(getFileSeparator()))
        return validateFileName(selectedFile)
    }

    private ConfigObject getConfigObject(Parameterization parameterization) {
        ConfigObject result
        ParameterizationDAO.withTransaction { TransactionStatus status ->
            if (!parameterization.isLoaded())
                parameterization.load()
            parameterization.orderByPath = true
            result = parameterization.toConfigObject()
        }
        return result
    }

    private ConfigObject getConfigObject(ConfigObjectBasedModellingItem modellingItem) {
        modellingItem.data
    }

    private ConfigObject getConfigObject(ResultConfiguration resultConfiguration) {
        ConfigObject result
        ResultConfigurationDAO.withTransaction { TransactionStatus status ->
            result = resultConfiguration.toConfigObject()
        }
        return result
    }

    protected boolean validate(List<ModellingItem> items) {
        boolean status = true
        items.each { ModellingItem item ->
            if (item instanceof Simulation) {
                Simulation simulation = item
                SingleValueResult.withTransaction { trx ->
                    simulation.load()
                    SimulationRun simulationRun = simulation.simulationRun
                    int count = SingleValueResult.countBySimulationRun(simulationRun)
                    if (count > 50000) {
                        status = false
                    }
                }
            }
        }
        return status
    }

    // TODO drop the defs and test it works with static typing
    // Probably rewrite logic to be readable too...
    //
    static String validateFileName(String filename) {
        String separator = getFileSeparator()
        def arr = filename.split(Pattern.quote(separator))
        def pattern = ~/[^\w.]/
        arr[arr.size() - 1] = pattern.matcher(arr[arr.size() - 1]).replaceAll("")

        filename = ""
        arr.eachWithIndex { String p, int index ->
            filename += p
            if (index != arr.size() - 1)
                filename += separator
        }
        return filename
    }

    static String getFileSeparator() {
        return ClientContext.getSystemProperty("file.separator")
    }
}
