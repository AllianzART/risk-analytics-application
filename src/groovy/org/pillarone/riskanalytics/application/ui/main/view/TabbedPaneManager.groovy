package org.pillarone.riskanalytics.application.ui.main.view

import com.canoo.ulc.detachabletabbedpane.server.ULCDetachableTabbedPane
import com.ulcjava.base.application.ULCAlert
import com.ulcjava.base.application.ULCComponent
import com.ulcjava.base.application.ULCContainer
import com.ulcjava.base.application.UlcUtilities
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.IWindowListener
import com.ulcjava.base.application.event.WindowEvent
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.main.action.SaveAction
import org.pillarone.riskanalytics.application.ui.main.view.item.*
import org.pillarone.riskanalytics.application.ui.util.I18NAlert
import org.pillarone.riskanalytics.application.ui.view.viewlock.ViewLockService
import org.pillarone.riskanalytics.core.user.Person
import org.pillarone.riskanalytics.core.user.UserManagement
import org.pillarone.riskanalytics.core.util.Configuration

import static org.pillarone.riskanalytics.core.util.Configuration.*

class TabbedPaneManager {
    private static Log LOG = LogFactory.getLog(TabbedPaneManager)
    private static boolean disableAR228Fix = Configuration.coreGetAndLogStringConfig("disableAR228Fix", "false")=="true"

    private ULCDetachableTabbedPane tabbedPane
    private DependentFramesManager dependentFramesManager
    private Map<AbstractUIItem, ULCComponent> tabManager = [:]

    //keep a map of open items to avoid to compare titles to find the tabs

    TabbedPaneManager(ULCDetachableTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane
        this.dependentFramesManager = new DependentFramesManager(this.tabbedPane)
    }

    private DetailViewManager getDetailViewManager() {
        Holders.grailsApplication.mainContext.getBean('detailViewManager', DetailViewManager)
    }

    private RiskAnalyticsMainView getRiskAnalyticsMainView() {
        Holders.grailsApplication.mainContext.getBean('riskAnalyticsMainView', RiskAnalyticsMainView)
    }

    private ViewLockService getViewLockService() {
        Holders.grailsApplication.mainContext.getBean('viewLockService', ViewLockService)
    }

    /**
     * Creates a new tab for the given item - general case
     * Use ViewLockService to detect edit collisions if user available (production env).
     * SimulationSettings pane and P14n handled separately.
     * @item AbstractUIItem for which to add a tab
     */
    void addTab(AbstractUIItem item) {
        Person currentUser = getCurrentUser()
        try{
            if(currentUser != null && item instanceof ModellingUIItem ) {
                handleEditCollisionWarningIfNeededBeforeOpening(item, currentUser)
            } else {
                // test env (user null) or not modelling item
                addTabInternal(item)
            }
        }catch(Exception e){
            String w = "Error opening '${item.nameAndVersion}': ${e.message}.\nPlease report this to IT Apps."
            LOG.error(w,e)
            new ULCAlert("Failed to open tab for item", w, "OK").show()
            if(currentUser){
                viewLockService.release(item, currentUser.getUsername())
            }
            throw e
        }
    }
    /**
     * Creates a new tab for given simulation settings pane.
     * Does not use ViewLockService's edit collision detection.
     */
    void addTab(SimulationSettingsUIItem item) {
        addTabInternal(item)
    }

    /**
     * Creates a new tab for given sim result.
     * Does not use ViewLockService's edit collision detection.
     */
    void addTab(SimulationResultUIItem item) {
        addTabInternal(item)
    }

    /**
     * Creates a new tab for the given P14n
     * If P14n is opening for editing then use ViewLockService's edit collision detection.
     * See OpenItemAction.openItem(ParameterizationUIItem) and OpenItemDialog for clues.
     * @item ParameterizationUIItem for which to add a tab
     */
    void addTab(ParameterizationUIItem item) {
        Person currentUser = getCurrentUser()
        try{
            if( currentUser == null ){
                // test environment
                addTabInternal(item)
                return
            }
            if( isReadOnlyP14n(item)    )
            {
                addTabInternal(item)
            } else {
                handleEditCollisionWarningIfNeededBeforeOpening(item, currentUser)
            }
        }catch(IllegalStateException e){
            if(disableAR228Fix){
                throw e
            }
            String w = "Error opening '${item.nameAndVersion}': ${e.message}.\nPlease inform IT Apps."
            LOG.error(w,e)
            new ULCAlert("Failed to open P14n", w, "OK").show()
            if(currentUser){
                viewLockService.release(item, currentUser.getUsername())
            }
        }catch(Exception e){
            String w = "Error opening '${item.nameAndVersion}': ${e.message}.\nPlease screenshot this for IT Apps."
            LOG.error(w,e)
            new ULCAlert("Failed to open P14n", w, "OK").show()
            if(currentUser){
                viewLockService.release(item, currentUser.getUsername())
            }
            throw e
        }

    }

    private boolean  isReadOnlyP14n(ParameterizationUIItem parameterizationUIItem){

        // TODO Ask Hannes: OpenItemAction.openItem() uses synchronized block and a DB transaction around similar logic
        synchronized (parameterizationUIItem){
            boolean usedInSimulation = parameterizationUIItem.isUsedInSimulation()
            if(usedInSimulation){
                return true
            }
            boolean editableState = parameterizationUIItem.newVersionAllowed() //NONE or DATA_ENTRY states can be edited
            if(!editableState){
                return true
            }
        }
        return false
    }

