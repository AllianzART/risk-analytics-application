package org.pillarone.riskanalytics.application.ui.pivot.model.CustomTable

import com.ulcjava.base.application.table.AbstractTableModel
import com.ulcjava.base.application.AbstractListModel

/**
 * TableModel for the CustomTable
 *
 * @author ivo.nussbaumer
 */
public class CustomTableModel extends AbstractTableModel {
    private List<List<Object>> data
    private RowHeaderListModel rowHeaderModel
    private GroovyShell groovyShell

    public boolean editMode

    /**
     * Constructor
     *
     * @param data Initial Table-Data
     */
    public CustomTableModel(List<List<Object>> data) {
        this.data = data
        
        groovyShell = new GroovyShell()
        rowHeaderModel = new RowHeaderListModel(this)
    }

    /**
     * Inserts a Column into the table
     *
     * @param col Position to insert the new Column
     */
    public void insertCol (int col) {
        for (List<Object> rowData : data) {
            rowData.add (col, "")
        }
        fireTableStructureChanged()
    }

    /**
     * Adds a Column to the end of the table
     * @return new Number of Columns
     */
    public int addCol () {
        for (List<Object> rowData : data) {
            rowData.add ("")
        }

        fireTableStructureChanged()
        return columnCount-1
    }

    /**
     * Deletes a Column
     * @param col Index of the Column to delete
     */
    public void deleteCol (int col) {
        for (List<Object> rowData : data) {
            rowData.remove(col)
        }
        fireTableStructureChanged()
    }

    /**
     * Inserts a Row into the table
     *
     * @param col Position to insert the new Row
     */
    public void insertRow (int row, List<Object> rowData = null) {
        if (rowData == null)
            rowData = new LinkedList<Object>()

        while (rowData.size() < columnCount) {
            rowData.add ("")
        }

        this.data.add(row, rowData)

        fireTableRowsInserted(row, row)
        rowHeaderModel.fireContentsChanged(this, row, rowCount-1)
    }

    /**
     * Adds a Row to the end of the table
     * @return new Number of Row
     */
    public int addRow (List<Object> rowData = null) {
        if (rowData == null)
            rowData = new LinkedList<Object>()

        while (rowData.size() < columnCount) {
            rowData.add ("")
        }

        this.data.add(rowData)

        fireTableRowsInserted(rowCount-1, rowCount-1)
        rowHeaderModel.fireContentsChanged(this, rowCount-1, rowCount-1)
        return rowCount-1
    }

    /**
     * Deletes a Row
     * @param row Index of the Row to delete
     */
    public void deleteRow (int row) {
        data.remove(row)
        fireTableStructureChanged()
        rowHeaderModel.fireContentsChanged(this, row, rowCount-1)
    }

    /**
     * Calculates the number of columns, depending on the stored data
     * @return
     */
    public int getColumnCount() {
        int colCount = 0
        for (List<Object> rowData : data) {
            colCount = (rowData.size() > colCount) ? rowData.size() : colCount
        }
        return colCount
    }

    /**
     * Calculates the number of rows, depending on the stored data
     * @return
     */
    public int getRowCount() {
        return data.size()
    }

    /**
     * Returns the Header-Name of the Column, formatted as an Excel-Like string
     * @param col the index of the column
     * @return header-name as an excel-like string
     */
    public String getColumnName(int col) {
        return CustomTableHelper.getColString(col+1)
    }

    public Class getColumnClass(int col) {
        return String.class
    }

    public boolean isCellEditable(int row, int col) {
        return editMode
    }

    /**
     * Returns the data (formula, data, or constant-value) of the cell
     *
     * @param row the row of the cell
     * @param col the column of the cell
     * @return the data of the cell
     */
    public Object getDataAt(int row, int col) {
        if (row >= rowCount || col >= columnCount)
            return null

        return data[row][col]
    }

    /**
     * Returns the value to display (resolved formula, value of a data-cell) in the table of a cell
     *
     * @param row the row of the cell
     * @param col the column of the cell
     * @return the value to display of the cell
     */
    public Object getValueAt(int row, int col) {
        if (row >= rowCount || col >= columnCount)
            return null

        // If editMode, just return the original data
        if (editMode == true) {
            return data[row][col]
        }

        String cellData = data[row][col]

        // if cellData is a formula, resolve the formula
        if (cellData.startsWith("=")) {
            String formula = CustomTableHelper.replaceVariables (this, cellData.substring(1), row, col)
            formula = CustomTableHelper.executeFunctions (formula)
            Object value = groovyShell.evaluate("return " + formula)
            return value
        }

        // if cellData is a data-Reference, get the value from the database
        if (cellData.startsWith("#")) {
            String dataReference = CustomTableHelper.replaceVariables (this, cellData.substring(1), row, col)
            return ""
        }

        return cellData
    }

    /**
     * sets the data of a cell
     *
     * @param value the data to set
     * @param row the row of the cell
     * @param col the column of the cell
     */
    public void setValueAt(Object value, int row, int col) {
        if (row >= rowCount || col >= columnCount)
            return

        data[row][col] = value
        fireTableCellUpdated(row, col)
    }

    /**
     * ListModel for the RowHeader-List
     */
    private class RowHeaderListModel extends AbstractListModel {
        CustomTableModel customTableModel

        /**
         * Constructor
         * @param customTableModel the CustomTableModel
         */
        public RowHeaderListModel (CustomTableModel customTableModel) {
            this.customTableModel = customTableModel
        }

        /**
         * The size of the RowHeaderListModel is the number of rows in the CustomTableModel
         * @return
         */
        public int getSize() {
            return customTableModel.rowCount
        }

        /**
         * the Name of a row is just the number of the row+1
         * @param i the row index
         * @return the name of the element
         */
        public Object getElementAt(int row) {
            return (row+1).toString()
        }
    }
}
