package gov.pnnl.improv.data;

import gov.pnnl.improv.data.DataSourceType;
import gov.pnnl.improv.db.SQLiteQueryFactory;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.Set;

import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;


public class TreeDBDataSource extends AbstractSQLiteDataSource
{

    private static final boolean VISIBILITY = true;
    private DataSourceType mType;
    private String mName;
    private String mLocation;
    private String mDescription;
    private String mNodeTable;
    private String mEdgeTable;
    private boolean mActive;
    private boolean mValid;
    private Tree mTree;

    public TreeDBDataSource(String name, String location, String description, DataSourceType type)
    {
        this.mName = name;
        this.mLocation = location;
        this.mDescription = description;
        this.mActive = false;
        this.mType = type;
        if (location.length() > 0)
        {
            try
            {
                File f = new File(location);
                if (f.isAbsolute() && !f.exists())
                {
                    mValid = false;
                } else
                {
                    connect(location);
                    mValid = true;
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public synchronized Tree getData(String nodeDBTable, String edgeDBTable)
    {
        if (this.mTree == null)
        {
            try
            {
                Table nodeTable = mConn.getData(SQLiteQueryFactory.nodes(nodeDBTable, VISIBILITY));
                Table edgeTable = mConn.getData(SQLiteQueryFactory.edges(edgeDBTable, VISIBILITY));
                return new Tree(nodeTable, edgeTable, "id", "source", "target");
            } catch (Exception e)
            {
                return getData();
            }
        }
        return this.mTree;
    }

    public synchronized Tree getData()
    {
        return getData(this.mNodeTable, this.mEdgeTable);
    }

    public synchronized Tree getTreeViewData(int[] nodes, int[][] edges)
    {//Set<Node> nodes, Set<Edge> edges) {
        try
        {
            // create node table
            Table nodeTable = new Table();
            nodeTable.addColumn("node_id", int.class);
            nodeTable.addColumn("name", String.class);
//			nodeTable.addRows(nodes.size());
            nodeTable.addRows(nodes.length);
            int nid = 0;
//			for (Node n: nodes) {
            for (int id : nodes)
            {
//				int id = n.getInt("id");
                nodeTable.set(nid, "node_id", id);
                Object[] rows = rawSqlSelect(SQLiteQueryFactory.select(mNodeTable, new String[]
                        {
                            "name"
                        }, new int[]
                        {
                            id
                        }));
                Object[] row = (Object[]) rows[0];
                nodeTable.set(nid, "name", row[0]);
                nid++;
            }
            // create edge table
            Table edgeTable = new Table();
            edgeTable.addColumn("edge_id", int.class);
            edgeTable.addColumn("source", int.class);
            edgeTable.addColumn("target", int.class);
//			edgeTable.addRows(edges.size());
            edgeTable.addRows(edges.length);
            int eid = 0;
//			for (Edge e: edges) {
            for (int i = 0; i < edges.length; i++)
            {
                edgeTable.set(eid, "edge_id", edges[i][0]);//e.getInt("id"));
                edgeTable.set(eid, "source", edges[i][1]);//e.getInt("source"));
                edgeTable.set(eid, "target", edges[i][2]);// e.getInt("target"));
                eid++;
            }
            // now create and return the tree
            return new Tree(nodeTable, edgeTable, "node_id", "source", "target");
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized Tree getSpecificData(String nodeDBTable, String edgeDBTable)
    { //, int[] ids, int[] eIds) {
        if (this.mTree == null)
        {
            try
            {
                Table nodeTable = mConn.getData("SELECT id, name FROM " + nodeDBTable + "_tree");//select(nodeDBTable + "_tree", new String[] { "id, name" }, ids));
                Table edgeTable = mConn.getData("SELECT id, source, target FROM " + edgeDBTable + "_tree");//SQLiteQueryFactory.select(edgeDBTable + "_tree", new String[] { "*" }, eIds));
                return new Tree(nodeTable, edgeTable, "id", "source", "target");
            } catch (Exception e)
            {
                return getData();
            }
        }
        return this.mTree;
    }

    public synchronized Tree getSpecificData()
    { //int[] ids, int[] eIds) {
        return getSpecificData(this.mNodeTable, this.mEdgeTable);//, ids, eIds);
    }

    public void clearCache()
    {
        this.mTree = null;
    }

    public Table getNodeTableData()
    {
        try
        {
            Table nodeTable = mConn.getData(SQLiteQueryFactory.nodes(this.mNodeTable, VISIBILITY));
            return nodeTable;
        } catch (Exception e1)
        {
            e1.printStackTrace();
        }
        return null;
    }

    public Table getEdgeTableData()
    {
        try
        {
            Table edgeTable = mConn.getData(SQLiteQueryFactory.edges(this.mEdgeTable, VISIBILITY));
            return edgeTable;
        } catch (Exception e1)
        {
            e1.printStackTrace();
        }
        return null;
    }

    @Override
    public String getLocation()
    {
        return this.mLocation;
    }

    @Override
    public String getName()
    {
        return this.mName;
    }

    @Override
    public void setLocation(String newLocation)
    {
        this.mPropertyChangeSupport.firePropertyChange("location",
                this.mLocation, this.mLocation = newLocation);
        this.connect(newLocation);
        this.mValid = true;
    }

    @Override
    public void setName(String newName)
    {
        this.mPropertyChangeSupport.firePropertyChange("name", this.mName,
                this.mName = newName);
    }

    @Override
    public String getEdgeTable()
    {
        return this.mEdgeTable;
    }

    @Override
    public String getNodeTable()
    {
        return this.mNodeTable;
    }

    @Override
    public void setEdgeTable(String newEdgeTable)
    {
        this.mPropertyChangeSupport.firePropertyChange("edgeTable",
                this.mEdgeTable, this.mEdgeTable = newEdgeTable);
    }

    @Override
    public void setNodeTable(String newNodeTable)
    {
        this.mPropertyChangeSupport.firePropertyChange("nodeTable",
                this.mNodeTable, this.mNodeTable = newNodeTable);
    }

    public void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener)
    {
        this.mPropertyChangeSupport.addPropertyChangeListener(propertyName,
                listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        this.mPropertyChangeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public String toString()
    {
        return this.mName + ":" + this.mLocation;
    }

    @Override
    public String getDescription()
    {
        return this.mDescription;
    }

    @Override
    public void setDescription(String newDesc)
    {
        this.mPropertyChangeSupport.firePropertyChange("description",
                this.mDescription, this.mDescription = newDesc);
    }

    @Override
    public boolean isActive()
    {
        return this.mActive;
    }

    @Override
    public void setActive(boolean active)
    {
        this.mActive = active;
    }

    @Override
    public DataSourceType getType()
    {
        return this.mType;
    }

    @Override
    public void setType(DataSourceType newType)
    {
        this.mType = newType;
    }

    @Override
    public boolean isValid()
    {
        return this.mValid;
    }

}
