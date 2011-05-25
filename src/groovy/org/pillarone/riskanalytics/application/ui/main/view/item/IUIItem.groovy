package org.pillarone.riskanalytics.application.ui.main.view.item

import com.ulcjava.base.application.ULCContainer
import org.pillarone.riskanalytics.application.ui.main.model.IContentModel

//classes implementing this interface need to be created for all item-"views" (modelling items, batch runs, comparisons etc.)
//todo fja interface not used
public interface IUIItem {

    /**
     *  This method returns a String which can be used to represent this item on the ui (window/tab titles etc.)
     * @return
     */
    public String createTitle()

    /**
     * Creates a view when the item is opened by the user.
     * Currently a ULCComponent, maybe we can switch to a 'AbstractDetailView'
     * @return
     */
    public ULCContainer createDetailView()

    public IContentModel getViewModel()

    /**
     * Cleanup when an UIItem is closed (remove listeners etc.)
     */
    public void close()

    /**
     * @param newName
     */
    public void rename(String newName)

    public void save()

    public boolean remove()

    public void delete()

    public IUIItem incrementVersion()

    public boolean isEditable()

    public boolean isLoaded()

    public Object getItem()


}