package org.pillarone.riskanalytics.application.ui.customtable.view

import com.ulcjava.base.application.*
import com.ulcjava.base.application.event.*
import java.util.regex.Pattern
import org.pillarone.riskanalytics.application.ui.customtable.model.CustomTableModel
import org.pillarone.riskanalytics.application.ui.customtable.model.CustomTableHelper

/**
 * A TextField for editing the values in the CustomTable
 *
 * @author ivo.nussbaumer
 */
public class CellEditTextField extends ULCTextField {
    public  boolean          selectDataMode = false
    private int              insertDataPos = 0
    private CustomTableModel customTableModel
    private CustomTable      customTable
    private int row = 0
    private int col = 0

    /**
     * Constructor
     *
     * @param customTable the CustomTable
     */
    public CellEditTextField(CustomTable customTable) {
        this.customTable = customTable
        this.customTableModel = customTable.getModel()

        // If the users enters a '=' in the textbox, enable the selectDataMode
        this.addKeyListener(new IKeyListener() {
            void keyTyped(KeyEvent keyEvent) {
                if (keyEvent.keyChar == "=" || CellEditTextField.this.text == "=") {
                    selectDataMode = true
                }
            }
        })

        // When the focus on the textbox is lost, check if the selected text are variables, and enable the selectDataMode
        this.addFocusListener(new IFocusListener() {
            void focusGained(FocusEvent focusEvent) {
            }
            void focusLost(FocusEvent focusEvent) {
                Pattern variables_pattern = ~/[A-Z]+[0-9]+[A-Z0-9,]+[A-Z]+[0-9]+/
                String selectedText = CellEditTextField.this.getSelectedText()

                if (selectedText ==~ CustomTableHelper.variable_pattern ||
                    selectedText ==~ CustomTableHelper.range_pattern ||
                    selectedText ==~ variables_pattern) {
                    selectDataMode = true;
                }
            }
        })

        // when the user preses the Enter-key, copy the value of the textbox into the table, and move the cursor in the table
        this.addActionListener(new IActionListener(){
            void actionPerformed(ActionEvent actionEvent) {
                selectDataMode = false;
                customTableModel.setValueAt (CellEditTextField.this.text, CellEditTextField.this.row, CellEditTextField.this.col)

                int selectRow = CellEditTextField.this.row+1
                int selectCol = CellEditTextField.this.col

                if (selectRow >= CellEditTextField.this.customTable.rowCount) {
                    selectRow = 0
                    selectCol++

                    if (selectCol >= CellEditTextField.this.customTable.columnCount) {
                        selectCol = 0
                    }

                    CellEditTextField.this.customTable.getColumnModel().getSelectionModel().setSelectionInterval(selectCol, selectCol)
                    CellEditTextField.this.customTable.getSelectionModel().setSelectionInterval(selectRow, selectRow)
                } else {
                    CellEditTextField.this.customTable.getSelectionModel().setSelectionInterval(selectRow, selectRow)
                }
            }
        })
    }

    /**
     * Set the Text in the textbox
     * @param row the row of the cell, from where the value is
     * @param col the col of the cell, from where the value is
     */
    public void setText (int row, int col) {
        this.text = customTableModel.getDataAt (row, col)
        this.row = row
        this.col = col
        selectDataMode = false
    }

    /**
     * Inserts a string into the textfield (used for inserting variables of the selected cells)
     * @param data the string to insert
     */
    public void insertData (String data) {
        StringBuilder sb = new StringBuilder (this.text)
        sb.delete (this.getSelectionStart(), this.getSelectionEnd())
        sb.insert (this.getSelectionStart(), data)
        this.text = sb.toString()
        this.setSelectionEnd(this.getSelectionStart() + data.size())
        this.requestFocus()
    }
}