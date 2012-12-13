/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.datasource.ui;

import gov.pnnl.improv.data.DataSourceManager;
import gov.pnnl.improv.data.IDataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;


/**
 *
 * @author D3X924
 */
public class DataSourceQuery implements Lookup.Provider
{
    private final Lookup mLookup;
    private final InstanceContent mContent;
    private Collection<IDataSource> mDataSourceList;
    
    public DataSourceQuery()
    {
        mDataSourceList = new ArrayList<IDataSource>();
        mContent = new InstanceContent();
        mLookup = new AbstractLookup(mContent);
        loadDataSources();
        
        mContent.add( new IReloadableDataSource() {
            @Override
            public void reload() throws Exception
            {
                loadDataSources();
            }
            
            @Override
            public void setActive(String name) throws Exception
            {
                DataSourceManager.getInstance().setActiveDatasource(name);
            }
        });
    }
    
    @Override
    public Lookup getLookup()
    {
        return mLookup;
    }
    
    public Collection<IDataSource> getDataSources()
    {
        return mDataSourceList;
    }
    
    private void loadDataSources()
    {
        mDataSourceList = DataSourceManager.getInstance().getDatasources();
    }
}
