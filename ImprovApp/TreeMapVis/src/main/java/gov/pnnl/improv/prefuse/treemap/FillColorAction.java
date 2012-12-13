/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.prefuse.treemap;

import gov.pnnl.improv.mpv.util.visual.ColorPreferences;
import java.awt.Color;
import prefuse.Visualization;
import prefuse.action.assignment.ColorAction;
import prefuse.data.Table;
import prefuse.data.expression.Predicate;
import prefuse.util.ColorLib;
import prefuse.util.ColorMap;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;


/**
 *
 */
public class FillColorAction extends ColorAction
{
    private static final int DEF_MIN = 0;
    private static final int DEF_MAX = 25;
    private ColorMap mColorMap = new ColorMap(ColorLib.getInterpolatedPalette(10,
                ColorPreferences.DARK_YELLOW,
                ColorLib.rgb(0, 25, 0)),
            DEF_MIN, DEF_MAX);

    private Color mSearchColor = ColorPreferences.SEARCH_HIGHLIGHT_DEFAULT;
    private Table t;
    private double mThreshold = 0.0;

    public FillColorAction(String group)
    {
        super(group, VisualItem.FILLCOLOR);
    }

    public FillColorAction(String group, Predicate condition)
    {
        super(group, condition, VisualItem.FILLCOLOR);
    }
    
    public void setSearchColor(Color aColor)
    {
        if (aColor != null)
        {
            mSearchColor =  aColor;
        }
    }

    /**
     * Sets the size of the color map.
     */
    public void setColorMapSize(int[] colorPalette, double min, double max)
    {
        mThreshold = min;
        mColorMap = new ColorMap(colorPalette, mThreshold, max);
    }

    @Override
    public int getColor(VisualItem item)
    {
        if (item != null && item instanceof NodeItem)
        {
            NodeItem nitem = (NodeItem) item;
            return getColorByScore(nitem);
        }
        else
        {
            return mColorMap.getColor(0);
        }
    }

    public int getColorByScore(NodeItem nitem)
    {
        if (nitem != null)
        {
            if (nitem.getChildCount() > 0)
            {
                return 0; // no fill for parent nodes
            }
            else
            {
                if (m_vis.isInGroup(nitem, Visualization.SEARCH_ITEMS))
                {
                    return mSearchColor.getRGB();
                }
                else
                {
                    return mColorMap.getColor(nitem.getDepth());
                }
            }
        }
        else
        {
            return mColorMap.getColor(0);
        }
        
//        int depth = nitem.getDepth();
//        int color = depth <= DEF_MAX ? mColorMap.getColor(depth) : mColorMap.getColor(0);
//        return color;
    }

//    @Override
//    protected void setup()
//    {
//        t = mDataSource.getTable("SELECT " + mScoreName + " FROM " + mDataSource.getNodeTable());
//        super.setup();
//    }

    @Override
    protected void finish()
    {
        t = null;
        super.finish();
    }

}
