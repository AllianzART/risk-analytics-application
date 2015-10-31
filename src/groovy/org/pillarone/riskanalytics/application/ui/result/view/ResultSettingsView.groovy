package org.pillarone.riskanalytics.application.ui.result.view
import com.ulcjava.base.application.*
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.IActionListener
import com.ulcjava.base.application.util.Dimension
import com.ulcjava.base.application.util.IFileChooseHandler
import com.ulcjava.base.application.util.IFileStoreHandler
import com.ulcjava.base.shared.FileChooserConfig
import grails.util.Holders
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.pillarone.riskanalytics.application.dataaccess.item.ModellingItemFactory
import org.pillarone.riskanalytics.application.ui.base.action.ResourceBasedAction
import org.pillarone.riskanalytics.application.ui.main.eventbus.event.OpenDetailViewEvent
import org.pillarone.riskanalytics.application.ui.main.eventbus.RiskAnalyticsEventBus
import org.pillarone.riskanalytics.application.ui.main.view.item.UIItemFactory
import org.pillarone.riskanalytics.application.ui.util.DateFormatUtils
import org.pillarone.riskanalytics.application.util.LocaleResources
import org.pillarone.riskanalytics.core.ModelDAO
import org.pillarone.riskanalytics.core.components.ComponentUtils
import org.pillarone.riskanalytics.core.model.DeterministicModel
import org.pillarone.riskanalytics.core.simulation.item.ModelItem
import org.pillarone.riskanalytics.core.simulation.item.ModelStructure
import org.pillarone.riskanalytics.core.simulation.item.ModellingItem
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.pillarone.riskanalytics.core.simulation.item.parameter.ParameterHolder
import org.pillarone.riskanalytics.core.util.IConfigObjectWriter

class ResultSettingsView {

    Simulation simulation
    ULCBoxPane content

    public ResultSettingsView(Simulation simulation) {
        this.simulation = simulation;
        initComponents()
    }

    private RiskAnalyticsEventBus getRiskAnalyticsEventBus() {
        Holders.grailsApplication.mainContext.getBean('riskAnalyticsEventBus', RiskAnalyticsEventBus)
    }

