/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.prefuse.treemap;

import gov.pnnl.improv.mpv.util.visual.ColorPreferences;
import gov.pnnl.improv.prefuse.treemap.TreeMapDemo.LabelLayout;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.StrokeAction;
import prefuse.action.layout.graph.SquarifiedTreeMapLayout;
import prefuse.action.layout.graph.TreeLayout;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Tree;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.GraphicsLib;
import prefuse.util.PrefuseLib;
import prefuse.util.display.DisplayLib;
import prefuse.util.display.PaintListener;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTree;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;


public class TreeMapRenderer extends Display
{
    private static final long serialVersionUID = -6207205463423289776L;

    private static final Logger LOGGER =
            Logger.getLogger(TreeMapRenderer.class.getName());
    
    public static final String TREE_GROUP = "tree";
    public static final String TREE_NODES_GROUP = "tree.nodes";
    public static final String TREE_EDGES_GROUP = "tree.edges";
    
    
    //**********************************************************************
    // Members
    //**********************************************************************
    
    private Tree mTree;
    private TreeStats mTreeStats;
    private ColorPreferences mColorStore;
    
    
    
    //----------------------------------------------
    // TBD
    //----------------------------------------------
    
    private FillColorAction mFillColorAction;
    private BorderColorAction mBorderColorAction;
    private StrokeAction mStrokeAction;
    private ActionList mColorsAction;
    private String mScoreName = "scan_count";		// Name of the field used for scoring
    private int mScoreLevel = 3;					// Level of the tree to use for scoring
    private int mColorMapMax = 0;					// Maximum score for color map as percentage of overall maximum score
    private Color[] mBorderColors;
    private int[] mPalette = ColorPreferences.GRADIENT_PALETTE_DEFAULT;

    /*
     * Lookup to get data range for a given field name and hierarchy level
     */
    private Set<Integer> mSearchItemsIds;
    private NodeItem mClusterMouseOver = null;
    private Set<NodeItem> mClustersToHighlight = new HashSet<NodeItem>();
    private Map<Integer, Set<NodeItem>> mClustersToLabel = new HashMap<Integer, Set<NodeItem>>();
    private Rectangle mSelection;
    private int mItemHighlight = -1;

    private boolean mDonePainting = true;
    private double mLayoutWidth;
    private double mLayoutHeight;


    //**********************************************************************
    // Public Methods
    //**********************************************************************
    
    public TreeMapRenderer()
    {
        super(new Visualization());
        setDoubleBuffered(true);

        mColorStore = new ColorPreferences();
        mBorderColors = mColorStore.getColorGroup(ColorPreferences.BORDER_COLORS);

        this.mSearchItemsIds = new HashSet<Integer>();
    }

