package gov.pnnl.improv.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import prefuse.data.Table;
import prefuse.data.io.sql.ConnectionFactory;
import prefuse.data.io.sql.DatabaseDataSource;


public class SQLiteDatabaseConnection implements IDatabaseConnection
{
    private DatabaseDataSource mDatabase;
    private Connection mConnection;
    

    public void connect(String dbFilePathOrUri) throws Exception
    {
        if (mConnection == null || mConnection.isClosed())
        {
            Class.forName("org.sqlite.JDBC");
            // jdbc:sqlite:filepath for file system
            // jdbc:sqlite::resource:uri
            mConnection =
                    DriverManager.getConnection("jdbc:sqlite::resource:" + dbFilePathOrUri);
        }

        mDatabase = ConnectionFactory.getDatabaseConnection(mConnection);
    }

    public void connect(String dbFilePathOrUri, String username, String password)
            throws Exception
    {
        if (mConnection == null || mConnection.isClosed())
        {
            Class.forName("org.sqlite.JDBC");
            if (username == null || password == null)
            {
                connect(dbFilePathOrUri);
            } else
            {
                mConnection = DriverManager.getConnection("jdbc:sqlite::resource:" + dbFilePathOrUri,
                        username, password);
                mDatabase = ConnectionFactory.getDatabaseConnection(mConnection);
            }
        }
    }

    public void disconnect() throws Exception
    {
        mDatabase = null;
        mConnection.close();
    }

    public void begin() throws Exception
    {
        if (mConnection != null)
        {
            mConnection.setAutoCommit(false);
        }
    }

    public void commit() throws Exception
    {
        if (mConnection != null)
        {
            mConnection.commit();
            mConnection.setAutoCommit(true);
        }
    }

    public synchronized Table getData(String query) throws Exception
    {
        return mDatabase.getData(query);
    }

    public synchronized void loadData(Table t, String query) throws Exception
    {
        mDatabase.loadData(t, query);
    }

    public synchronized void rawSql(String sql) throws SQLException
    {
        Statement st = mConnection.createStatement();
        st.execute(sql);
        st.close();
    }

    public synchronized Object[] rawSqlSelect(String sql) throws SQLException
    {
        Statement st = mConnection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        try
        {
            List<Object[]> results = new ArrayList<Object[]>();
            int colCount = rs.getMetaData().getColumnCount();
            Set<Integer> intCols = new HashSet<Integer>();
            Set<Integer> realCols = new HashSet<Integer>();
            Set<Integer> strCols = new HashSet<Integer>();
            for (int i = 1; i <= colCount; i++)
            {
                int colType = rs.getMetaData().getColumnType(i);
                if (colType == Types.INTEGER)
                {
                    intCols.add(i);
                } else if (colType == Types.REAL)
                {
                    realCols.add(i);
                } else
                {
                    strCols.add(i);
                }
            }
            while (rs.next())
            {
                Object[] rowData = new Object[colCount];
                for (Integer intCol : intCols)
                {
                    rowData[intCol - 1] = new Integer(rs.getInt(intCol));
                }
                for (Integer realCol : realCols)
                {
                    rowData[realCol - 1] = new Integer(rs.getInt(realCol));
                }
                for (Integer strCol : strCols)
                {
                    rowData[strCol - 1] = rs.getString(strCol);
                }
                results.add(rowData);
            }
            rs.close();
            st.close();
            return results.toArray();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized void preparedStatement(String sql, Object[][] rows) throws SQLException
    {
        PreparedStatement st = mConnection.prepareStatement(sql);
        for (int i = 0; i < rows.length; i++)
        {
            for (int j = 0; j < rows[i].length; j++)
            {
                Object v = rows[i][j];
                if (v instanceof Integer)
                {
                    st.setInt(j + 1, ((Integer) v).intValue());
                } else
                {
                    st.setString(j + 1, (String) v);
                }
            }
            st.addBatch();
        }
        mConnection.setAutoCommit(false);
        st.executeBatch();
        st.close();
        mConnection.commit();
        mConnection.setAutoCommit(true);
        System.out.println(sql);
    }

    public synchronized String getTableSchema(String table) throws Exception
    {
        Object[] rs = rawSqlSelect(SQLiteQueryFactory.getSchemaOfTable(table));
        return ((Object[]) rs[0])[0].toString();
    }

    public synchronized Map<String, Map<String, String>> getTables() throws Exception
    {
        Map<String, Map<String, String>> tables = new HashMap<String, Map<String, String>>();
        rawSql(SQLiteQueryFactory.caseSensitivityPragma(true));
        ResultSet rs = mConnection.getMetaData().getTables(null, null, "%", new String[]
                {
                    "TABLE"
                });
        while (rs.next())
        {
            String tableName = rs.getString("TABLE_NAME");
            Map<String, String> columns = new HashMap<String, String>();
            ResultSet rsCols = mConnection.getMetaData().getColumns(null, null, tableName, null);
            while (rsCols.next())
            {
                columns.put(rsCols.getString("COLUMN_NAME"), rsCols.getString("TYPE_NAME"));
            }
            rsCols.close();
            tables.put(tableName, columns);
        }
        rs.close();
        return tables;
    }

}