    private void initComponents() {

        ULCBoxPane settings = boxLayout(getText('settings')) { ULCBoxPane box ->

            ULCBoxPane content = new ULCBoxPane(3, 0)
            addLabels(content, getText('name') + ":", "$simulation.name (id: ${simulation.id ?: 'N/A'})" ) //AR-168 show sim id
            addLabels(content, getText('creationDate') + ":", DateFormatUtils.formatDetailed(simulation.start) )
            addLabels(content, getText('modelLabel') + ":", "$simulation.modelClass.simpleName v${simulation.modelVersionNumber.toString()}", new ULCButton(new ExportModelItemAction(simulation)))
            addLabels(content, getText('structure') + ":", "$simulation.structure.name v${simulation.structure.versionNumber.toString()}", new ULCButton(new ExportStructureAction(simulation.structure)))
            ULCButton openParams = new ULCButton(getText('open'))
            openParams.addActionListener([actionPerformed: { event -> openItem(simulation.parameterization) }] as IActionListener)
            String paramText = getText("parameterization") + ":"
            addLabels(content, paramText, "$simulation.parameterization.name v${simulation.parameterization.versionNumber.toString()}", openParams)
            ULCButton openTemplate = new ULCButton(getText('open'))
            openTemplate.addActionListener([actionPerformed: { event -> openItem(simulation.template) }] as IActionListener)
            addLabels(content, getText('template') + ":", "$simulation.template.name v${simulation.template.versionNumber.toString()}", openTemplate)
            if (!DeterministicModel.isAssignableFrom(simulation.modelClass)) {
                addLabels(content, getText('randomSeed') + ":", "$simulation.randomSeed")
            } else {
                addLabels(content, getText('firstPeriod') + ":", DateTimeFormat.forPattern('dd.MM.yyyy').print(simulation.beginOfFirstPeriod) )
            }
            addLabels(content, getText('periods') + ":", simulation.periodCount.toString() )
            addLabels(content, getText('modelVersion') + ":", simulation.modelVersionNumber.toString() )
            int simulationDuration = (simulation.end.getMillis() - simulation.start.getMillis()) / 1000
            addLabels(content, getText('completedIterations') + ":", "${simulation.numberOfIterations.toString()} in ${simulationDuration} secs" )

            box.add(content)
        }

        ULCBoxPane runtimeParameters = boxLayout("Runtime parameters") { ULCBoxPane box ->
            ULCBoxPane content = new ULCBoxPane(2, 0)
            def sorted = simulation.notDeletedParameterHolders.sort { ComponentUtils.getNormalizedName(it.path) }
            for (ParameterHolder parameter in sorted) {
                content.add(ULCBoxPane.BOX_LEFT_CENTER, new ULCLabel(ComponentUtils.getNormalizedName(parameter.path)))
                content.add(ULCBoxPane.BOX_LEFT_CENTER, new ULCLabel(formatParameter(parameter.businessObject)))
            }

            box.add(content)
        }

        boolean hasRuntimeParameters = !simulation.runtimeParameters.empty

        ULCBoxPane holder = new ULCBoxPane(hasRuntimeParameters ? 3 : 2, 1)
        holder.add(ULCBoxPane.BOX_EXPAND_TOP, settings)
        holder.add(ULCBoxPane.BOX_EXPAND_TOP, new ULCFiller())

        ULCBoxPane box = new ULCBoxPane()
        box.add(ULCBoxPane.BOX_EXPAND_TOP, holder)
        if (hasRuntimeParameters) {
            box.add(ULCBoxPane.BOX_EXPAND_TOP, runtimeParameters)
        }
        box.add(ULCBoxPane.BOX_EXPAND_EXPAND, new ULCFiller())


        // AR-168 See https://ulc.canoo.com/developerzone/doc/ULCCore/8.0.2.2/apidoc/index.html
        //
        ULCScrollPane scrollPane = new ULCScrollPane(box)
        scrollPane.horizontalScrollBar.blockIncrement = 180            // page-up/down size
        scrollPane.verticalScrollBar.blockIncrement = 180              // page-up/down size

        content = new ULCBoxPane()
        content.add(ULCBoxPane.BOX_EXPAND_EXPAND, scrollPane)
    }

    private String formatParameter(def parameter) {
        return parameter.toString()
    }

    private String formatParameter(DateTime parameter) {
        return DateTimeFormat.forPattern("yyyy-MM-dd").print(parameter)
    }

    private void openItem(ModellingItem item) {
        riskAnalyticsEventBus.post(new OpenDetailViewEvent(UIItemFactory.createItem(item)))
    }

    private void addLabels(ULCBoxPane container, String key, String value ) {
        def keyLabel = new ULCLabel(key)
        container.add(ULCBoxPane.BOX_LEFT_CENTER, spaceAround(keyLabel, 5, 10, 0, 0))
        def valueLabel = new ULCLabel(value)
        container.add(2, ULCBoxPane.BOX_LEFT_CENTER, spaceAround(valueLabel, 5, 10, 0, 0)) //Spans 2 cells
    }

    private void addLabels(ULCBoxPane container, String key, String value, ULCComponent thirdComponent ) {
        def keyLabel = new ULCLabel(key)
        container.add(ULCBoxPane.BOX_LEFT_CENTER, spaceAround(keyLabel, 5, 10, 0, 0))
        def valueLabel = new ULCLabel(value)
        container.add(ULCBoxPane.BOX_LEFT_CENTER, spaceAround(valueLabel, 5, 10, 0, 0))
        thirdComponent.preferredSize = new Dimension(100, 25)
        container.add(ULCBoxPane.BOX_LEFT_TOP, spaceAround(thirdComponent, 5, 10, 0, 0))
    }

    private ULCBoxPane spaceAround(ULCComponent comp, int top, int left, int bottom, int right) {
        ULCBoxPane deco = new ULCBoxPane()
        deco.border = BorderFactory.createEmptyBorder(top, left, bottom, right)
        deco.add comp
        return deco
    }

