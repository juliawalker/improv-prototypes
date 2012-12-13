/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.mpv.util.visual;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import prefuse.util.ColorLib;


/**
 *
 * @author D3X924
 */
public class ColorPreferences
{
    //----------------------------------------------
    // Keys
    //----------------------------------------------
    public static final String MARQUEE_SELECTION = "MARQUEE_SELECTION";
    public static final String CLUSTER_HIGHLIGHT = "CLUSTER_HIGHLIGHT";
    public static final String SEARCH_HIGHLIGHT = "SEARCH_HIGHLIGHT";
    public static final String CLUSTER_BORDER = "CLUSTER_BORDER";
    public static final String PROTEIN_BORDER = "PROTEIN_BORDER";
    public static final String PEPTIDE_BORDER = "PEPTIDE_BORDER";
    
    public static final String BORDER_COLORS = "BORDER_COLORS";
    public static final String GRADIENT_COLORS = "GRADIENT_COLORS";
    
    //----------------------------------------------
    // Default Colors
    //----------------------------------------------
    public static final Color GRAY1 = new Color(100, 100, 100);
    public static final Color COL_DARK_YELLOW = new Color(255, 200, 0);
    public static final Color MARQUEE_SELECTION_DEFAULT = Color.GREEN;
    public static final Color CLUSTER_HIGHLIGHT_DEFAULT = Color.BLUE;
    public static final Color CLUSTER_BORDER_DEFAULT = GRAY1;
    public static final Color PROTEIN_BORDER_DEFAULT = Color.BLACK;
    public static final Color PEPTIDE_BORDER_DEFAULT = Color.BLACK;
    public static final Color SEARCH_HIGHLIGHT_DEFAULT = new Color(255, 102, 153);

    public static final int BLACK = Color.BLACK.getRGB();
    public static final int WHITE = Color.WHITE.getRGB();
    public static final int RED = Color.RED.getRGB();
    public static final int GREEN = Color.GREEN.getRGB();
    public static final int BLUE = Color.BLUE.getRGB();
    public static final int PURPLE = ColorLib.rgb(255, 0, 255);
    public static final int DARK_YELLOW = COL_DARK_YELLOW.getRGB();
    public static final int YELLOW = Color.YELLOW.getRGB();
    public static final int ORANGE = Color.ORANGE.getRGB();
    public static final int VERY_DARK_GREEN = ColorLib.rgba(0, 25, 0, 192);
    public static final int DARK_GREEN = ColorLib.rgb(0, 50, 0);
    public static final int CLEAR = ColorLib.rgba(0, 0, 0, 0);


    public static final Color[] BORDER_COLORS_DEFAULT = new Color[] {
        CLUSTER_BORDER_DEFAULT, PROTEIN_BORDER_DEFAULT, PEPTIDE_BORDER_DEFAULT
    };
    
    public static final Color[] GRADIENT_COLORS_DEFAULT = new Color[] {
        Color.BLACK, COL_DARK_YELLOW, Color.YELLOW, Color.WHITE
    };
    
    public static final int[] GRADIENT_PALETTE_DEFAULT = new int[] {
        BLACK, DARK_YELLOW, YELLOW, WHITE
    };
    

    //**********************************************************************
    // Members
    //**********************************************************************
    private Map<String, Color> mColorMap;
    private Map<String, Color[]> mColorGroup;

    
    public ColorPreferences()
    {
        mColorMap = new HashMap<String, Color>();
        mColorGroup = new HashMap<String, Color[]>();
        loadDefaultColors();
    }

    public void addColor(String aKey, Color aColor)
    {
        if (aKey != null && aColor != null)
        {
            mColorMap.put(aKey, aColor);
        }
    }
    
    public Color getColor(String aKey)
    {
        return mColorMap.get(aKey);
    }
    
    public Color[] getColorGroup(String aKey)
    {
        return mColorGroup.get(aKey);
    }
    
    public int[] colorsToPalette(Color[] colors)
    {
        if (colors != null && colors.length > 0)
        {
            final int len = colors.length;
            int[] palette = new int[len];
            
            for (int i = 0; i < len; i++)
            {
                palette[i] = colors[i].getRGB();
            }
            return palette;
        }
        
        return new int[0];
    }
    
    public final void loadDefaultColors()
    {
        mColorMap.put(MARQUEE_SELECTION, MARQUEE_SELECTION_DEFAULT);
        mColorMap.put(CLUSTER_HIGHLIGHT, CLUSTER_HIGHLIGHT_DEFAULT);
        mColorMap.put(SEARCH_HIGHLIGHT, SEARCH_HIGHLIGHT_DEFAULT);
        mColorMap.put(CLUSTER_BORDER, CLUSTER_BORDER_DEFAULT);
        mColorMap.put(PROTEIN_BORDER, PROTEIN_BORDER_DEFAULT);
        mColorMap.put(PEPTIDE_BORDER, PEPTIDE_BORDER_DEFAULT);
        
        mColorGroup.put(BORDER_COLORS, BORDER_COLORS_DEFAULT);
        mColorGroup.put(GRADIENT_COLORS, GRADIENT_COLORS_DEFAULT);
    }

}
