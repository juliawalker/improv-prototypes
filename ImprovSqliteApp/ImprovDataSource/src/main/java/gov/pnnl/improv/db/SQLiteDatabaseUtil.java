package gov.pnnl.improv.db;

import java.util.Map;


/**
 * Use this utility class to create databases from files
 *
 * @author Kelly Domico (d3p056) Pacific Northwest National Laboratory
 *
 */
public class SQLiteDatabaseUtil
{

    public static void createPrefuseTable(String newSchemaDbLocation) throws Exception
    {
        SQLiteDatabaseConnection conn = new SQLiteDatabaseConnection();
        conn.connect(newSchemaDbLocation, null, null);
        createPrefuseTable(conn);
        conn.disconnect();
    }

    public static void createPrefuseTable(SQLiteDatabaseConnection conn) throws Exception
    {
        if (!isValidFormat(conn))
        {
            // grab the column names from the nodes table
            String extraColumns = "";
            String extraColumnNames = "";
            Map<String, String> peptidesColumns = conn.getTables().get("peptides");
            peptidesColumns.remove("id");
            peptidesColumns.remove("node_id");
            peptidesColumns.remove("protein_id");
            peptidesColumns.remove("sequence");
            peptidesColumns.remove("mod_count");
            peptidesColumns.remove("scan_count");
            for (String column : peptidesColumns.keySet())
            {
                extraColumns += ", " + column + " " + peptidesColumns.get(column);
                if (peptidesColumns.get(column).equalsIgnoreCase("integer") || peptidesColumns.get(column).equalsIgnoreCase("real"))
                {
                    extraColumns += " DEFAULT 0";
                }
                extraColumnNames += ", " + column;
            }

            String createNodeTable = "CREATE TABLE nodes (id INTEGER PRIMARY KEY, nid INTEGER, name, entity, unique_peptides INTEGER DEFAULT 0, "
                    + "protein_count INTEGER DEFAULT 0, cluster_name, amino_acid_coverage INTEGER DEFAULT 0, go_function_id, go_function_name, gi_number, "
                    + "amino_acids_composition INTEGER DEFAULT 0, sequence, mod_count INTEGER DEFAULT 0, scan_count INTEGER DEFAULT 0" + extraColumns + ", visible INTEGER DEFAULT 1)";
            String createEdgeTable = "CREATE TABLE edges (id INTEGER PRIMARY KEY, source INTEGER, target INTEGER, visible INTEGER DEFAULT 1)";
            String insertRoot = "INSERT INTO nodes (id, nid, name, entity) VALUES(0, 0, 'root', 'root')";
            conn.rawSql(createNodeTable);
            conn.rawSql(insertRoot);
            conn.rawSql(createEdgeTable);

            System.out.println("Done creating tables");

            // add index
            String nodesIndex = "CREATE INDEX nid_index ON nodes (nid)";
            conn.rawSql(nodesIndex);
            System.out.println("Done creating indexes");

            int lastEdgeId = 0;
            String insertClusters = "INSERT INTO nodes (nid, name, entity, cluster_name, amino_acid_coverage) "
                    + "SELECT id AS nid, cluster_name AS name, 'cluster', cluster_name, amino_acid_coverage FROM clusters";
            conn.rawSql(insertClusters);
            String insertRootClusterEdges = "INSERT INTO edges (source, target) SELECT 0, nodes.id FROM clusters "
                    + "JOIN nodes ON clusters.id = nodes.nid WHERE nodes.entity='cluster'";
            conn.rawSql(insertRootClusterEdges);
            lastEdgeId = Integer.parseInt(((Object[]) conn.rawSqlSelect("SELECT max(id) FROM edges")[0])[0].toString());

            System.out.println("Done inserting clusters - lastEdgeId = " + lastEdgeId);

            String insertProteins = "INSERT INTO nodes (nid, name, entity, go_function_id, go_function_name,"
                    + "gi_number, amino_acids_composition, protein_count) SELECT id AS nid, go_function_id AS name, 'protein', go_function_id, "
                    + "go_function_name, gi_number, amino_acids_composition, 1 FROM proteins";
            conn.rawSql(insertProteins);
            String insertClusterProteinEdges = "INSERT INTO edges (source, target) SELECT proteins.cluster_id, nodes.id FROM proteins "
                    + "JOIN nodes ON proteins.id = nodes.nid WHERE nodes.entity='protein'";
            conn.rawSql(insertClusterProteinEdges);
            Object[] proteinRows = conn.rawSqlSelect("SELECT edges.id, nodes.id FROM edges JOIN nodes ON edges.source = nodes.nid WHERE nodes.entity='cluster' AND edges.id > " + lastEdgeId);
            conn.begin();
            for (int i = 0; i < proteinRows.length; i++)
            {
                Object[] values = (Object[]) proteinRows[i];
                conn.rawSql("UPDATE edges SET source = " + values[1] + " WHERE id = " + values[0]);
            }
            conn.commit();
            lastEdgeId = Integer.parseInt(((Object[]) conn.rawSqlSelect("SELECT max(id) FROM edges")[0])[0].toString());

            System.out.println("Done inserting proteins - lastEdgeId = " + lastEdgeId);

            String insertPeptides = "INSERT INTO nodes (nid, name, entity, sequence, mod_count, scan_count, unique_peptides, protein_count" + extraColumnNames + ") "
                    + "SELECT id AS nid, sequence AS name, 'peptide', sequence, mod_count, scan_count, 1, 0" + extraColumnNames + " FROM peptides";
            conn.rawSql(insertPeptides);
            String insertProteinPeptideEdges = "INSERT INTO edges (source, target) SELECT peptides.protein_id, nodes.id FROM peptides "
                    + "JOIN nodes ON peptides.id = nodes.nid WHERE nodes.entity='peptide'";
            conn.rawSql(insertProteinPeptideEdges);
            Object[] peptideRows = conn.rawSqlSelect("SELECT edges.id, nodes.id FROM edges JOIN nodes ON edges.source = nodes.nid WHERE nodes.entity='protein' AND edges.id > " + lastEdgeId);
            conn.begin();
            for (int i = 0; i < peptideRows.length; i++)
            {
                Object[] values = (Object[]) peptideRows[i];
                conn.rawSql("UPDATE edges SET source = " + values[1] + " WHERE id = " + values[0]);
            }
            conn.commit();
            lastEdgeId = Integer.parseInt(((Object[]) conn.rawSqlSelect("SELECT max(id) FROM edges")[0])[0].toString());

            System.out.println("Done inserting peptides - lastEdgeId = " + lastEdgeId);

            Object[] upProteinRows = conn.rawSqlSelect("SELECT proteins.id, count(DISTINCT peptides.sequence) FROM proteins JOIN peptides ON peptides.protein_id = proteins.id GROUP BY proteins.id");
            conn.begin();
            for (int i = 0; i < upProteinRows.length; i++)
            {
                Object[] values = (Object[]) upProteinRows[i];
                conn.rawSql("UPDATE nodes SET unique_peptides = " + values[1] + " WHERE nid = " + values[0] + " AND entity='protein'");
            }
            conn.commit();

            System.out.println("Done updating protein unique peptides");

            Object[] pcClusterRows = conn.rawSqlSelect("SELECT clusters.id, count(DISTINCT proteins.go_function_id) FROM clusters JOIN proteins ON proteins.cluster_id = clusters.id GROUP BY clusters.id");
            conn.begin();
            for (int i = 0; i < pcClusterRows.length; i++)
            {
                Object[] values = (Object[]) pcClusterRows[i];
                conn.rawSql("UPDATE nodes SET protein_count = " + values[1] + " WHERE nid = " + values[0] + " AND entity='cluster'");
            }
            conn.commit();

            System.out.println("Done updating cluster protein count");

            Object[] upClusterRows = conn.rawSqlSelect("SELECT clusters.id, count(DISTINCT peptides.sequence) FROM clusters "
                    + "JOIN proteins ON proteins.cluster_id = clusters.id JOIN peptides ON peptides.protein_id = proteins.id "
                    + "GROUP BY clusters.id");
            conn.begin();
            for (int i = 0; i < upClusterRows.length; i++)
            {
                Object[] values = (Object[]) upClusterRows[i];
                conn.rawSql("UPDATE nodes SET unique_peptides = " + values[1] + " WHERE nid = " + values[0] + " AND entity='cluster'");
            }
            conn.commit();

            Object[] r1 = conn.rawSqlSelect("SELECT count(DISTINCT sequence) FROM peptides");
            int totalPeptideCount = Integer.parseInt(((Object[]) r1[0])[0].toString());
            Object[] r2 = conn.rawSqlSelect("SELECT count(DISTINCT go_function_id) FROM proteins");
            int totalProteinCount = Integer.parseInt(((Object[]) r2[0])[0].toString());
            conn.rawSql("UPDATE nodes SET unique_peptides = " + totalPeptideCount + " WHERE id=0");
            conn.rawSql("UPDATE nodes SET protein_count = " + totalProteinCount + " WHERE id=0");

            System.out.println("Done updating cluster unique peptides");
        }
    }

