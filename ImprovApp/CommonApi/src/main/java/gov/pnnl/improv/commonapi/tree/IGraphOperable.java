/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.commonapi.tree;

import prefuse.data.Graph;


/**
 *
 * @author d3x924
 */
public interface IGraphOperable
{
    /**
     * Load new graph data.
     * @param aNodes    the graph/tree nodes
     */
    void processGraphData(Graph aNodes);
    
}
