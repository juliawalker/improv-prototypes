package gov.pnnl.improv.views.treemap;

import gov.pnnl.improv.datasource.AbstractSQLiteDataSource;
import gov.pnnl.improv.datasource.DataSourceManager;
import gov.pnnl.improv.data.IDataSource;
import gov.pnnl.improv.events.ActiveDatasourceChangedListener;
import gov.pnnl.improv.ui.layout.CustomSquarifiedTreeMapLayout;
import gov.pnnl.improv.views.GalaxyViewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;


public class GalaxyViewerManager implements ActiveDatasourceChangedListener
{

    private static GalaxyViewerManager INSTANCE = new GalaxyViewerManager();
    private int m_layout_type = CustomSquarifiedTreeMapLayout.DEFAULT;
    Map<String, GalaxyViewer> m_dsNameToViewerId;

    public GalaxyViewerManager()
    {
        this.m_dsNameToViewerId = new HashMap<String, GalaxyViewer>();
    }

    public static GalaxyViewerManager getInstance()
    {
        return INSTANCE;
    }

    public void setLayout(int newLayout)
    {
        this.m_layout_type = newLayout;
    }

    @Override
    public void activeDatasourceChanged(final IDataSource datasource)
    {
        final String name = datasource.getName();
        if (datasource.isActive())
        {
            // create a new galaxy viewer for the new datasource
            try
            {
//				GalaxyViewer viewer = null;
                IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
//				if (this.m_dsNameToViewerId.isEmpty()) {
//					IViewReference[] refs = page.getViewReferences();
//					
//				}
                if (!this.m_dsNameToViewerId.containsKey(name))
                {
                    GalaxyViewer viewer = (GalaxyViewer) page.showView("gov.pnl.improv.views.GalaxyViewer", "gvsecondary" + name, IWorkbenchPage.VIEW_VISIBLE);
                    page.showView("gov.pnl.improv.views.GalaxyViewer", "gvsecondary" + name, IWorkbenchPage.VIEW_ACTIVATE);
//					viewer.setActiveDatasourceChanged(datasource);					
                    // add to map to keep track of datasources to galaxy viewer instances
                    this.m_dsNameToViewerId.put(name, viewer);
                }
                GalaxyViewer defView = (GalaxyViewer) page.findView("gov.pnl.improv.views.GalaxyViewer");
                if (defView != null)
                {
                    page.hideView(defView);
                    defView.dispose();
                }
            } catch (PartInitException e)
            {
                e.printStackTrace();
            }
        } else
        {
            // close the corresponding datasource
//			this.m_dsNameToViewerId.get(name).dispose();
            // remove the datasource from map of instances
            this.m_dsNameToViewerId.remove(name);
        }
    }

    public String[] getCommonScoreFields()
    {
        DataSourceManager dsm = DataSourceManager.getInstance();
        String[] scoreFields = new String[0];
        for (String dsName : this.m_dsNameToViewerId.keySet())
        {
            ArrayUtils.addAll(scoreFields, getScoreFields(dsm.getDatasourceForName(dsName)));
        }
        return scoreFields;
    }

    private String[] getScoreFields(IDataSource datasource)
    {
        if (datasource != null)
        {
            AbstractSQLiteDataSource ds = (AbstractSQLiteDataSource) datasource;
            Map<String, String> cols = ds.getColumnAndTypesForTable(ds.getNodeTable());
            List<String> scoreField = new ArrayList<String>();
            for (String col : cols.keySet())
            {
                if (!col.equals("id") && !col.equals("nid") && !col.equals("visible"))
                {
                    if (cols.get(col).equals("INTEGER") || cols.get(col).equals("REAL"))
                    {
                        scoreField.add(col);
                    }
                }
            }
            return scoreField.toArray(new String[scoreField.size()]);
        } else
        {
            return null;
        }
    }

    public int getLayout()
    {
        return this.m_layout_type;
    }

}
