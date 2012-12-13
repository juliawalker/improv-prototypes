/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.mpv.util.visual;


import java.io.File;
import prefuse.data.Tree;
import prefuse.data.io.TreeMLReader;


/**
 *
 * @author d3x924
 */
public class TreeDataUtil
{
    public static final String DEMO_NODE_FILE1 = "/chi-ontology.xml.gz";
    private static final String URI_FILE = "file:/";
    
    public static Tree loadTree(File aDataFile)
    {
        Tree t = null;
        boolean loadFromDemo = (aDataFile == null);
        
        try
        {
            String dataFile = loadFromDemo ? DEMO_NODE_FILE1 : aDataFile.getPath();
            System.out.println("File Name: " + aDataFile);
            
            if (loadFromDemo)
            {
                t = (Tree)new TreeMLReader().readGraph(dataFile);
            }
            else
            {
                dataFile = dataFile.startsWith(URI_FILE)
                        ? dataFile
                        : (URI_FILE + dataFile);
                t = (Tree)new TreeMLReader().readGraph(dataFile);
            }    
            
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return t;
    }    
    
}
