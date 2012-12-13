/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.data;

import gov.pnnl.improv.events.ActiveDatasourceChangedListener;
import gov.pnnl.improv.events.DatasourcesUpdatedListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import org.openide.util.Lookup;


/**
 *
 * @author D3X924
 */
public class DataEventHandler
{
    private static final DataEventHandler INSTANCE = new DataEventHandler();
    
    private DataEventHandler()
    {
        
    }
    
    public static DataEventHandler getInstance()
    {
        return INSTANCE;
    }
    
    public static void fireActiveDatasourceChanged(IDataSource oldDs, IDataSource ds)
    {
        Collection<ActiveDatasourceChangedListener> items =
                (Collection<ActiveDatasourceChangedListener>)Lookup.getDefault()
                .lookupAll(ActiveDatasourceChangedListener.class);
        
        if (items != null && !items.isEmpty())
        {
            for (ActiveDatasourceChangedListener adc : items)
            {
                adc.activeDatasourceChanged(ds);
            } 
        }
        
        /*
        try
        {
            IViewReference[] viewRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
            for (int i = 0; i < viewRefs.length; i++)
            {
                IViewPart view = viewRefs[i].getView(false);
                if (view instanceof ActiveDatasourceChangedListener)
                {
                    ((ActiveDatasourceChangedListener) view).activeDatasourceChanged(ds);
                }
            }
            if (ds.isActive())
            {
                StatisticsManager.getInstance().addDatasourceToTree(ds);
                if (oldDs != null)
                {
                    StatisticsManager.getInstance().removeDatasourceFromTree(oldDs);
                }
            } else
            {
                StatisticsManager.getInstance().removeDatasourceFromTree(ds);
            }
        } catch (NullPointerException npe)
        {
        }
        */
    }
    
    public static void fireDataSourceAdded(IDataSource datasource)
    {
        fireDatasourcesUpdated(DatasourcesUpdatedListener.ADD, datasource);
    }

    public static void fireDataSourceDeleted(IDataSource datasource)
    {
        fireDatasourcesUpdated(DatasourcesUpdatedListener.DELETE, datasource);
    }

    public static void fireDataSourceUpdated(IDataSource datasource)
    {
        fireDatasourcesUpdated(DatasourcesUpdatedListener.UPDATE, datasource);
    }

    public static void fireDataSourceActiveChanged(IDataSource datasource)
    {
        fireDatasourcesUpdated(DatasourcesUpdatedListener.ACTIVE_CHANGE, datasource);
    }

    public static void fireDatasourcesUpdated(int type, IDataSource datasource)
    {
        Collection<DatasourcesUpdatedListener> items =
                (Collection<DatasourcesUpdatedListener>)Lookup.getDefault()
                .lookupAll(DatasourcesUpdatedListener.class);
        
        if (items != null && !items.isEmpty())
        {
            for (DatasourcesUpdatedListener ev : items)
            {
                ev.datasourcesUpdated(type, datasource);
            } 
        }
    }

}