    private ULCBoxPane boxLayout(String title, Closure body) {
        ULCBoxPane result = new ULCBoxPane()
        result.border = BorderFactory.createTitledBorder(" $title ")
        ULCBoxPane inner = new ULCBoxPane()
        body(inner)
        result.add spaceAround(inner, 0, 5, 5, 5)
        result.add ULCBoxPane.BOX_EXPAND_CENTER, new ULCFiller()
        return result
    }

    /**
     * Utility method to get resource bundle entries for this class
     *
     * @param key
     * @return the localized value corresponding to the key
     */
    protected String getText(String key) {
        return LocaleResources.getString("ResultSettingsView." + key);
    }
}

class ExportModelItemAction extends ResourceBasedAction {
    Simulation simulation
    private ModelItem modelItemI

    public ExportModelItemAction(Simulation simulation) {
        super("ExportModelItem")
        this.@simulation = simulation
    }

    public void doActionPerformed(ActionEvent event) {
        if (!modelItem.loaded) {
            modelItem.load()
        }

        FileChooserConfig config = new FileChooserConfig()
        config.dialogTitle = "Export for ${modelItem.name}"
        config.dialogType = FileChooserConfig.SAVE_DIALOG
        config.FILES_ONLY
        config.selectedFile = "${modelItem.name}_v${simulation.modelVersionNumber.toString()}.groovy"

        ULCWindow ancestor = UlcUtilities.getWindowAncestor(event.source)
        ClientContext.chooseFile([
                onSuccess: { filePaths, fileNames ->
                    String selectedFile = filePaths[0]
                    trace("export model to $selectedFile")
                    ClientContext.storeFile([prepareFile: { OutputStream stream ->
                        BufferedWriter bw
                        try {
                            bw = new BufferedWriter(new OutputStreamWriter(stream))
                            bw.write modelItem.srcCode
                        } catch (Throwable t) {
                            new ULCAlert(ancestor, "Export failed", t.message, "Ok").show()
                        } finally {
                            bw.close()
                        }
                    }, onSuccess                        : { path, name ->
                    }, onFailure                        : { reason, description ->
                        new ULCAlert(ancestor, "Export failed", description, "Ok").show()
                    }] as IFileStoreHandler, selectedFile)

                },
                onFailure: { reason, description ->
                }] as IFileChooseHandler, config, ancestor)
    }

    public ModelItem getModelItem() {
        if (!modelItemI) {
            modelItemI = ModellingItemFactory.getModelItem(ModelDAO.findByNameAndItemVersion(simulation.modelClass.simpleName, simulation.modelVersionNumber.toString()))
        }
        modelItemI
    }
}

class ExportStructureAction extends ResourceBasedAction {
    ModelStructure item

    public ExportStructureAction(ModelStructure structure) {
        super("ExportStructure")
        this.@item = structure
    }

    public void doActionPerformed(ActionEvent event) {
        if (!item.loaded) {
            item.load()
        }
        IConfigObjectWriter writer = item.getWriter()


        FileChooserConfig config = new FileChooserConfig()
        config.dialogTitle = "Export for ${item.name}"
        config.dialogType = FileChooserConfig.SAVE_DIALOG
        config.FILES_ONLY
        config.selectedFile = "${item.name}.groovy"

        ULCWindow ancestor = UlcUtilities.getWindowAncestor(event.source)
        ClientContext.chooseFile([
                onSuccess: { filePaths, fileNames ->
                    String selectedFile = filePaths[0]
                    ClientContext.storeFile([prepareFile: { OutputStream stream ->
                        try {
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(stream))
                            writer.write(item.data, bw)
                        } catch (Throwable t) {
                            new ULCAlert(ancestor, "Export failed", t.message, "Ok").show()
                        } finally {
                            stream.close()
                        }
                    }, onSuccess                        : { path, name ->
                    }, onFailure                        : { reason, description ->
                        new ULCAlert(ancestor, "Export failed", description, "Ok").show()
                    }] as IFileStoreHandler, selectedFile)

                },
                onFailure: { reason, description ->
                }] as IFileChooseHandler, config, ancestor)
    }


}