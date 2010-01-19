package org.pillarone.riskanalytics.application.ui.util

import java.text.NumberFormat
import org.pillarone.riskanalytics.core.parameterization.AbstractMultiDimensionalParameter
import org.pillarone.riskanalytics.application.ui.parameterization.view.MultiDimensionalCellRenderer

/**
    Format a MultiDimensionalParam for the parameter view, where only an indication of the
    backing data should be shown.
    These methods are used for the cell value itself. The tooltip content is defined in
    {@link MultiDimensionalCellRenderer}
 */
public class Formatter {

    public static final int MAX_DISPLAY_COLS = 5
    public static final int MAX_DISPLAY_ROWS = 4

    static String format(AbstractMultiDimensionalParameter mdp, Locale locale) {
        List values = mdp.values
        if (! values ) return ''
        def cols = mdp.columnCount - mdp.titleColumnCount
        def rows = mdp.rowCount - mdp.titleRowCount
        if (rows > MAX_DISPLAY_ROWS || cols > MAX_DISPLAY_COLS ) return "<$cols/$rows>"
        StringBuilder result = new StringBuilder()
        result << format(values, locale)
        return result.toString()
    }

    static String format(List list, Locale locale) {
        if (list.any {it instanceof List}) {
            StringBuilder result = new StringBuilder()
            result << "["
            result << list.collect {format(it, locale)}.join("; ")
            result << "]"
            return result.toString()
        }

        List values = list
        NumberFormat format = NumberFormat.getInstance(locale)
        values = list.collect {
            if (it instanceof Number) {
                return format.format(it)
            } else {
                return String.valueOf(it)
            }
        }
        return "[${values.join("; ")}]".toString()
    }
}
