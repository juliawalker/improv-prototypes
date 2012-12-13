package gov.pnnl.improv.prefuse.treemap;


import gov.pnnl.improv.mpv.util.visual.ColorPreferences;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;


import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.StrokeAction;
import prefuse.action.layout.Layout;
import prefuse.action.layout.graph.SquarifiedTreeMapLayout;
import prefuse.controls.ControlAdapter;
import prefuse.data.Schema;
import prefuse.data.Tree;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.data.query.SearchQueryBinding;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.ColorMap;
import prefuse.util.FontLib;
import prefuse.util.PrefuseLib;
import prefuse.util.UpdateListener;
import prefuse.visual.DecoratorItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTree;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;


/**
 * Demonstration showcasing a TreeMap layout of a hierarchical data set and the
 * use of dynamic query binding for text search. Animation is used to highlight
 * changing search results.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class TreeMapDemo extends Display
{
    private static final long serialVersionUID = -481900265234475125L;

    // create data description of labels, setting colors, fonts ahead of time
    private static final Schema LABEL_SCHEMA = PrefuseLib.getVisualItemSchema();

    static
    {
        LABEL_SCHEMA.setDefault(VisualItem.INTERACTIVE, false);
        LABEL_SCHEMA.setDefault(VisualItem.TEXTCOLOR, ColorLib.gray(200));
        LABEL_SCHEMA.setDefault(VisualItem.FONT, FontLib.getFont("Tahoma", 11));
    }

    private static final String tree = "tree";
    private static final String treeNodes = "tree.nodes";
    private static final String treeEdges = "tree.edges";
    private static final String labels = "labels";
    private Tree mTree;
    private TreeStats mTreeStats;
    private SearchQueryBinding searchQ;
    private BorderColorAction mBorderColorAction;
    private BorderColorAction2 mBorderColorAction2;
    private FillColorAction mFillColorAction;
    private FillColorAction2 mFillColorAction2;
    private StrokeAction mStrokeAction;

    public TreeMapDemo()
    {
        super(new Visualization());
    }
    
    public void showData(Tree t, String label)
    {
        mTree = t;
        mTreeStats = new TreeStats(t);
        mTreeStats.computeStatistics();

        // add the tree to the visualization
        VisualTree vt = m_vis.addTree(tree, t);
        m_vis.setVisible(treeEdges, null, false);

        // ensure that only leaf nodes are interactive
        Predicate noLeaf = (Predicate) ExpressionParser.parse("childcount()>0");
        m_vis.setInteractive(treeNodes, noLeaf, false);

        // add labels to the visualization
        // first create a filter to show labels only at top-level nodes
        Predicate labelP = (Predicate) ExpressionParser.parse("treedepth()=1");
        // now create the labels as decorators of the nodes
        m_vis.addDecorators(labels, treeNodes, labelP, LABEL_SCHEMA);

        // set up the renderers - one for nodes and one for labels
        DefaultRendererFactory rf = new DefaultRendererFactory();
        rf.add(new InGroupPredicate(treeNodes), new NodeRenderer());
        rf.add(new InGroupPredicate(labels), new LabelRenderer(label));
        m_vis.setRendererFactory(rf);

        // border colors
        ActionList colors = getColorActionList();
        m_vis.putAction("colors", colors);

        // animate paint change
        ActionList animatePaint = new ActionList(400);
        animatePaint.add(new ColorAnimator(treeNodes));
        animatePaint.add(new RepaintAction());
        m_vis.putAction("animatePaint", animatePaint);

        // create the single filtering and layout action list
        ActionList layout = new ActionList();
        layout.add(new SquarifiedTreeMapLayout(tree));
        layout.add(new LabelLayout(labels));
        layout.add(colors);
        layout.add(new RepaintAction());
        m_vis.putAction("layout", layout);

        // initialize our display
        setItemSorter(new TreeDepthItemSorter());
        addControlListener(new ControlAdapter()
        {
            @Override
            public void itemEntered(VisualItem item, MouseEvent e)
            {
                item.setStrokeColor(mBorderColorAction.getColor(item));
                item.getVisualization().repaint();
            }

            @Override
            public void itemExited(VisualItem item, MouseEvent e)
            {
                item.setStrokeColor(item.getEndStrokeColor());
                item.getVisualization().repaint();
            }

        });

        searchQ = new SearchQueryBinding(vt.getNodeTable(), label);
        m_vis.addFocusGroup(Visualization.SEARCH_ITEMS, searchQ.getSearchSet());
        searchQ.getPredicate().addExpressionListener(new UpdateListener()
        {
            @Override
            public void update(Object src)
            {
                m_vis.cancel("animatePaint");
                m_vis.run("colors");
                m_vis.run("animatePaint");
            }

        });

        // perform layout
        m_vis.run("layout");
    }

    public SearchQueryBinding getSearchQuery()
    {
        return searchQ;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the stroke color for drawing treemap node outlines. A graded
     * grayscale ramp is used, with higer nodes in the tree drawn in lighter
     * shades of gray.
     */
    public static class BorderColorAction2 extends ColorAction
    {
        public BorderColorAction2(String group)
        {
            super(group, VisualItem.STROKECOLOR);
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
            if (depth < 2)
            {
                return ColorLib.gray(100);
            }
            else if (depth < 4)
            {
                return ColorLib.gray(75);
            }
            else
            {
                return ColorLib.gray(50);
            }
        }

    }

    /**
     * Set fill colors for treemap nodes. Search items are colored in pink,
     * while normal nodes are shaded according to their depth in the tree.
     */
    public static class FillColorAction2 extends ColorAction
    {
        private ColorMap cmap = new ColorMap(
                ColorLib.getInterpolatedPalette(10,
                ColorLib.rgb(85, 85, 85), ColorLib.rgb(0, 0, 0)), 0, 9);

        public FillColorAction2(String group)
        {
            super(group, VisualItem.FILLCOLOR);
        }

        @Override
        public int getColor(VisualItem item)
        {
            if (item instanceof NodeItem)
            {
                NodeItem nitem = (NodeItem) item;
                if (nitem.getChildCount() > 0)
                {
                    return 0; // no fill for parent nodes
                }
                else
                {
                    if (m_vis.isInGroup(item, Visualization.SEARCH_ITEMS))
                    {
                        return ColorLib.rgb(191, 99, 130);
                    }
                    else
                    {
                        return cmap.getColor(nitem.getDepth());
                    }
                }
            }
            else
            {
                return cmap.getColor(0);
            }
        }

    } // end of inner class TreeMapColorAction

    /**
     * Set label positions. Labels are assumed to be DecoratorItem instances,
     * decorating their respective nodes. The layout simply gets the bounds of
     * the decorated node and assigns the label coordinates to the center of
     * those bounds.
     */
    public static class LabelLayout extends Layout
    {
        public LabelLayout(String group)
        {
            super(group);
        }

        @Override
        public void run(double frac)
        {
            Iterator iter = m_vis.items(m_group);
            while (iter.hasNext())
            {
                DecoratorItem item = (DecoratorItem) iter.next();
                VisualItem node = item.getDecoratedItem();
                Rectangle2D bounds = node.getBounds();
                setX(item, null, bounds.getCenterX());
                setY(item, null, bounds.getCenterY());
            }
        }

    } // end of inner class LabelLayout

    /**
     * A renderer for treemap nodes. Draws simple rectangles, but defers the
     * bounds management to the layout.
     */
    public static class NodeRenderer extends AbstractShapeRenderer
    {
        private Rectangle2D m_bounds = new Rectangle2D.Double();

        public NodeRenderer()
        {
            m_manageBounds = false;
        }

        @Override
        protected Shape getRawShape(VisualItem item)
        {
            m_bounds.setRect(item.getBounds());
            return m_bounds;
        }

    } // end of inner class NodeRenderer

    private ActionList getColorActionList()
    {
        String nodeLabel = TreeMapRenderer.TREE_NODES_GROUP;

        // border colors
        if (mBorderColorAction2 == null)
        {
//            Color[] colors = ColorPreferences.BORDER_COLORS_DEFAULT;
//            mBorderColorAction = new BorderColorAction(nodeLabel, colors);
            mBorderColorAction2 = new BorderColorAction2(nodeLabel);
        }
        if (mFillColorAction == null)
        {
            mFillColorAction = new FillColorAction(nodeLabel);
        }
//        if (mFillColorAction2 == null)
//        {
//            mFillColorAction2 = new FillColorAction2(nodeLabel);
//        }
        
        if (mStrokeAction == null)
        {
            mStrokeAction = new StrokeAction(nodeLabel, new java.awt.BasicStroke(0));
        }

        double max = (mTree != null) ? mTreeStats.getTreeDepth() : 5.0;
        double min = 0;
        mFillColorAction.setColorMapSize(ColorPreferences.GRADIENT_PALETTE_DEFAULT,
                min, max);

        ActionList colors = new ActionList();
        colors.add(mBorderColorAction2);
        colors.add(mFillColorAction);
        colors.add(mStrokeAction);
        return colors;
    }

}
