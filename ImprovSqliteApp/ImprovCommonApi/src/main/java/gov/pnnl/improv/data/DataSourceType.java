package gov.pnnl.improv.data;

import gov.pnnl.improv.data.IDataSource;
import gov.pnnl.improv.data.RootDataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class DataSourceType
{
    private static final String SQLITE_DESC = "This datasource requires at least two tables for the TreeMap - a nodes table and an edges table";
    private static final String FILEBASED_DESC = "This datasource requires a prefuse XML file for the TreeMap";
    private static final String PUBLIC_DESC = "This datasource is a public SQLite datasource";
    private static final String LOCAL_DESC = "This datasource is a local SQLite datasource";

    public static DataSourceType SQLITE = new DataSourceType(RootDataSource.ROOT, "SQLite", SQLITE_DESC);
    public static DataSourceType FILEBASED = new DataSourceType(RootDataSource.ROOT, "File Based", FILEBASED_DESC);
    public static DataSourceType PUBLIC = new DataSourceType(RootDataSource.ROOT, "Public", PUBLIC_DESC);
    public static DataSourceType LOCAL = new DataSourceType(RootDataSource.ROOT, "Local", LOCAL_DESC);

    private String mName;
    private String mDescription;
    private List<IDataSource> mDatasources;
    private RootDataSource mRoot;
    private boolean mIsSorted;

    public DataSourceType(RootDataSource root, String name, String description)
    {
        this.mName = name;
        this.mDescription = description;
        this.mRoot = root;
        this.mDatasources = new ArrayList<IDataSource>();
    }

    public String getName()
    {
        return this.mName;
    }

    public String getDescription()
    {
        return this.mDescription;
    }

    public RootDataSource getRoot()
    {
        return this.mRoot;
    }

    public void addDatasource(IDataSource ds)
    {
        if (!this.mDatasources.contains(ds))
        {
            this.mDatasources.add(ds);
            this.mIsSorted = false;
        }
    }

    public void removeDatasource(IDataSource ds)
    {
        if (this.mDatasources.contains(ds))
        {
            this.mDatasources.remove(ds);
        }
    }

    public List<IDataSource> getDatasources()
    {
        if (!this.mIsSorted)
        {
            Collections.sort(this.mDatasources, new Comparator<IDataSource>()
            {
                @Override
                public int compare(IDataSource o1, IDataSource o2)
                {
                    return o1.getName().compareTo(o2.getName());
                }

            });
        }
        return this.mDatasources;
    }

}
