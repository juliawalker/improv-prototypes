/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.prefuse.treemap;

import java.awt.Color;
import prefuse.action.assignment.ColorAction;
import prefuse.util.ColorLib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;


/**
 *
 */
public class BorderColorAction extends ColorAction
{
    private Color[] mBorderColors;

    public BorderColorAction(String group, Color[] borderColors)
    {
        super(group, VisualItem.STROKECOLOR);
        mBorderColors = borderColors;
    }

    public void setBorderColor(int level, Color color)
    {
        if (level < mBorderColors.length)
        {
            mBorderColors[level] = color;
        }
    }

    @Override
    public int getColor(VisualItem item)
    {
        NodeItem nitem = (NodeItem) item;
        if (nitem.isHover())
        {
            return ColorLib.rgb(99, 130, 191);
        }

        int depth = nitem.getDepth();
        switch (depth)
        {
            case 1:
            case 2:
            case 3:
                return mBorderColors[depth - 1].getRGB();

            default:
                break;
        }

        return ColorLib.gray(50);
    }

}
