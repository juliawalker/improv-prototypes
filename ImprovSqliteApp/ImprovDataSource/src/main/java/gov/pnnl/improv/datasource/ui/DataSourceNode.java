/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.datasource.ui;

import gov.pnnl.improv.data.IDataSource;
import java.awt.Image;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.InstanceContent;


/**
 *
 * @author D3X924
 */
public class DataSourceNode extends AbstractNode
{
    private IReloadableDataSource mDataSourceActions;
    private DataSourceEvent mDataSource;
    private Lookup mLookup;
    
    public DataSourceNode(IReloadableDataSource dsactions, DataSourceEvent ds, Lookup lookup)
    {
        super(Children.LEAF, lookup);
        mDataSourceActions = dsactions;
        mLookup = lookup;
        mDataSource = ds;

        if (ds != null)
        {
            setDisplayName(ds.getDataSourceName());
        }
    }

    @Override
    public String getHtmlDisplayName()
    {
        if (mDataSource == null)
        {
            return null;
        }

        StringBuilder temp = new StringBuilder();

        if (mDataSource.getDataSource().isActive())
        {
            temp.append("<font color='#0A43C7'>");
            temp.append("<b>")
                .append(mDataSource.getDataSourceName())
                .append("</b>");
        }
        else
        {
            temp.append("<font color='#777777'>");
            temp.append(mDataSource.getDataSourceName());
        }

        temp.append("</font>");
        return temp.toString();
    }
    
    @Override
    public Image getIcon(int type)
    {
        return ImageUtilities.loadImage("gov/pnnl/improv/datasource/ui/database.png");
    }
    
    @Override
    public Action[] getActions(boolean popup)
    {
        return mDataSource.getDataSource().isActive()
            ? null
            : new Action[] {
              new ActivateDataSourceAction(mDataSource.getDataSourceName(),
              mLookup, mDataSourceActions)
            };
    }

    public IDataSource getDataSource()
    {
        return mDataSource != null ? mDataSource.getDataSource() : null;
    }
}
