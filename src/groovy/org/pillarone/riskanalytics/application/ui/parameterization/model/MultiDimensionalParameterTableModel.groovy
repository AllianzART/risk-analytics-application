package org.pillarone.riskanalytics.application.ui.parameterization.model

import com.ulcjava.base.application.table.AbstractTableModel
import org.joda.time.DateTime
import org.pillarone.riskanalytics.application.ui.base.model.IBulkChangeable
import org.pillarone.riskanalytics.application.ui.base.model.IModelChangedListener
import org.pillarone.riskanalytics.core.parameterization.AbstractMultiDimensionalParameter
import org.pillarone.riskanalytics.core.parameterization.ComboBoxMatrixMultiDimensionalParameter
import org.pillarone.riskanalytics.core.parameterization.MultiDimensionalParameterDimension
import org.pillarone.riskanalytics.core.parameterization.ConstrainedMultiDimensionalParameter
import org.pillarone.riskanalytics.core.components.ResourceHolder
import org.pillarone.riskanalytics.core.simulation.item.VersionNumber
import org.pillarone.riskanalytics.core.parameterization.IComboBoxBasedMultiDimensionalParameter
import org.pillarone.riskanalytics.core.parameterization.PeriodMatrixMultiDimensionalParameter

class MultiDimensionalParameterTableModel extends AbstractTableModel implements IBulkChangeable {
    private boolean bulkChange = false
    private List changedCells

    AbstractMultiDimensionalParameter multiDimensionalParam
    private List listeners
    private boolean indexed = false

    boolean readOnly = false

    public MultiDimensionalParameterTableModel() {
    }

    public MultiDimensionalParameterTableModel(AbstractMultiDimensionalParameter multiDimensionalParam) {
        this.@multiDimensionalParam = multiDimensionalParam
        this.listeners = []
    }

    public MultiDimensionalParameterTableModel(AbstractMultiDimensionalParameter multiDimensionalParam, boolean indexed) {
        this(multiDimensionalParam)
        this.indexed = indexed
    }

    void addListener(IModelChangedListener listener) {
        listeners << listener
    }

    void notifyModelChanged() {
        if (!bulkChange) {
            listeners.each {it.modelChanged()}
        }
    }

    void addColumnAt(int index) {
        multiDimensionalParam.addColumnAt(getIndex(index, columnCount))
        notifyModelChanged()
        fireTableStructureChanged()
    }

    void removeColumnAt(int index) {
        multiDimensionalParam.removeColumnAt(getIndex(index, columnCount))
        notifyModelChanged()
        fireTableStructureChanged()
    }

    void addRowAt(int index) {
        multiDimensionalParam.addRowAt(getIndex(index, rowCount))

        notifyModelChanged()
        fireTableStructureChanged()
    }

    void removeRowAt(int index) {
        multiDimensionalParam.removeRowAt(getIndex(index, rowCount))
        notifyModelChanged()
        fireTableStructureChanged()
    }

    void removeColumnAndRow(int index) {
        multiDimensionalParam.removeColumnAt(getIndex(index, columnCount))
        multiDimensionalParam.removeRowAt(getIndex(index, rowCount))
        notifyModelChanged()
        fireTableStructureChanged()
    }

    void addColumnAndRow(int index) {
        multiDimensionalParam.addColumnAt(getIndex(index, columnCount))
        multiDimensionalParam.addRowAt(getIndex(index, rowCount))
        notifyModelChanged()
        fireTableStructureChanged()
    }

    void moveColumnTo(int from, int to) {
        multiDimensionalParam.moveColumnTo(from, to)
        notifyModelChanged()
        fireTableStructureChanged()
    }

    public void moveRowTo(int from, int to) {
        multiDimensionalParam.moveRowTo(from, to)
        notifyModelChanged()
        fireTableStructureChanged()
    }

    public void moveColumnAndRow(int from, int to) {
        multiDimensionalParam.moveColumnTo(from, to)
        multiDimensionalParam.moveRowTo(from, to)
        notifyModelChanged()
        fireTableStructureChanged()
    }

    public int getColumnCount() {
        def count = multiDimensionalParam.getColumnCount()
        return indexed ? count + 1 : count
    }

    public int getRowCount() {
        return multiDimensionalParam.getRowCount()
    }

    public int getValueColumnCount() {
        def count = multiDimensionalParam.getValueColumnCount()
        return indexed ? count + 1 : count
    }