    public void showData(Graph aGraph)
    {
        if (aGraph == null)
        {
            return;
        }
        
        mTree = (Tree)aGraph;
        mTreeStats = new TreeStats(mTree);

        //----------------------------------------------
        // Link Tree to Visualization		
        //----------------------------------------------
        VisualTree visTree = m_vis.addTree(TREE_GROUP, mTree);
        m_vis.setVisible(TREE_EDGES_GROUP, null, false);

        //----------------------------------------------
        // ensure that only leaf nodes are interactive
        //----------------------------------------------
        Predicate noLeaf = (Predicate) ExpressionParser.parse("childcount()>0");
        m_vis.setInteractive(TREE_NODES_GROUP, noLeaf, false);

        //----------------------------------------------
        // Node label renderer
        //----------------------------------------------
        DefaultRendererFactory rf = new DefaultRendererFactory();
        rf.add(new InGroupPredicate(TREE_NODES_GROUP), new NodeRenderer());
        rf.add(new InGroupPredicate("labels"), new LabelRenderer("label"));
//		rf.add(new InGroupPredicate(LABELS), this.mLabelRenderer);
        m_vis.setRendererFactory(rf);
        
        //------ TEMP
        Schema visSchema = PrefuseLib.getVisualItemSchema();
        Predicate labelP = (Predicate)ExpressionParser.parse("treedepth()=1");
        // now create the labels as decorators of the nodes
        m_vis.addDecorators("labels", TREE_NODES_GROUP, labelP, visSchema);
        //------ TEMP
        

        //----------------------------------------------
        // Color
        //----------------------------------------------
        ActionList colors = getColorActionList();
        m_vis.putAction("colors", colors);

        
        //----------------------------------------------
        //  Layout
        //----------------------------------------------
        ActionList layout = new ActionList();
        layout.add(new SquarifiedTreeMapLayout(TREE_GROUP));
        layout.add(new LabelLayout("labels"));
        layout.add(colors);
        layout.add(new RepaintAction());
        m_vis.putAction("layout", layout);

        // create the single filtering and layout action list
//        if (this.mLayout != null)
//        {
//            int previousLayout = new Integer(this.mLayout.getCurrentLayout());
//            this.mLayout = new CustomSquarifiedTreeMapLayout(TREE_GROUP, previousLayout);
//        } else
//        {
//            this.mLayout = new CustomSquarifiedTreeMapLayout(TREE_GROUP);
//            this.mLayout.setLayoutType(GalaxyViewerManager.getInstance().getLayout());
//        }


        //----------------------------------------------
        // Layout and Color
        //----------------------------------------------
        // initialize our display
        setItemSorter(new TreeDepthItemSorter());


        addPaintListener(new PaintListener()
        {
            @Override
            public void prePaint(Display arg0, Graphics2D g2)
            {
            }

            @Override
            public void postPaint(Display arg0, Graphics2D g2)
            {
                if (mSearchItemsIds != null)
                {
                    Color searchColor = mColorStore.getColor(ColorPreferences.SEARCH_HIGHLIGHT);
                    Graph g = (Graph) m_vis.getGroup(TREE_GROUP);
                    int nodesFound = 0;
                    Iterator iter = g.nodes();

                    while (iter.hasNext() && nodesFound < mSearchItemsIds.size())
                    {
                        NodeItem nitem = (NodeItem) iter.next();
                        int id = nitem.getInt("id");
                        if (mSearchItemsIds.contains(id))
                        {
                            Rectangle2D bounds = fromWorld(nitem.getBounds());
                            g2.setColor(new Color(searchColor.getRed(), searchColor.getGreen(), searchColor.getBlue(), 180));
                            g2.fillRect(getClosestInt(bounds.getX()), getClosestInt(bounds.getY()),
                                    getClosestInt(bounds.getWidth()), getClosestInt(bounds.getHeight()));
                        }
                    }
                }
                if (mSelection != null)
                {
                    Color color = mColorStore.getColor(ColorPreferences.MARQUEE_SELECTION);
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
                    g2.drawRect(mSelection.x, mSelection.y, mSelection.width, mSelection.height);
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
                    g2.fillRect(mSelection.x, mSelection.y, mSelection.width, mSelection.height);
                }

                Color col = mColorStore.getColor(ColorPreferences.CLUSTER_HIGHLIGHT);
                Color clusterColor = new Color(col.getRed(), col.getGreen(), col.getBlue());
                
                if (mClusterMouseOver != null)
                {
                    Rectangle2D clusterBounds = fromWorld(mClusterMouseOver.getBounds());
                    g2.setColor(clusterColor);
                    g2.drawRect(getClosestInt(clusterBounds.getX()), getClosestInt(clusterBounds.getY()),
                            getClosestInt(clusterBounds.getWidth()), getClosestInt(clusterBounds.getHeight()));
                }
                if (!mClustersToHighlight.isEmpty())
                {
                    for (NodeItem n : mClustersToHighlight)
                    {
                        if (n != null)
                        {
                            Rectangle2D clusterBounds = fromWorld(n.getBounds());
                            g2.setColor(clusterColor);
                            g2.drawRect(getClosestInt(clusterBounds.getX()), getClosestInt(clusterBounds.getY()),
                                    getClosestInt(clusterBounds.getWidth()), getClosestInt(clusterBounds.getHeight()));
                            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 70));
                            g2.fillRect(getClosestInt(clusterBounds.getX()), getClosestInt(clusterBounds.getY()),
                                    getClosestInt(clusterBounds.getWidth()), getClosestInt(clusterBounds.getHeight()));
                        }
                    }
                }
                if (mItemHighlight > 0)
                {
                    Graph g = (Graph) m_vis.getGroup(TREE_GROUP);
                    g2.setColor(clusterColor);
                    Iterator iter = g.nodes();
                    while (iter.hasNext())
                    {
                        NodeItem nitem = (NodeItem) iter.next();
                        int id = nitem.getInt("id");
                        if (mItemHighlight == id)
                        {
                            Rectangle2D bounds = fromWorld(nitem.getBounds());
//							g2.setColor(new Color(255, 0, 0, 180));
                            g2.fillRect(getClosestInt(bounds.getX()), getClosestInt(bounds.getY()),
                                    getClosestInt(bounds.getWidth()), getClosestInt(bounds.getHeight()));
                            break;
                        }
                    }
                }

                if (mTree != null)
                {
//                    String level = store.getString(IMPROVConstants.GV_NODE_LABEL_LEVEL);
//                    int depth = level.equals("Cluster") ? 1 : (level.equals("Protein") ? 2 : 3);
//                    
//                    FontRenderContext frc = g2.getFontRenderContext();
//                    Font f = new Font("Helvetica", Font.BOLD, FONT_SIZE_DEFAULT);
//
//                    for (NodeItem n : mClustersToLabel.get(depth))
//                    {
//                        if (n != null)
//                        {
//                            Rectangle2D cBounds = fromWorld(n.getBounds());
//                            String query = "SELECT name FROM " + mDataSource.getNodeTable() + " WHERE id = " + n.getInt("id");
//                            Object[] rows = mDataSource.rawSqlSelect(query);
//                            String s = new String(((Object[]) rows[0])[0].toString());
//                           
//                            TextLayout tl = new TextLayout(s, f, frc);
//                            Rectangle2D tBounds = tl.getBounds();
//                            int width = (int) tBounds.getWidth();
//                            int height = (int) tBounds.getHeight();
//                            int x = (int) cBounds.getCenterX();
//                            int y = (int) cBounds.getCenterY();
//                            int startX = x - (width / 2);
//                            g2.setColor(Color.WHITE);
//                            if ((x + width / 2 + 5) > getBounds().width)
//                            {
//                                startX = getBounds().width - width - 5;
//                            }
//                            g2.fillRect(startX - 5, y - (height / 2) - 5, width + 10, height + 10);
//                            g2.setColor(Color.BLACK);
//                            g2.setFont(f);
//                            g2.drawString(s, startX, y + (height / 2));
//                        }
//                    }
                }

                g2.dispose();
                mDonePainting = true;
            }

        });


        //---TEMP--------------------
        /*
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

        SearchQueryBinding searchQ = new SearchQueryBinding(visTree.getNodeTable(), "label");
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
        */
        //---TEMP--------------------

        
        m_vis.run("layout");
    }

    public boolean isDonePainting()
    {
        return this.mDonePainting;
    }

    public Rectangle2D getItemBounds(Node node)
    {
        if (node != null)
        {
            NodeItem nItem = (NodeItem) m_vis.getVisualItem(TREE_GROUP, node);
            if (nItem != null)
            {
                return nItem.getBounds();
            }
        }
        return null;
    }

    public void setSearchResults(Set<Integer> ids)
    {
        boolean lastSearchEmpty = this.mSearchItemsIds.isEmpty();
        this.mSearchItemsIds = ids;
        if (!lastSearchEmpty || !this.mSearchItemsIds.isEmpty())
        {
            repaint();
        }
    }

    public void setItemHighlight(int id)
    {
        this.mItemHighlight = id;
        repaint();
    }

    public void setClusterMouseOver(NodeItem n, boolean repaint)
    {
        this.mClusterMouseOver = n;
        this.mClustersToHighlight.clear();
        if (repaint)
        {
            repaint();
        }
    }

    public void setClusterHighlight(NodeItem n, boolean repaint)
    {
        this.mClustersToHighlight.clear();
        this.mClusterMouseOver = null;
        addClusterHighlight(n, repaint);
    }

    public void setClusterHighlight(Set<NodeItem> clusters, boolean repaint)
    {
        this.mClustersToHighlight = clusters;
        this.mClusterMouseOver = null;
        if (repaint)
        {
            repaint();
        }
    }

    public void addClusterHighlight(NodeItem node, boolean repaint)
    {
        if (node != null)
        {
            this.mClustersToHighlight.add(node);
        }
        if (repaint)
        {
            repaint();
        }
    }

    public void setClustersToLabel(Set<NodeItem> clusters, boolean repaint)
    {
        for (NodeItem item : clusters)
        {
            this.mClustersToLabel.get(item.getDepth()).add(item);
        }
        if (repaint)
        {
            repaint();
        }
    }

    public void addClusterToLabel(NodeItem n, boolean repaint)
    {
        if (n != null)
        {
            int depth = n.getDepth();
            Set<NodeItem> nodes = null;

            if (this.mClustersToLabel.isEmpty() || this.mClustersToLabel.get(depth) == null)
            {
                nodes = new HashSet<NodeItem>();
            }
            else
            {
                nodes = this.mClustersToLabel.get(depth);
            }

            if (nodes.contains(n))
            {
                nodes.remove(n);
            } else
            {
                nodes.add(n);
            }
            this.mClustersToLabel.put(depth, nodes);
        }

        if (repaint)
        {
            repaint();
        }
    }

    public void setMarqueeSelection(Rectangle rect)
    {
        this.mSelection = rect;
        repaint();
    }

    public void clear()
    {
        if (mTree != null)
        {
            PaintListener[] listeners = getListeners(PaintListener.class);
            for (PaintListener l : listeners)
            {
                removePaintListener(l);
            }

            mTree.getNodes().clear();
            mTree.getNodeTable().clear();
            mTree.getEdges().clear();
            mTree.getEdgeTable().clear();
            mTree.clear();
            mTree.removeAllGraphModelListeners();

            m_offscreen.flush();
            m_queue.clear();

            m_vis.removeGroup(TREE_GROUP);
            m_vis.removeAction("colors");
            m_vis.removeAction("layout");
            m_vis.reset();

            setItemSorter(null);
        }
    }

    private ActionList getColorActionList()
    {
        // border colors
        if (mBorderColorAction == null)
        {
            Color[] colors = ColorPreferences.GRADIENT_COLORS_DEFAULT;
            mBorderColorAction = new BorderColorAction(TREE_NODES_GROUP, colors);
            //mBorderColorAction = new BorderColorAction(TREE_NODES_GROUP, this.mBorderColors);
        }
        if (mFillColorAction == null)
        {
            mFillColorAction = new FillColorAction(TREE_NODES_GROUP);
        }
        if (mStrokeAction == null)
        {
            mStrokeAction = new StrokeAction(TREE_NODES_GROUP, new java.awt.BasicStroke(0));
        }

        double max = (mTree != null) ? mTreeStats.getTreeDepth() : 5.0;
        double min = max * this.mColorMapMax / 100.0;
        mFillColorAction.setColorMapSize(mPalette, min, max);

        mColorsAction = new ActionList();
        mColorsAction.add(mFillColorAction);
        mColorsAction.add(mStrokeAction);
        mColorsAction.add(mBorderColorAction);
        return mColorsAction;
    }

    private ActionList getLayoutActionList(TreeLayout layout)
    {
        ActionList colorsAction = (ActionList) m_vis.getAction("colors");
        ActionList layoutAction = new ActionList();
        layoutAction.add(layout);
//		layoutAction.add(new NodeLabelLayout(LABELS));
        if (colorsAction != null)
        {
            layoutAction.add(colorsAction);
        } else
        {
            layoutAction.add(getColorActionList());
        }
        layoutAction.add(new RepaintAction(m_vis));
        return layoutAction;
    }

    public void changeBorderColor(int level, Color color)
    {
        this.mBorderColors[level] = color;
        mBorderColorAction.setBorderColor(level, color);
        updateNodeColoring();
    }

    public void prepareLayout(int width, int height)
    {
        mDonePainting = false;
        this.mClustersToHighlight.clear();
        this.mClustersToLabel.clear();

        this.mSelection = null;
        this.mLayoutWidth = width;
        this.mLayoutHeight = height;
    }

    /**
     * Revises the node coloring to reflect any changes to control parameters.
     */
    private void updateNodeColoring()
    {
        if (mTree != null)
        {
            double max = (mTreeStats != null) ? mTreeStats.getTreeDepth() : 2.0;
            double min = max * this.mColorMapMax / 100.0;
            mFillColorAction.setColorMapSize(mPalette, min, max);

            this.mDonePainting = false;
            this.mColorsAction.run();
            repaint();
        }
    }

    /**
     * Zooms to a specific rectangle (in world coordinates).
     */
    public void zoomToRectangle(Rectangle2D worldRectangle)
    {
        while (isTranformInProgress())
        {
            try
            {
                Thread.sleep(500);
            } catch (InterruptedException e)
            {
                // don't care
            }
        }

        // Find the center of the zoom
        double xCenter = worldRectangle.getCenterX();
        double yCenter = worldRectangle.getCenterY();
        Point2D center = new Point2D.Double(xCenter, yCenter);

        // Find the maximum scale that will fit both dimensions
        Rectangle2D worldBounds = getItemBounds();
        double xScale = worldBounds.getWidth() / worldRectangle.getWidth();
        double yScale = worldBounds.getHeight() / worldRectangle.getHeight();
        double newScale = Math.min(xScale, yScale);

        // Get the change in scale
        double oldScale = getScale();
        double deltaScale = newScale / oldScale;

        // Change the view with a Display method
        animatePanAndZoomToAbs(center, deltaScale, 1);

        while (isTranformInProgress())
        {
            try
            {
                Thread.sleep(1000);
            } catch (InterruptedException e)
            {
                // don't care
            }
        }

        m_vis.run("repaint");
    }

    /**
     * Changes the zoom/view to fit the tree (as in the original view).
     */
    public void zoomToFit()
    {
        while (isTranformInProgress())
        {
            try
            {
                Thread.sleep(500);
            } catch (InterruptedException e)
            {
                // don't care
            }
        }

        double width = mLayoutWidth;
        double height = mLayoutHeight;
        Rectangle2D rect = new Rectangle2D.Double(0, 0, width, height);
        GraphicsLib.expand(rect, (int) (1 / getScale()));
        DisplayLib.fitViewToBounds(this, rect, 1);
    }

    public void panToPoint(int xDiff, int yDiff)
    {
        while (isTranformInProgress())
        {
            try
            {
                Thread.sleep(5);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        double w = (getScale() - 1) * getBounds().width;
        double h = (getScale() - 1) * getBounds().height;
        double fx = getDisplayX() - xDiff;
        double fy = getDisplayY() - yDiff;
        if (xDiff > 0)
        {
            if (fx < 0)
            {
                if (getDisplayX() != 0)
                {
                    xDiff = (int) getDisplayX() + 1;
                } else
                {
                    xDiff = 0;
                }
            }
        } else
        {
            if (fx > w)
            {
                double diff = w - getDisplayX();
                if (diff != 0)
                {
                    xDiff = (int) (-diff);
                } else
                {
                    xDiff = 0;
                }
            }
        }
        if (yDiff > 0)
        {
            if (fy < 0)
            {
                if (getDisplayY() != 0)
                {
                    yDiff = (int) getDisplayY() + 1;
                } else
                {
                    yDiff = 0;
                }
            }
        } else
        {
            if (fy > h)
            {
                double diff = h - getDisplayY();
                if (diff != 0)
                {
                    yDiff = (int) (-diff);
                } else
                {
                    yDiff = 0;
                }
            }
        }
        animatePan(xDiff, yDiff, 1);
    }

    public Tree getTree()
    {
        return this.mTree;
    }

    /**
     * Gets a copy of the palette being used.
     */
    public int[] getPalette()
    {
        return Arrays.copyOfRange(mPalette, 0, mPalette.length);
    }

    public void setDefault(String name, int percentOfMax, int level)
    {
        this.mScoreName = name;
        this.mScoreLevel = level;
        this.mColorMapMax = percentOfMax;

        final Color[] borderColors = ColorPreferences.BORDER_COLORS_DEFAULT;
        mBorderColorAction.setBorderColor(0, borderColors[0]);
        mBorderColorAction.setBorderColor(1, borderColors[1]);
        mBorderColorAction.setBorderColor(2, borderColors[2]);

        updateNodeColoring();
    }

    /**
     * Sets the color map maximum score as a percentage of the overall maximum
     * score.
     */
    public void setColorMapMax(int percentOfMax)
    {
        this.mColorMapMax = percentOfMax;
        updateNodeColoring();
    }
    
    
    //**********************************************************************
    // Private Methods
    //**********************************************************************

    private int getClosestInt(double val)
    {
        return (int) Math.floor(val);
    }

    private Rectangle2D fromWorld(Rectangle2D worldRect)
    {
        // Convert the rectangle to world coordinates
        AffineTransform transform = getTransform();
        Shape displayShape = transform.createTransformedShape(worldRect);
        Rectangle2D displayRect = displayShape.getBounds2D();

        return displayRect;
    }

    /**
     * A renderer for treemap nodes. Draws simple rectangles, but defers the
     * bounds management to the layout.
     */
    private class NodeRenderer extends AbstractShapeRenderer
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

}
