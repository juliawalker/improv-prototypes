/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.prefuse.treemap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;


/**
 *
 * @author D3X924
 */
public class TreeStats
{
    private Tree mTree;
    private Map<Integer, Set<Node>> mClustersToLabel;
    private int mTreeDepth = 0;
    
    public TreeStats(Tree aTree)
    {
        mTree = aTree;
        mClustersToLabel = new HashMap<Integer, Set<Node>>();
    }

    /**
     * Gets the depth of a tree.
     */
    public int getTreeDepth()
    {
        return mTreeDepth;
    }
    
    public void computeStatistics()
    {
        gatherTreeStatistics();
    }
    
    private void buildTreeDepth()
    {
        if (mTree != null)
        {
            // Apparently not known, so compute it
            Table nodeTable = this.mTree.getNodeTable();
            int rowCount = nodeTable.getRowCount();

            for (int row = 0; row < rowCount; row++)
            {
                int level = this.mTree.getDepth(row);
                this.mTreeDepth = Math.max(this.mTreeDepth, level);
                Node node = mTree.getNode(level);
                addClusterToLabel(node);
            }
        }
    }

	private void addClusterToLabel(Node n)
    {
		if (n != null)
        {
			int depth = n.getDepth();	
			Set<Node> nodes = null;

			if (mClustersToLabel.get(depth) == null)
            {
				nodes = new HashSet<Node>();
                mClustersToLabel.put(depth, nodes);
            }
			else
            {
				nodes = mClustersToLabel.get(depth);
            }
            
            if (!nodes.contains(n))
            {
                nodes.add(n);
            }
        }
	}

    
    /**
     * Computes statistics for the tree.
     */
    private void gatherTreeStatistics()
    {
        // Determine how many levels it has
        buildTreeDepth();
        System.out.println("tree depth = " + mTreeDepth);
    }

}
