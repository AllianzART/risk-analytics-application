package org.pillarone.riskanalytics.application.ui.base.model

import com.ulcjava.base.application.ULCPopupMenu
import com.ulcjava.base.application.ULCTableTree
import com.ulcjava.base.application.util.Font
import com.ulcjava.base.application.util.ULCIcon
import org.pillarone.riskanalytics.application.ui.main.view.MainSelectionTableTreeCellRenderer

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
public interface NavigationTreeNode {

    public ULCPopupMenu getPopupMenu(MainSelectionTableTreeCellRenderer renderer, ULCTableTree tree)

    public ULCIcon getIcon()

    public Font getFont(String fontName, int fontSize)

    public String getToolTip()

}