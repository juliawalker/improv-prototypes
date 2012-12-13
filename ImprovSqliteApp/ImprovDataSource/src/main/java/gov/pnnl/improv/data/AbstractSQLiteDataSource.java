package gov.pnnl.improv.data;

import gov.pnnl.improv.data.DataSourceType;
import gov.pnnl.improv.data.IDataSource;
import gov.pnnl.improv.db.SQLiteDatabaseConnection;
import gov.pnnl.improv.db.SQLiteQueryFactory;

import java.beans.PropertyChangeSupport;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import prefuse.data.Table;


public abstract class AbstractSQLiteDataSource implements IDataSource
{
    public static final boolean VISIBILITY = true;

    protected SQLiteDatabaseConnection mConn;
    protected PropertyChangeSupport mPropertyChangeSupport = new PropertyChangeSupport(this);

    public void connect(String location)
    {
        try
        {
            this.mConn = new SQLiteDatabaseConnection();
            this.mConn.connect(location, null, null);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void disconnect()
    {
        try
        {
            this.mConn.disconnect();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public Table getTable(String query)
    {
        Table data = null;
        try
        {
            data = mConn.getData(query);
        } catch (Exception e1)
        {
            e1.printStackTrace();
        }
        return data;
    }

    public void rawSql(String sql)
    {
        try
        {
            System.out.println(sql);
            this.mConn.rawSql(sql);
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public Object getSingleValue(String sql)
    {
        Object val = null;
        try
        {
            Object[] rs = this.mConn.rawSqlSelect(sql);
            if (rs != null)
            {
                return ((Object[]) rs[0])[0];
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return val;
    }

    public int getSingleIntValue(String sql)
    {
        int val = 0;
        try
        {
            Object[] rs = this.mConn.rawSqlSelect(sql);
            if (rs != null)
            {
                val = (Integer) ((Object[]) rs[0])[0];
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return val;
    }

    public double getSingleDoubleValue(String sql)
    {
        double val = 0;
        try
        {
            Object[] rs = this.mConn.rawSqlSelect(sql);
            if (rs != null)
            {
                val = Double.parseDouble(((Object[]) rs[0])[0].toString());
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return val;
    }

    public Object[] rawSqlSelect(String sql)
    {
        try
        {
            return this.mConn.rawSqlSelect(sql);
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public void preparedStatement(String sql, Object[][] rows)
    {
        try
        {
            this.mConn.preparedStatement(sql, rows);
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public int getCount(String table)
    {
        int count = 0;
        try
        {
            Object[] rs = this.mConn.rawSqlSelect(SQLiteQueryFactory.getRowCount(table));
            count = (Integer) ((Object[]) rs[0])[0];
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return count;
    }

    public boolean idExists(String table, int id)
    {
        boolean exists = false;
        try
        {
            Object[] rs = this.mConn.rawSqlSelect(SQLiteQueryFactory.idExists(table, id));
            exists = (Integer) ((Object[]) rs[0])[0] > 0;
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return exists;
    }

    public List<String> getColumnsForTable(String table)
    {
        try
        {
            Map<String, Map<String, String>> tables = this.mConn.getTables();
            if (tables.containsKey(table))
            {
                List<String> columns = new ArrayList<String>(tables.get(table).keySet());
                Collections.sort(columns);
                return columns;
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, String> getColumnAndTypesForTable(String table)
    {
        try
        {
            Map<String, Map<String, String>> tables = this.mConn.getTables();
            if (tables.containsKey(table))
            {
                return tables.get(table);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public Set<String> getTables()
    {
        try
        {
            return this.mConn.getTables().keySet();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public String getTableSchema(String table)
    {
        try
        {
            return this.mConn.getTableSchema(table);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    abstract public String getLocation();

    @Override
    abstract public String getName();

    @Override
    abstract public void setLocation(String location);

    @Override
    abstract public void setName(String newName);

    @Override
    abstract public void setType(DataSourceType newType);

    @Override
    abstract public DataSourceType getType();

    abstract public void setNodeTable(String newNodeTable);

    abstract public void setEdgeTable(String newEdgeTable);

    abstract public String getNodeTable();

    abstract public String getEdgeTable();

}
