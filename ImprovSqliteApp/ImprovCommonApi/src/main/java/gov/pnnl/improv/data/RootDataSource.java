package gov.pnnl.improv.data;

import java.util.ArrayList;
import java.util.List;


public class RootDataSource
{
    public static final RootDataSource ROOT = new RootDataSource();

    private List<DataSourceType> mDataSourceTypes;

    public RootDataSource()
    {
        this.mDataSourceTypes = new ArrayList<DataSourceType>();
    }

    public String getName()
    {
        return "Datasources";
    }

    public void addDataSourceType(DataSourceType dst)
    {
        if (!this.mDataSourceTypes.contains(dst))
        {
            this.mDataSourceTypes.add(dst);
        }
    }

    public void removeDataSourceType(DataSourceType dst)
    {
        if (this.mDataSourceTypes.contains(dst))
        {
            this.mDataSourceTypes.remove(dst);
        }
    }

    public List<DataSourceType> getDataSourceTypes()
    {
        return this.mDataSourceTypes;
    }

}
