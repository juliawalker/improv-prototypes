/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.prefuse.treemap;


import gov.pnnl.improv.commonapi.tree.IGraphOperable;
import gov.pnnl.improv.treemapvis.TreeMapTopComponent;
import java.util.logging.Logger;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import prefuse.data.Graph;
import prefuse.data.Tree;



/**
 *
 * @author d3x924
 */
@ServiceProvider(service=IGraphOperable.class)
public class TreeMapGraphOperator implements IGraphOperable
{
    private static final Logger LOGGER =
            Logger.getLogger(TreeMapGraphOperator.class.getName());
    
    
    public TreeMapGraphOperator()
    {
    }

    /**
     * Load new graph data.
     * @param aNodes    the graph/tree nodes
     */
    @Override
    public void processGraphData(Graph aNodes)
    {
        LOGGER.info("@@@   Graph Operator called");
        TopComponent tc = WindowManager.getDefault().findTopComponent("TreeMapTopComponent");
        
        if (tc != null)
        {
            TreeMapTopComponent top = (TreeMapTopComponent)tc;
            Tree tr = (Tree)aNodes;

            top.updateTreeMap(tr);
            //top.updateTreeRenderer(tr);
        }
    }
    
}
