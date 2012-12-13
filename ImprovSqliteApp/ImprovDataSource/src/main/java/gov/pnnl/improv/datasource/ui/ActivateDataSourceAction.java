/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.datasource.ui;

import gov.pnnl.improv.data.DataSourceManager;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;


/**
 *
 * @author D3X924
 */
public class ActivateDataSourceAction extends AbstractAction
{
    private static final long serialVersionUID = -1262525608511637603L;
    
    private IReloadableDataSource mDsActions;
    private IReloadableView mReloadableView;
    private String mSourceName;

    public ActivateDataSourceAction(String aName, Lookup aLookup,
            IReloadableDataSource source)
    {
        mSourceName = aName;
        putValue(NAME, "Set Active");
        
        mReloadableView = aLookup.lookup(IReloadableView.class);
        mDsActions = source;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        try
        {
            if (mDsActions != null)
            {
                mDsActions.setActive(mSourceName);
            }

            if (mReloadableView != null)
            {
                mReloadableView.reloadChildren();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

}
