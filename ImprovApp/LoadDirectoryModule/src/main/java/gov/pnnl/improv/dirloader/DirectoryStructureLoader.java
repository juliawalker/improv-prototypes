/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.dirloader;


import java.io.File;
import java.io.FileFilter;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Tree;


/**
 * Copied from the SCI2 plugin prepcessor modules
 */
public class DirectoryStructureLoader
{
    public static final int READ_ALL = -1;
    private final static String ID_LABEL = "label";

    private final static FileFilter DIR_ONLY_FILTER = new FileFilter()
    {
        @Override
        public boolean accept(File file)
        {
            return file.isDirectory();
        }
    };

    private final static FileFilter FILE_ONLY_FILTER = new FileFilter()
    {
        @Override
        public boolean accept(File file)
        {
            return file.isFile();
        }
    };
    
    public static Graph readDirectory(File directory, int numDirectoryLevels, boolean readFiles)
    {
        Schema metadata = getColumnMetadata(ID_LABEL, String.class);
        Tree tree = new Tree();
        tree.addColumns(metadata);
        Node rootNode = tree.addRoot();
        readDirectory(rootNode, directory, 0, numDirectoryLevels, readFiles);

        return tree;
    }

    private static Schema getColumnMetadata(String columnName, Class dataType)
    {
        Schema metadata = new Schema();
        metadata.addColumn(columnName, dataType, "");
        return metadata;
    }

    private static void readDirectory(Node node, File curDir, int curLevel, int levels, boolean readFiles)
    {
        node.setString(ID_LABEL, curDir.getName());

        /*
         * if this is the last level, then we need to stop. 
         * Otherwise build the sub-tree.
         */
        if (curLevel != levels)
        {
            if (readFiles)
            {
                File[] fileList = curDir.listFiles(FILE_ONLY_FILTER);

                //if they don't have permission then it'll be null, 
                //so just make it so there is no files in the dir
                if (fileList == null)
                {
                    fileList = new File[] { };
                }

                for (int i = 0; i < fileList.length; ++i)
                {
                    Node tnFile = ((Tree) node.getGraph()).addChild(node);
                    tnFile.setString(ID_LABEL, fileList[i].getName());
                }
            }

            File[] dirList = curDir.listFiles(DIR_ONLY_FILTER);

            //if they don't have permission then it'll be null, 
            //so just make it so there is no dirs in the dir
            if (dirList == null)
            {
                dirList = new File[] { };
            }

            for (int i = 0; i < dirList.length; ++i)
            {
                Node tnDir = ((Tree) node.getGraph()).addChild(node);
                readDirectory(tnDir, dirList[i], curLevel + 1, levels, readFiles);
            }
        }
    }

}
