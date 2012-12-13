package gov.pnnl.improv.datasource.ui;

import javax.swing.Action;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.InstanceContent;


/**
 *
 * @author D3X924
 */
public class RootDataSourceNode extends AbstractNode
{
    private static final String NODE_NAME = "Data Sources";
    private DataSourceQuery mDataSourceQuery;
    private InstanceContent mContent;
    private Lookup mLookup;
    private DataSourceEventChildFactory mChildFactory;
    private ExplorerManager mExplorer;

    public RootDataSourceNode(DataSourceQuery query, InstanceContent content,
            Lookup lookup, DataSourceEventChildFactory factory)
    {
        super(Children.create(new DataSourceEventChildFactory(query, lookup),
                true), lookup);
        
        mDataSourceQuery = query;
        mContent = content;
        mLookup = lookup;
        mChildFactory = factory;

        mContent.add(new IReloadableView() {
            @Override
            public void reloadChildren() throws Exception
            {
                mChildFactory = new DataSourceEventChildFactory(mDataSourceQuery, mLookup);
                setChildren(Children.create(mChildFactory, false));
                mChildFactory.updateNodes();
                
                // Find the active data source so the nodes in the tree view
                // are expanded and highlited properly
                DataSourceNode selected = null;
                Node[] nodes = getChildren().getNodes();
                for (Node n : nodes)
                {
                    DataSourceNode n2 = (DataSourceNode)n;
                    if (n2 != null && n2.getDataSource().isActive())
                    {
                        selected = n2;
                    }
                }
                
                if (hasPropertyChangeListener() && selected != null)
                {
                    firePropertyChange("SetActiveDataSource", null, selected);
                }
            }
        });
    }
    
    @Override
    public String getDisplayName()
    {
        return NODE_NAME;
    }
    
    @Override
    public Action[] getActions(boolean context)
    {
        return null;
    }

}
