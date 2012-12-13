/**
 *
 */
package gov.pnnl.improv.data;

import gov.pnnl.improv.db.SQLiteDatabaseConnection;
import gov.pnnl.improv.db.SQLiteDatabaseUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author D3P056
 *
 */
public class DataSourceManager
{
    private static final DataSourceManager INSTANCE = new DataSourceManager();
    
    // This is for example databases
    private static final String URI_PREFIX = "gov/pnnl/improv/improvdatasource/";

    private Map<String, IDataSource> mDatasourcesMap = new HashMap<String, IDataSource>();
    private String mActiveDatasource;
    private Set<String> mActiveDatasources = new HashSet<String>();
    private boolean mLoading = false;


    public DataSourceManager()
    {
        loadPreferredDatasources();
    }

    public static DataSourceManager getInstance()
    {
        return INSTANCE;
    }

    public void addDatasource(IDataSource datasource)
    {
        datasource.getType().addDatasource(datasource);

        if (!this.mDatasourcesMap.containsKey(datasource.getName()))
        {
            this.mDatasourcesMap.put(datasource.getName(), datasource);
        }

        if (!mLoading)
        {
//            DataEventHandler.fireDataSourceAdded(datasource);
        }
    }

    public void removeDatasource(IDataSource datasource)
    {
        datasource.getType().removeDatasource(datasource);
        if (this.mDatasourcesMap.containsKey(datasource.getName()))
        {
            this.mDatasourcesMap.remove(datasource.getName());
        }

//        DataEventHandler.fireDataSourceDeleted(datasource);
    }

    public void updateDatasource(IDataSource datasource)
    {
        String nameFound = null;
        for (String name : this.mDatasourcesMap.keySet())
        {
            if (this.mDatasourcesMap.get(name).equals(datasource))
            {
                nameFound = name;
                break;
            }
        }
        if (nameFound != null)
        {
            this.mDatasourcesMap.remove(nameFound);
            addDatasource(datasource);
            persistDatasources();
        }

//        DataEventHandler.fireDataSourceUpdated(datasource);
    }

    public Set<String> getDatasourceNames()
    {
        return this.mDatasourcesMap.keySet();
    }
    

    public Collection<IDataSource> getDatasources()
    {
        return this.mDatasourcesMap.values();
    }

    public void clearActiveDatasource()
    {
        IDataSource oldDatasource = getActiveDatasource();
        oldDatasource.setActive(false);
        mActiveDatasource = null;
        
//        DataEventHandler.fireDataSourceActiveChanged(oldDatasource);
//        DataEventHandler.fireActiveDatasourceChanged(null, oldDatasource);
    }

    public void setDatasourceAsActive(String name, boolean active)
    {
        IDataSource ds = mDatasourcesMap.get(name);
        this.mDatasourcesMap.get(name).setActive(active);
        IDataSource oldDatasource = null;

        if (active)
        {
            for (String ad : mActiveDatasources)
            {
                oldDatasource = mDatasourcesMap.get(ad);
                oldDatasource.setActive(false);
                break;
            }

            mActiveDatasources.clear();
            mActiveDatasources.add(name);
        }
        else
        {
            mActiveDatasources.remove(name);
        }
        
//        DataEventHandler.fireDataSourceActiveChanged(ds);
//        DataEventHandler.fireActiveDatasourceChanged(oldDatasource, ds);
    }

    public List<IDataSource> getActiveDatasources()
    {
        List<IDataSource> actives = new ArrayList<IDataSource>();
        for (String activeName : this.mActiveDatasources)
        {
            actives.add(this.mDatasourcesMap.get(activeName));
        }
        return actives;
    }

    public void setActiveDatasource(String name)
    {
        this.mActiveDatasource = name;
        setDatasourceAsActive(name, true);
    }

    public IDataSource getActiveDatasource()
    {
        return this.mDatasourcesMap.get(this.mActiveDatasource);
    }

    public IDataSource getDatasourceForName(String name)
    {
        return this.mDatasourcesMap.get(name);
    }

    public boolean hasDatasource(String name)
    {
        return this.mDatasourcesMap.containsKey(name);
    }

    public String getUniqueName(String name)
    {
        int count = 1;
        while (this.mDatasourcesMap.containsKey(name + count))
        {
            count++;
        }
        return name + count;
    }

    public boolean isUniqueName(String name, IDataSource ds)
    {
        if (!this.mDatasourcesMap.containsKey(name))
        {
            return true;
        }

        if (this.mDatasourcesMap.get(name).equals(ds))
        {
            return true;
        }

        return false;
    }

    public void persistDatasources()
    {
//        savePreferredDatasources();
    }

    private void loadPreferredDatasources()
    {
        mLoading = true;

        Set<String> locations = new HashSet<String>();
        locations.add(URI_PREFIX + "clans.improv");
        locations.add(URI_PREFIX + "hallam.improv");
        locations.add(URI_PREFIX + "pfam.improv");
        loadFromDataFolder(locations);
        
        mLoading = false;
    }

    private void loadFromDataFolder(Set<String> Uris)
    {
        for (String dburi : Uris)
        {
            int index = dburi.lastIndexOf("/");
            String name = dburi.substring(index + 1);
            loadDatasource(name, dburi, DataSourceType.LOCAL);
        }
    }

    private void loadDatasource(String name, String dbFileUri, DataSourceType type)
    {
        try
        {
            TreeDBDataSource ds = new TreeDBDataSource(name, dbFileUri, name, type);
            ds.setNodeTable("nodes");
            ds.setEdgeTable("edges");
            final SQLiteDatabaseConnection conn = new SQLiteDatabaseConnection();
            conn.connect(dbFileUri);

            //--------------- TODO ------------------------------------------
            // Add it in background later
            SQLiteDatabaseUtil.createPrefuseTable(conn);
            addDatasource(ds);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
