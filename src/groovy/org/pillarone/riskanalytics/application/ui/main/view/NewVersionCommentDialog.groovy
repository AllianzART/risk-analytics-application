package org.pillarone.riskanalytics.application.ui.main.view

import com.ulcjava.base.application.event.IActionListener
import com.ulcjava.base.application.event.KeyEvent
import com.ulcjava.base.application.util.Dimension
import com.ulcjava.base.application.util.KeyStroke
import com.ulcjava.base.application.*
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.pillarone.riskanalytics.application.ui.util.UIUtils
import org.pillarone.riskanalytics.core.simulation.item.Parameterization

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class NewVersionCommentDialog {
    private static Log LOG = LogFactory.getLog(NewVersionCommentDialog)

    ULCDialog dialog
    ULCTextArea commentTextArea
    private ULCButton okButton
    private ULCButton cancelButton
    private String initialText = null
    private String okLabel = null
    Closure okAction
    String title

    public NewVersionCommentDialog(Closure okAction) {
        this.okAction = okAction
        initComponents()
        layoutComponents()
        attachListeners()
    }

    public NewVersionCommentDialog(Closure okAction, String initialText, String okLabel ) {
        this.initialText = initialText
        this.okLabel=okLabel
        this.okAction = okAction
        initComponents()
        layoutComponents()
        attachListeners()
    }

    private void initComponents() {
        ULCWindow window = UIUtils.getWindowAncestor()
        dialog = new ULCDialog(window, UIUtils.getText(NewVersionCommentDialog, "addComment"), true)
        dialog.name = 'renameDialog' // TODO if we fix name on dialog which tests will break lol ?!
        commentTextArea = new ULCTextArea(5, 45)
        commentTextArea.setMinimumSize(new Dimension(200, 160))
        commentTextArea.setMaximumSize(new Dimension(400, 160))
        commentTextArea.name = "commentTextArea"
        commentTextArea.lineWrap = true
        commentTextArea.wrapStyleWord = true
        if(initialText){
            commentTextArea.text = initialText
        }
        okButton = new ULCButton(okLabel ?: UIUtils.getText(NewVersionCommentDialog, "createNewVersion"))
        okButton.name = 'okButton'
        cancelButton = new ULCButton(UIUtils.getText(NewVersionCommentDialog, "Cancel"))

    }

    private void layoutComponents() {
        ULCBoxPane content = new ULCBoxPane(rows: 2, columns: 4)
        content.border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
        content.add(3, ULCBoxPane.BOX_EXPAND_CENTER, new ULCScrollPane(commentTextArea))
        content.add(ULCBoxPane.BOX_EXPAND_BOTTOM, new ULCFiller())
        content.add(ULCBoxPane.BOX_EXPAND_BOTTOM, new ULCFiller())
        okButton.setPreferredSize(new Dimension(160, 20))
        content.add(ULCBoxPane.BOX_RIGHT_BOTTOM, okButton)
        cancelButton.setPreferredSize(new Dimension(160, 20))
        content.add(ULCBoxPane.BOX_RIGHT_BOTTOM, cancelButton)

        dialog.add(content)
        dialog.setLocationRelativeTo(UIUtils.getWindowAncestor())
        dialog.pack()
        dialog.resizable = false

    }

    public void show() {
        dialog.visible = true
    }

    public hide() {
        dialog.visible = false
    }

    private void attachListeners() {
        IActionListener action = [actionPerformed: { e ->
            if(initialText){
                String oneLiner = ""+initialText
                LOG.info(oneLiner.replaceAll('\n', ' '));
                boolean debugMe = true;
            }
            okAction.call(commentTextArea.getText()); hide();
        }] as IActionListener
        okButton.addActionListener(action)
        commentTextArea.registerKeyboardAction(action, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK, false), ULCComponent.WHEN_FOCUSED)
        cancelButton.addActionListener([actionPerformed: { e -> hide() }] as IActionListener)
    }


}
