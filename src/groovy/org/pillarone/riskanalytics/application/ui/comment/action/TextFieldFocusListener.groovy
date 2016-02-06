package org.pillarone.riskanalytics.application.ui.comment.action

import com.ulcjava.base.application.ULCTextField
import com.ulcjava.base.application.event.FocusEvent
import com.ulcjava.base.application.event.IFocusListener
import com.ulcjava.base.application.util.Color
import groovy.transform.CompileStatic

/**
 * @author fouad.jaada@intuitive-collaboration.com
 *
 * Make search field show grey hint if user clicks away without typing anything.
 * When user clicks in the field clear the hint out (unless a search filter is present).
 *
 */
@CompileStatic
class TextFieldFocusListener implements IFocusListener {
    ULCTextField searchText
    final String hintText

    public TextFieldFocusListener(ULCTextField searchText, String hintText) {
        this.searchText = searchText;
        this.hintText = hintText
    }

    void focusGained(FocusEvent focusEvent) {
        String text = searchText.getText()
        if (hintText.equals(text)) {
            searchText.setText("")
            searchText.setForeground(Color.black)
        }

    }

    void focusLost(FocusEvent focusEvent) {
        String text = searchText.getText()
        if (!text || text == "") {
            searchText.setText(hintText)
            searchText.setForeground(Color.gray)
        }
    }

}