    // Do not call this with a null user
    //
    private void handleEditCollisionWarningIfNeededBeforeOpening(ModellingUIItem item, Person currentUser) {
        Set<String> alreadyEditingUsers = viewLockService.lock(item, currentUser.getUsername())
        if(alreadyEditingUsers.size() > 0) {
            final String alreadyCsv = alreadyEditingUsers.join(", ")
            final String warning = "Opening ${item.nameAndVersion} DESPITE already opened by ${alreadyCsv}"
            final String cancel = "Canceled opening ${item.nameAndVersion} - already opened by ${alreadyCsv}"
            I18NAlert alert = new I18NAlert(null, "ViewLockedAlert", [item.nameAndVersion, alreadyCsv])
            alert.addWindowListener([windowClosing: { WindowEvent windowEvent ->
                def value = windowEvent.source.value
                if (value.equals(alert.firstButtonLabel)) {
                    LOG.warn(warning)
                    addTabInternal(item)
                }
                if (value.equals(alert.secondButtonLabel)) {
                    LOG.info(cancel)
                    viewLockService.release(item, currentUser.getUsername())
                }
            }] as IWindowListener)
            alert.show()
        } else {
            addTabInternal(item)
        }
    }

    private void addTabInternal(AbstractUIItem item) {
        IDetailView detailView = detailViewManager.createDetailViewForItem(item)
        if (item instanceof ModellingUIItem) {
            item.addModellingItemChangeListener(riskAnalyticsMainView)
            item.addModellingItemChangeListener new MarkItemAsUnsavedListener(this, item)
        }
        ULCContainer view = detailView.content
        tabManager[item] = view
        tabbedPane.addTab(item.createTitle(), item.icon, view)
        int tabIndex = tabbedPane.tabCount - 1
        tabbedPane.selectedIndex = tabIndex
        tabbedPane.setToolTipTextAt(tabIndex, item.toolTip)
    }

    protected Person getCurrentUser() {
        UserManagement.getCurrentUser()
    }

    void closeTab(ULCComponent component) {
        AbstractUIItem item = getAbstractItem(component)
        if (item) {
            closeTabForItem(item)
            Person currentUser = getCurrentUser()
            if(currentUser != null && item instanceof ModellingUIItem) {
                viewLockService.release(item, currentUser.getUsername())
            }
        } else {
            log.warning("item null for component " + component)
        }
    }

    private void closeTabForItem(AbstractUIItem abstractUIItem) {
        if (abstractUIItem instanceof ModellingUIItem && abstractUIItem.item.changed) {
            ModellingUIItem modellingUIItem = abstractUIItem
            boolean closeTab = true
            ULCAlert alert = new I18NAlert(UlcUtilities.getWindowAncestor(tabManager[modellingUIItem]), "itemChanged")
            alert.addWindowListener([windowClosing: { WindowEvent windowEvent ->
                def value = windowEvent.source.value
                if (value.equals(alert.firstButtonLabel)) {
                    SaveAction saveAction = new SaveAction(tabbedPane, modellingUIItem.item)
                    saveAction.doActionPerformed(new ActionEvent(this, "save"))
                } else if (value.equals(alert.thirdButtonLabel)) {
                    closeTab = false

                } else {
                    modellingUIItem.unload()
                }
                if (closeTab) {
                    removeTab(modellingUIItem)
                } else {
                    selectTab(modellingUIItem)
                }
            }] as IWindowListener)
            alert.show()
        } else {
            removeTab(abstractUIItem)
        }
    }

    /**
     * Opens and selects the card for the given model
     * @param model the model to open
     */
    void selectTab(AbstractUIItem item) {
        ULCComponent component = tabManager[item]
        if (tabbedPane.indexOfComponent(component) >= 0) {
            tabbedPane.selectedComponent = component
        } else {
            dependentFramesManager.selectTab(item)
        }
    }

    /**
     * Removes a tab from the Tabbed Pane for the given item
     * @param item
     */
    void removeTab(AbstractUIItem item) {
        ULCComponent component = tabManager[item]
        if (component) {
            if (tabbedPane.indexOfComponent(component) >= 0) {
                tabbedPane.remove(component)
            } else {
                dependentFramesManager.closeTab(item)
            }
            tabManager.remove(item)
        }
        detailViewManager.close(item)
    }

    boolean tabExists(AbstractUIItem abstractUIItem) {
        return tabManager.containsKey(abstractUIItem)
    }

    AbstractUIItem getAbstractItem(ULCComponent component) {
        AbstractUIItem abstractUIItem = null
        tabManager.each { k, v ->
            if (v == component)
                abstractUIItem = k
        }
        return abstractUIItem
    }

    void updateTabbedPaneTitle(AbstractUIItem abstractUIItem) {
        ULCComponent component = tabManager[abstractUIItem]
        if (component) {
            int tabIndex = tabbedPane.indexOfComponent(component)
            if (tabIndex >= 0)
                tabbedPane.setTitleAt(tabIndex, abstractUIItem.createTitle())
            else {
                dependentFramesManager.updateTabbedPaneTitle(abstractUIItem)
            }
        }
    }

    boolean isEmpty() {
          tabbedPane.getTabCount() == 0 && tabbedPane.dependentFrames.isEmpty()
    }
}
