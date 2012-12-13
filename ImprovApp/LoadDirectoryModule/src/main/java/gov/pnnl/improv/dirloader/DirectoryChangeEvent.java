/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.dirloader;


/**
 *
 * @author d3x924
 */
public class DirectoryChangeEvent
{
    private int maxDepth = -1;
    private boolean includeFiles;
    private boolean recurseAll;
    private String directory;

    
    public DirectoryChangeEvent(String aDirectory, boolean aIncludeFiles,
            boolean aRecurseAll, int aMaxDepth)
    {
        directory = aDirectory;
        includeFiles = aIncludeFiles;
        recurseAll = aRecurseAll;
        maxDepth = aMaxDepth;
    }

    public int getMaxDepth()
    {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth)
    {
        this.maxDepth = maxDepth;
    }

    public boolean isIncludeFiles()
    {
        return includeFiles;
    }

    public void setIncludeFiles(boolean includeFiles)
    {
        this.includeFiles = includeFiles;
    }

    public boolean isRecurseAll()
    {
        return recurseAll;
    }

    public void setRecurseAll(boolean recurseAll)
    {
        this.recurseAll = recurseAll;
    }

    public String getDirectory()
    {
        return directory;
    }

    public void setDirectory(String directory)
    {
        this.directory = directory;
    }
    
}