    /**
     * Convert the old schema databases to new schema format
     *
     * @param sqliteDb
     * @throws Exception
     */
    public static void convertToNewSchema(SQLiteDatabaseConnection conn, String sqliteDb) throws Exception
    {
        // grab the column names from the nodes table
        String extraColumns = "";
        String extraColumnNames = "";
        Map<String, String> nodesColumns = conn.getTables().get("nodes");
        nodesColumns.remove("id");
        nodesColumns.remove("name");
        nodesColumns.remove("entity");
        nodesColumns.remove("uniquepeptides");
        nodesColumns.remove("protein_count");
        nodesColumns.remove("Mod_Count");
        nodesColumns.remove("cluster_coverage");
        nodesColumns.remove("visible");
        nodesColumns.remove("amino_acid_coverage");
        for (String column : nodesColumns.keySet())
        {
            extraColumns += ", " + column + " " + nodesColumns.get(column);
            extraColumnNames += ", " + column;
        }

        // first drop duplicate tables
        if (conn.getTables().containsKey("clusters"))
        {
            conn.rawSql("DROP TABLE clusters");
        }
        if (conn.getTables().containsKey("proteins"))
        {
            conn.rawSql("DROP TABLE proteins");
        }
        if (conn.getTables().containsKey("peptides"))
        {
            conn.rawSql("DROP TABLE peptides");
        }

        System.out.println("done dropping tables");

        String clusterTableSQL = "CREATE TABLE clusters (id INTEGER PRIMARY KEY, node_id INTEGER, cluster_name, amino_acid_coverage INTEGER DEFAULT 0)";
        String proteinTableSQL = "CREATE TABLE proteins (id INTEGER PRIMARY KEY, node_id INTEGER, cluster_id INTEGER, go_function_id, go_function_name, gi_number, amino_acids_composition INTEGER DEFAULT 0)";
        String peptideTableSQL = "CREATE TABLE peptides (id INTEGER PRIMARY KEY, node_id INTEGER, protein_id INTEGER, sequence, mod_count INTEGER DEFAULT 0, scan_count INTEGER DEFAULT 0"
                + extraColumns + ")";

        System.out.println("done creating tables");

        conn.rawSql(clusterTableSQL);
        conn.rawSql(proteinTableSQL);
        conn.rawSql(peptideTableSQL);

        conn.rawSql("CREATE INDEX clusternid ON clusters (node_id)");
        conn.rawSql("CREATE INDEX proteinnid ON proteins (node_id)");
        conn.rawSql("CREATE INDEX peptidenid ON peptides (node_id)");

        String clusterInsertSQL = "INSERT INTO clusters (node_id, cluster_name) SELECT id, name FROM nodes "
                + "WHERE entity='cluster' AND name != 'root'";
        String proteinInsertSQL = "INSERT INTO proteins (node_id, go_function_id) SELECT id, name FROM nodes "
                + "WHERE entity='protein'";
        String peptideInsertSQL = "INSERT INTO peptides (node_id, sequence, mod_count" + extraColumnNames + ") "
                + "SELECT id, name, mod_count" + extraColumnNames + " FROM nodes WHERE entity='peptide'";

        conn.rawSql(clusterInsertSQL);
        conn.rawSql(proteinInsertSQL);
        conn.rawSql(peptideInsertSQL);

        System.out.println("done inserting nodes");

        String clusterProteinEdges = "SELECT clusters.id, edges.target FROM edges JOIN clusters "
                + "ON edges.source = clusters.node_id";
        String proteinPeptideEdges = "SELECT proteins.id, edges.target FROM edges JOIN proteins "
                + "ON edges.source = proteins.node_id";

        long start = System.currentTimeMillis();
        Object[] rows = conn.rawSqlSelect(clusterProteinEdges);
        Object[][] data = new Object[rows.length][2];
        String preparedClusterProteinEdge = "UPDATE proteins SET cluster_id = ? WHERE node_id = ?";
        System.out.println("Starting cluster-protein edges... " + rows.length + " rows");
        for (int i = 0; i < rows.length; i++)
        {
            Object[] row = (Object[]) rows[i];
            data[i][0] = row[0];
            data[i][1] = row[1];
        }
        System.out.println("done preparing data and begin committing...");
        conn.preparedStatement(preparedClusterProteinEdge, data);
        long diff = (System.currentTimeMillis() - start);
        System.out.println("Finished with cluster-protein edges in " + diff + " ms");

        start = System.currentTimeMillis();
        rows = conn.rawSqlSelect(proteinPeptideEdges);
        data = new Object[rows.length][2];
        String preparedProteinPeptideEdge = "UPDATE peptides SET protein_id = ? WHERE id = ?";
        System.out.println("Starting protein-peptide edges... " + rows.length + " rows");
        for (int i = 0; i < rows.length; i++)
        {
            Object[] row = (Object[]) rows[i];
            data[i][0] = row[0];
            data[i][1] = row[1];
        }
        System.out.println("done preparing data and begin committing...");
        conn.preparedStatement(preparedProteinPeptideEdge, data);
        diff = (System.currentTimeMillis() - start);
        System.out.println("Finished with protein-peptide edges in " + diff + " ms");

        conn.disconnect();
    }

    public static boolean isValidFormat(SQLiteDatabaseConnection conn)
    {
        try
        {
            Map<String, Map<String, String>> tables = conn.getTables();
            if (tables.containsKey("nodes") && tables.containsKey("edges"))
            {
                if (tables.get("nodes").containsKey("name")
                        && tables.get("nodes").containsKey("entity") && tables.get("edges").containsKey("source")
                        && tables.get("edges").containsKey("target"))
                {
                    return true;
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

}
