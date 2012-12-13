/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.dirloader;

import gov.pnnl.improv.commonapi.tree.IGraphOperable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import prefuse.data.Graph;


@ActionID(
    category = "File",
    id = "gov.pnnl.improv.dirloader.DirectoryLoadingActionListener")
@ActionRegistration(
    iconBase = "gov/pnnl/improv/dirloader/folder_green_open_16.png",
    displayName = "#CTL_DirectoryLoadingActionListener")
@ActionReference(path = "Menu/File", position = 1300, separatorBefore = 1250)
@Messages("CTL_DirectoryLoadingActionListener=Load Directory Structure")
public final class DirectoryLoadingActionListener
    implements ActionListener, IDirectoryChangedListener
{
    private static final Logger LOGGER =
            Logger.getLogger(DirectoryLoadingActionListener.class.getName());
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        DirectoryOptionDialog.showDialog(null, this);
    }

    @Override
    public void directoryChanged(DirectoryChangeEvent event)
    {
        LOGGER.fine("### Change Event Fired");
        if (event == null || event.getDirectory() == null)
        {
            return;
        }
        
        File dir = new File(event.getDirectory());
        if (!dir.exists() || !dir.isDirectory())
        {
            System.out.println("###  Not a valid directory.");
            LOGGER.log(Level.WARNING,
                    "Not a valid directory or does not exist, {0}",
                    dir.getAbsolutePath());
            return;
        }
        
        Graph nodes = DirectoryStructureLoader.readDirectory(dir,
            event.isRecurseAll() ? Integer.MAX_VALUE : event.getMaxDepth(),
            event.isIncludeFiles());
        
        notifyDirectoryDataChange(nodes);
    }
    
    private void notifyDirectoryDataChange(Graph nodes)
    {
        if (nodes == null)
        {
            LOGGER.fine("Directory loader failed to produce data");
            return;
        }

        // Find all Graph Operable instances and notify with new data.
        LOGGER.fine("###  About to  notify all Graph operators");
        Collection<IGraphOperable> list =
                (Collection<IGraphOperable>)Lookup.getDefault()
                .lookupAll(IGraphOperable.class);
        
        if (list != null && !list.isEmpty())
        {
            for (IGraphOperable op : list)
            {
                op.processGraphData(nodes);
            } 
        }
    }

}
