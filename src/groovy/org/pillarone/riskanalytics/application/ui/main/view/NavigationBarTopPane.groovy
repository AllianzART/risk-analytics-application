package org.pillarone.riskanalytics.application.ui.main.view

import com.ulcjava.base.application.*
import com.ulcjava.base.application.event.ActionEvent
import com.ulcjava.base.application.event.IActionListener
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.util.Color
import com.ulcjava.base.application.util.Dimension
import com.ulcjava.base.application.util.KeyStroke
import groovy.transform.CompileStatic
import org.pillarone.riskanalytics.application.UserContext
import org.pillarone.riskanalytics.application.ui.base.model.modellingitem.FilterDefinition
import org.pillarone.riskanalytics.application.ui.base.model.modellingitem.NavigationTableTreeModel
import org.pillarone.riskanalytics.application.ui.comment.action.TextFieldFocusListener
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.util.Configuration

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
@CompileStatic
class NavigationBarTopPane {
    private static final String overrideSearchText = Configuration.coreGetAndLogStringConfig( "defaultSearchFilterText","")

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
        searchTextField.setMaximumSize(new Dimension(300, 20))
        searchTextField.setToolTipText UIUtils.getText(this.class, "searchText")
        searchTextField.setText( overrideSearchText ?: UIUtils.getText(this.class, "searchText") )
        searchTextField.setForeground(Color.gray)
        searchTextField.setPreferredSize(new Dimension(250, 20))

        clearButton = new ULCButton(UIUtils.getIcon("delete-active.png"))
        clearButton.name = "clearButton"
        clearButton.setToolTipText UIUtils.getText(this.class, "clear")

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
        searchTextField.addFocusListener(new TextFieldFocusListener(searchTextField))

        searchTextField.registerKeyboardAction(
            [actionPerformed: { ActionEvent e -> searchAction() }] as IActionListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false),
            ULCComponent.WHEN_FOCUSED
        );
        tableTreeModel.setNavigationBarTopPane(this) // to allow refresh button to clear the search filter ?
        clearButton.addActionListener([actionPerformed: { ActionEvent e -> clearSearchFilterAction() }] as IActionListener )

        if(!overrideSearchText.empty){
            searchAction()
        }
    }

    private void searchAction(){
            String text = searchTextField.getText()
            FilterDefinition filter = tableTreeModel.currentFilter
            filter.allFieldsFilter.query = text
            fireFilterChanged(filter)
        }
    public void clearSearchFilterAction(){
        // nb in applicationResources.properties :
        // search: parameterizations, results, templates, tags,...
        //
        searchTextField.setText(UIUtils.getText(this.class, "searchText"))
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
