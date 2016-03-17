package org.pillarone.riskanalytics.application.ui.main.view

import com.ulcjava.base.application.*
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.IActionListener
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.util.Color
import com.ulcjava.base.application.util.Dimension
import com.ulcjava.base.application.util.KeyStroke
import groovy.transform.CompileStatic
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.UserContext
import org.pillarone.riskanalytics.application.ui.base.model.modellingitem.FilterDefinition
import org.pillarone.riskanalytics.application.ui.base.model.modellingitem.NavigationTableTreeModel
import org.pillarone.riskanalytics.application.ui.comment.action.TextFieldFocusListener
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.user.UserManagement
import org.pillarone.riskanalytics.core.util.Configuration

import java.util.prefs.Preferences

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
@CompileStatic
class NavigationBarTopPane {
    private static Log LOG = LogFactory.getLog(NavigationBarTopPane)
    private static final String overrideSearchText = Configuration.coreGetAndLogStringConfig( "defaultSearchFilterText","")
    private static final boolean weAreRunningInATest =  ("test" == System.getProperty("grails.env"))
    private static Preferences preferences = Preferences.userNodeForPackage(this.class)
    private static final String SEARCH_FILTER_HINT = UIUtils.getText(NavigationBarTopPane, "searchText");


    private ULCToolBar toolBar
    private ULCToggleButton myStuffButton
    private ULCToggleButton assignedToMeButton
    private ULCTextField searchTextField
    private ULCButton clearButton
    NavigationTableTreeModel tableTreeModel

    private List<IFilterChangedListener> filterChangedListeners = []

    public NavigationBarTopPane(ULCToolBar toolBar, NavigationTableTreeModel tableTreeModel) {
        this.toolBar = toolBar
        this.tableTreeModel = tableTreeModel
    }

    void addFilterChangedListener(IFilterChangedListener listener) {
        filterChangedListeners << listener
    }

    void removeFilterChangedListener(IFilterChangedListener listener) {
        filterChangedListeners.remove(listener)
    }

    void fireFilterChanged(FilterDefinition newFilter) {
        filterChangedListeners*.filterChanged(newFilter)
    }

    public void init() {
        initComponents()
        layoutComponents()
        attachListeners()
    }

    protected void initComponents() {
        myStuffButton = new ULCToggleButton(UIUtils.getText(this.class, "MyStuff"))
        myStuffButton.name = "myStuffButton"
        myStuffButton.setPreferredSize new Dimension(100, 20)
        myStuffButton.setSelected(false)
        assignedToMeButton = new ULCToggleButton(UIUtils.getText(this.class, "assignedToMe"))
        assignedToMeButton.setPreferredSize new Dimension(100, 20)
        assignedToMeButton.setSelected(false)
        assignedToMeButton.setEnabled(false)

        searchTextField = new ULCTextField(name: "searchText")
        searchTextField.setMaximumSize(new Dimension(550, 20))
        searchTextField.setToolTipText SEARCH_FILTER_HINT
        if(weAreRunningInATest){
            searchTextField.setText( SEARCH_FILTER_HINT )
        }else{
            searchTextField.setText( overrideSearchText ?: preferences.get(searchFilterPrefsKey, "") ?: SEARCH_FILTER_HINT )
        }
        if(searchTextField.text == SEARCH_FILTER_HINT){
            searchTextField.setForeground(Color.gray)
        }
        searchTextField.setPreferredSize(new Dimension(500, 20))

        clearButton = new ULCButton(UIUtils.getIcon("cancel.png"))
        clearButton.name = "clearButton"
        clearButton.setToolTipText UIUtils.getText(this.class, "clear")

    }

    String getSearchFilterPrefsKey(){
        UserManagement.currentUser?.username + "_searchFilterText"
    }

    protected void layoutComponents() {
        if (UserContext.hasCurrentUser()) {
            toolBar.add(myStuffButton);
            toolBar.addSeparator()
            toolBar.add(assignedToMeButton);
            toolBar.addSeparator()
        }
        toolBar.add(searchTextField);
        toolBar.add(clearButton)
    }

    protected void attachListeners() {
        myStuffButton.addActionListener([actionPerformed: { ActionEvent event ->
            FilterDefinition filter = tableTreeModel.currentFilter
            filter.ownerFilter.active = myStuffButton.isSelected()
            fireFilterChanged(filter)
        }] as IActionListener)
        searchTextField.addFocusListener(new TextFieldFocusListener(searchTextField, SEARCH_FILTER_HINT))

        searchTextField.registerKeyboardAction(
            [actionPerformed: { ActionEvent e ->
                try{
                    searchAction()
                }catch(IllegalStateException ise){
                    // This alert fails - might be due to closure ?
                    //new ULCAlert("Search filter error", "Cause: ${ise.getMessage()}", "Ok").show()

                    //Instead, supply visible feedback via search filter
                    //
                    searchTextField.setText("Error: Transaction Service down ? ${ise.message}")
                    searchTextField.setForeground(Color.red)
                }
            }] as IActionListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false),
            ULCComponent.WHEN_FOCUSED
        );
        tableTreeModel.setNavigationBarTopPane(this) // eg allows refresh button to clear the search filter
        clearButton.addActionListener([actionPerformed: { ActionEvent e -> clearSearchFilterAction() }] as IActionListener )

    }

    // Intended to be called from somewhere during gui setup, after the listeners have been configured,
    // in case a search filter has been carried over from a prior session.
    // Not relevant during automated tests of course.
    public void initialFilterSearchAction(){
        if(!weAreRunningInATest) {
            if(filterChangedListeners.empty){
                LOG.warn("initialFilterSearchAction called when filterChangedListeners still empty");
            } else if( !overrideSearchText.empty || !preferences.get(searchFilterPrefsKey, "").empty ){
                try{
                    searchAction()
                }catch(IllegalStateException ise){
                    // See search action keyboard registration above for why alert not used
                    //
                    LOG.warn("Failed to re-instate persisted search filter - Transaction Service may be down", ise)
                    searchTextField.setText("Error: Transaction Service down ? ${ise.message}")
                    searchTextField.setForeground(Color.red)
                }
            }
        }
    }

    private void searchAction(){
        String text = searchTextField.getText()
        if(SEARCH_FILTER_HINT != text){
            preferences.put(searchFilterPrefsKey, text);
            FilterDefinition filter = tableTreeModel.currentFilter
            filter.allFieldsFilter.query = text
            fireFilterChanged(filter)
        }
    }
    public void clearSearchFilterAction(){
        // nb in applicationResources.properties :
        // search: parameterizations, results, templates, tags,...
        //
        searchTextField.setText(SEARCH_FILTER_HINT)
        searchTextField.setForeground Color.gray
        tableTreeModel.currentFilter.allFieldsFilter.query = ""
        tableTreeModel.currentFilter.ownerFilter.active = false
        fireFilterChanged(tableTreeModel.currentFilter)
        myStuffButton.setSelected false
        assignedToMeButton.setSelected false
    }

    private static String getLoggedUser() {
        return UserContext.getCurrentUser()?.getUsername()
    }

}