    public int getValueRowCount() {
        return multiDimensionalParam.getValueRowCount()
    }

    public Object getValueAt(int row, int column) {
        if (column == 0 && row == 0) return ""
        if (column == 0) return row
        Object value = multiDimensionalParam.getValueAt(row, column - 1)
        if (value instanceof DateTime) {
            value = value.toDate()
        } else if (value instanceof ResourceHolder) {
            value = value.toString()
        }
        return value
    }

    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (value == null || readOnly) {
            return
        }
        if (value instanceof Date) {
            value = new DateTime(value.time)

        }

        Object oldValue = getValueAt(rowIndex, columnIndex)
        // This check is because the use of a ErrorManager in the Editor causes the wrong input to be send to the ULC-side.
        // The wrong input will be sent as String instead of a Number, so we check the type of the old value against the new one
        if (value instanceof String) {
            Class oldValueClazz = oldValue?.class
            if (oldValueClazz != String) {
                return
            }
        }
        Object originalValue = multiDimensionalParam.getValueAt(rowIndex, columnIndex - 1)
        if (originalValue instanceof ResourceHolder) {
            value = new ResourceHolder(originalValue.resourceClass, value.substring(0, value.lastIndexOf(" ")), new VersionNumber(value.substring(value.lastIndexOf(" ") + 2)))
        }

        if (value != null && !value.equals(oldValue)) {
            multiDimensionalParam.setValueAt(value, rowIndex, columnIndex - 1)
            fireTableCellUpdated rowIndex, columnIndex
            if (multiDimensionalParam instanceof ComboBoxMatrixMultiDimensionalParameter) {
                //CBMMDP row/column titles are symmetric
                fireTableCellUpdated 0, rowIndex + 1
                if (columnIndex > rowIndex)
                    fireTableCellUpdated columnIndex - 1, rowIndex + 1
            } else if (multiDimensionalParam instanceof PeriodMatrixMultiDimensionalParameter) {
                fireTableCellUpdated(columnIndex - 1, rowIndex + 1)
            }
            notifyModelChanged()
        }

    }

    public void fireTableCellUpdated(int row, int column) {
        if (bulkChange) {
            changedCells << new TableCellLocation(row: row, col: column)
        } else {
            super.fireTableCellUpdated(row, column)
        }
    }

    @Override
    void fireTableStructureChanged() {
        if (!bulkChange) {
            super.fireTableStructureChanged()
        }
    }



    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return false
        }
        return !readOnly && multiDimensionalParam.isCellEditable(rowIndex, columnIndex - 1)
    }

    List currentValues() {
        return multiDimensionalParam.getValues()
    }


    void setDimension(MultiDimensionalParameterDimension dimension) {
        multiDimensionalParam.setDimension(dimension)
        notifyModelChanged()
        fireTableStructureChanged()
    }

    boolean isValuesConverted() {
        return multiDimensionalParam.valuesConverted
    }

    public getPossibleValues(int row, int col) {
        if (col == 0) {
            return row
        }
        multiDimensionalParam.getPossibleValues(row, col - 1)
    }

    public void startBulkChange() {
        changedCells = []
        bulkChange = true
    }

    public void stopBulkChange() {
        bulkChange = false
        if (changedCells.size() > 0) {
            int minRow = changedCells.row.sort()[0]
            int maxRow = changedCells.row.sort()[-1]
            fireTableRowsUpdated minRow, maxRow
            notifyModelChanged()
        }

    }

    public boolean columnCountChangeable() {
        return multiDimensionalParam.columnCountChangeable()
    }

    public boolean rowCountChangeable() {
        return multiDimensionalParam.rowCountChangeable()
    }

    private int getIndex(int index, int max) {
        if (index < 0) return 0
        return Math.min(index, max)
    }

    public static MultiDimensionalParameterTableModel getInstance(AbstractMultiDimensionalParameter parameter) {
        if (parameter instanceof ConstrainedMultiDimensionalParameter) {
            return new ConstrainedMultiDimensionalParameterTableModel(parameter, true)
        } else if (parameter instanceof PeriodMatrixMultiDimensionalParameter) {
            return new PeriodMultiDimensionalParameterTableModel(parameter, true)
        }
        else if (parameter instanceof IComboBoxBasedMultiDimensionalParameter) {
            return new ComboBoxMultiDimensionalParameterTableModel(parameter, true)
        }
        return new MultiDimensionalParameterTableModel(parameter, true)
    }

}

class TableCellLocation {
    int row
    int col
}

