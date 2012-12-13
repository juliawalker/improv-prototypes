/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.datasource.ui;

import gov.pnnl.improv.data.IDataSource;


/**
 *
 * @author D3X924
 */
public final class DataSourceEvent
{
    private IDataSource mDataSource;
    private String mDataSourceName;

    public DataSourceEvent()
    {
    }

    public IDataSource getDataSource()
    {
        return mDataSource;
    }

    public void setDataSource(IDataSource aValue)
    {
        mDataSource = aValue;
        if (mDataSource != null)
        {
            mDataSourceName = mDataSource.getName();
        }
    }

    public String getDataSourceName()
    {
        return mDataSourceName;
    }

    public void setDataSourceName(String aValue)
    {
        mDataSourceName = aValue;
    }

    @Override
    public String toString()
    {
        if (mDataSource == null)
        {
            return (mDataSourceName != null) ? mDataSourceName : "";
        }
        
        return mDataSource.getName();
    }

}
