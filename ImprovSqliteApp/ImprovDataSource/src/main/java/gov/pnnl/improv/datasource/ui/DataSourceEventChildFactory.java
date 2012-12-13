/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.datasource.ui;

import gov.pnnl.improv.data.IDataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.openide.util.Lookup;


/**
 *
 * @author D3X924
 */
public class DataSourceEventChildFactory extends ChildFactory<DataSourceEvent>
{
    private DataSourceQuery mQuery;
    private Lookup mLookup;

    public DataSourceEventChildFactory(DataSourceQuery aQuery, Lookup lookup)
    {
        mQuery = aQuery;
        mLookup = lookup;
    }

    @Override
    protected boolean createKeys(List toPopulate)
    {
        Collection<IDataSource> dslist = mQuery.getDataSources();
        final int len = dslist.size();
        Collection<DataSourceEvent> items = new ArrayList<DataSourceEvent>(len);
        
        for (IDataSource ds : dslist)
        {
            DataSourceEvent ev = new DataSourceEvent();
            ev.setDataSource(ds);
            items.add(ev);
        }

        toPopulate.addAll(items);
        return true;
    }
    

    @Override
    protected Node createNodeForKey(DataSourceEvent key)
    {
        IReloadableDataSource source =
                mQuery.getLookup().lookup(IReloadableDataSource.class);
        Node node = new DataSourceNode(source, key, mLookup);
        return node;
    }

    public void updateNodes()
    {
        refresh(true);
    }
}
