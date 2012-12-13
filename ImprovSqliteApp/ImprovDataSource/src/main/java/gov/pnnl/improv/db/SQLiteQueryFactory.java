package gov.pnnl.improv.db;


public class SQLiteQueryFactory
{

    public static synchronized String createAttach(String dbToAttach)
    {
        return "ATTACH DATABASE \'" + dbToAttach + "\' AS temp2";
    }

    public static synchronized String createDetach()
    {
        return "DETACH DATABASE temp2";
    }

    public static synchronized String dropTable(String name)
    {
        return "DROP TABLE IF EXISTS " + name;
    }

    public static synchronized String createTempMinimalNodeTable(String name)
    {
        return "CREATE TEMP TABLE " + name + "(id INTEGER, name)";
    }

    public static synchronized String createTempMinimalEdgeTable(String name)
    {
        return "CREATE TEMP TABLE " + name + "(id INTEGER, source INTEGER, target INTEGER)";
    }

    public static synchronized String insertTempNodeTreeTable(String name, String originalTable)
    {
        return "INSERT INTO " + name + " VALUES(?,?)";//" SELECT id, name FROM " + originalTable + " WHERE id=?";
    }

    public static synchronized String insertTempEdgeTreeTable(String name, String originalTable, String tempNodeTable)
    {
        return "INSERT INTO " + name + " (id, source, target) "
                + "SELECT " + originalTable + ".id, " + originalTable + ".source, "
                + originalTable + ".target FROM " + tempNodeTable
                + " JOIN " + originalTable + " ON " + originalTable + ".source = " + tempNodeTable + ".id";
    }

    public static synchronized String getSchemaOfTable(String table)
    {
        return "SELECT sql FROM sqlite_master WHERE tbl_name=\'" + table + "\'";
    }

    public static synchronized String insertNodeTableTemp(String fromNodeTable)
    {
        return "INSERT INTO " + fromNodeTable + " SELECT * FROM temp2." + fromNodeTable;
    }

    public static synchronized String insertEdgeTableTemp(String fromEdgeTable)
    {
        return "INSERT INTO " + fromEdgeTable + " SELECT * FROM temp2." + fromEdgeTable;
    }

    public static synchronized String deleteId(String table)
    {
        return "DELETE FROM " + table + " WHERE id=?";
    }

    public static synchronized String deleteNodeFromEdge(String edgeTable)
    {
        return "DELETE FROM " + edgeTable + " WHERE source=? OR target=?";
    }

    public static synchronized String deleteEdge(String edgeTable, String column)
    {
        return "DELETE FROM " + edgeTable + " WHERE " + column + "=?";
    }

    public static synchronized String getRowCount(String tableName)
    {
        return "SELECT count(*) AS rowcount FROM " + tableName;
    }

    public static synchronized String idExists(String tableName, int id)
    {
        return "SELECT count(id) AS rowcount FROM " + tableName + " WHERE id="
                + id;
    }

    public static synchronized String addColumn(String column, String table, String type)
    {
        return "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type + " DEFAULT 1";
    }

    public static synchronized String setNodeVisibility(String table)
    {
        return "UPDATE " + table + " SET visible=? WHERE id=?";
    }

    public static synchronized String setEdgeVisibility(String table)
    {
        return "UPDATE " + table + " SET visible=? WHERE id=?";
    }

    public static synchronized String addValue(String table, String column)
    {
        return "UPDATE " + table + " SET " + column + " = ? WHERE id=?";
    }

    public static synchronized String search(String table, String[] columns, String text, boolean regex)
    {
        String type = regex ? "REGEXP" : "LIKE";
        String like = " " + type + " '%" + text + "%'";
        String where = "";
        String cols = "";
        boolean first = true;
        for (String col : columns)
        {
//			if (!col.equals("id")) {							
            if (first)
            {
                first = false;
            } else
            {
                cols += ",";
                where += " OR ";
            }
            cols += col;
            where += col + like;
//			}
        }
        return "SELECT " + cols + " FROM " + table + " WHERE visible = 1 AND (" + where + ") ORDER BY id";
    }

    public static synchronized String select(String table, String[] columns, int[] ids)
    {
        String cols = "";
        boolean first = true;
        for (String col : columns)
        {
            if (first)
            {
                first = false;
            } else
            {
                cols += ",";
            }
            cols += col;
        }
        String where = "";
        first = true;
        for (int id : ids)
        {
            if (first)
            {
                first = false;
            } else
            {
                where += " OR ";
            }
            where += "id=" + id;
        }
        return "SELECT " + cols + " FROM " + table + " WHERE visible = 1 AND " + where + " ORDER BY id";
    }

    public static synchronized String selectEdges(String table, int id)
    {
        return "SELECT id FROM " + table + " WHERE target = " + id;
    }

    public static synchronized String selectSourceEdges(String table, int[] ids)
    {
        return selectEdges(table, "target", ids);
    }

    public static synchronized String selectTargetEdges(String table, int[] ids)
    {
        return selectEdges(table, "source", ids);
    }

    private static synchronized String selectEdges(String table, String type, int[] ids)
    {
        String where = "";
        boolean first = true;
        for (int id : ids)
        {
            if (first)
            {
                first = false;
            } else
            {
                where += " OR ";
            }
            where += type + "=" + id;
        }
        return "SELECT id FROM " + table + " WHERE " + where;
    }

    public static synchronized String caseSensitivityPragma(
            boolean caseInsensitive)
    {
        return "PRAGMA case_sensitive_like = " + !caseInsensitive;
    }

    public static synchronized String nodes(String nodeTable, boolean visible)
    {
        String vis = visible ? " WHERE visible=1" : "";
//		return "SELECT id, scancounts, mod_count, uniquepeptides, cluster_coverage, protein_count FROM "
//				+ nodeTable + vis;
        return "SELECT id FROM " + nodeTable + vis;
    }

    public static synchronized String edges(String edgeTable, boolean visible)
    {
        String vis = visible ? " WHERE visible=1" : "";
        return "SELECT id, source, target FROM " + edgeTable + vis;
    }

    public static synchronized String proteins(String nodeTable)
    {
        return "SELECT id FROM " + nodeTable + " WHERE entity='protein'";
    }

    public static synchronized String peptides(String nodeTable)
    {
        return "SELECT id FROM " + nodeTable + " WHERE entity='peptide'";
    }

    public static synchronized String getClusterCoverage(String nodeTable, String edgeTable, String scoreField, int id)
    {
        return "SELECT count(target) FROM " + edgeTable + " JOIN " + nodeTable + " "
                + "ON " + edgeTable + ".target = " + nodeTable + ".id "
                + "WHERE " + edgeTable + ".source = " + id + " AND " + nodeTable + "." + scoreField + " > 0";
    }

    public static synchronized String count(String table, String column, String filterColumn, Object filterValue)
    {
        String val = filterValue.toString();
        if (filterValue instanceof String)
        {
            val = "'" + val + "'";
        }
        return "SELECT count(DISTINCT " + column + ") as countVal FROM " + table + " WHERE " + filterColumn + "=" + val + " AND id > 0";
    }

    public static synchronized String top5(String table, String column)
    {
        return "SELECT name FROM " + table + " WHERE ";
    }

}
