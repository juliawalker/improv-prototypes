package gov.pnnl.improv.views.treemap;

import gov.pnnl.improv.datasource.TreeDBDataSource;
import gov.pnnl.improv.util.IMPROVConstants;
import gov.pnnl.improv.util.data.FieldLevel;
import gov.pnnl.improv.util.data.Range;
import gov.pnnl.improv.util.data.Stat;
import gov.pnnl.improv.util.visual.CustomColorMap;
import gov.pnnl.improv.views.ColorPreferences;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.swt.graphics.RGB;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.StrokeAction;
import prefuse.action.layout.graph.TreeLayout;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.data.tuple.TupleSet;
import prefuse.data.util.TreeNodeIterator;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.util.ColorLib;
import prefuse.util.ColorMap;
import prefuse.util.GraphicsLib;
import prefuse.util.display.DisplayLib;
import prefuse.util.display.PaintListener;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;


public class TreeMap extends Display
{
    private static final long serialVersionUID = 616725384865595755L;

    private TreeDBDataSource mDataSource;
    private FillColorAction mFillColorAction;
    private BorderColorAction mBorderColorAction;
    private StrokeAction mStrokeAction;
    private ActionList mColorsAction;
    private SQLiteLabelRenderer mLabelRenderer;

    private String mScoreName = "scan_count";		// Name of the field used for scoring
    private int mScoreLevel = 3;					// Level of the tree to use for scoring
    private int mColorMapMax = 0;					// Maximum score for color map as percentage of overall maximum score
    private int mTreeDepth = 0;						// Depth of the tree (= maximum of all node depths)
    private Color[] mBorderColors;
    private int[] mPalette = ColorPreferences.GRADIENT_PALETTE_DEFAULT;

    /*
     * Lookup to get data range for a given field name and hierarchy level
     */
    private Map<FieldLevel, Range> mDataRangeMap = new HashMap<FieldLevel, Range>();
    private Set<Integer> mSearchItemsIds;
    private NodeItem mClusterMouseOver = null;
    private Set<NodeItem> mClustersToHighlight = new HashSet<NodeItem>();
    private Map<Integer, Set<NodeItem>> mClustersToLabel = new HashMap<Integer, Set<NodeItem>>();
    private Rectangle mSelection;
    private int mItemHighlight = -1;
//	private static TreeNode top5Node = new TreeNode("Top 5 Expressed Clusters");
    private boolean mDonePainting = true;
    private double mLayoutWidth;
    private double mLayoutHeight;
    private Tree mTree;
    private CustomSquarifiedTreeMapLayout mLayout;

    public TreeMap(TreeDBDataSource datasource)
    {
        super(new Visualization());

        setDoubleBuffered(true);

        this.mSearchItemsIds = new HashSet<Integer>();
        this.mBorderColors = new RGB[3];

        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        this.mBorderColors[0] = PreferenceConverter.getColor(store, IMPROVConstants.GV_CLUSTER_BORDER_COLOR);
        this.mBorderColors[1] = PreferenceConverter.getColor(store, IMPROVConstants.GV_PROTEIN_BORDER_COLOR);
        this.mBorderColors[2] = PreferenceConverter.getColor(store, IMPROVConstants.GV_PEPTIDE_BORDER_COLOR);

        if (datasource != null)
        {
            setDatasource(datasource);
        }
    }

    public void setDatasource(TreeDBDataSource datasource)
    {
        this.mDataSource = datasource;
        this.mTree = datasource.getData();
        gatherTreeStatistics();

        // add the tree to the visualization			
        m_vis.addTree(TREE_GROUP, this.mTree);
        m_vis.setVisible(TREE_EDGES_GROUP, null, false);

        Graph g = (Graph) m_vis.getGroup(TREE_GROUP);
        TupleSet nodes = g.getNodes();
        nodes.addColumns(CustomSquarifiedTreeMapLayout.AREA_SCHEMA);
        nodes.addColumns(CustomSquarifiedTreeMapLayout.RANK_SCHEMA);
        nodes.addColumns(CustomSquarifiedTreeMapLayout.ALT_SCHEMA);
        nodes.addColumns(CustomSquarifiedTreeMapLayout.COV_SCHEMA);
        nodes.addColumns(CustomSquarifiedTreeMapLayout.RANK_STR_SCHEMA);

        // ensure that only leaf nodes are interactive
        Predicate noLeaf = (Predicate) ExpressionParser.parse("childcount()>0");
        m_vis.setInteractive(TREE_NODES_GROUP, noLeaf, false);

        // add labels to the visualization
        // first create a filter to show labels only at top-level nodes
//		Predicate labelP = (Predicate)ExpressionParser.parse("treedepth()=1");
        // now create the labels as decorators of the nodes
//		m_vis.addDecorators(LABELS, TREE_NODES_GROUP, labelP, LABEL_SCHEMA);

        // set up the renderers - one for nodes and one for labels
        boolean showLabels = Activator.getDefault().getPreferenceStore().getBoolean(IMPROVConstants.GV_NODE_LABELS);
        this.mLabelRenderer = new SQLiteLabelRenderer(mDataSource, "name", showLabels);
        DefaultRendererFactory rf = new DefaultRendererFactory();
        rf.add(new InGroupPredicate(TREE_NODES_GROUP), new NodeRenderer());
//		rf.add(new InGroupPredicate(LABELS), this.mLabelRenderer);
        m_vis.setRendererFactory(rf);

        // color settings
        m_vis.putAction("colors", getColorActionList());

        // create the single filtering and layout action list
        if (this.mLayout != null)
        {
            int previousLayout = new Integer(this.mLayout.getCurrentLayout());
            this.mLayout = new CustomSquarifiedTreeMapLayout(TREE_GROUP, previousLayout);
        } else
        {
            this.mLayout = new CustomSquarifiedTreeMapLayout(TREE_GROUP);
            this.mLayout.setLayoutType(GalaxyViewerManager.getInstance().getLayout());
        }
        setLayoutAction();

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
                    RGB searchRgb = PreferenceConverter.getColor(Activator.getDefault().getPreferenceStore(), IMPROVConstants.SEARCH_HIGHLIGHT);
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
                            g2.setColor(new Color(searchRgb.red, searchRgb.green, searchRgb.blue, 180));
                            g2.fillRect(getClosestInt(bounds.getX()), getClosestInt(bounds.getY()),
                                    getClosestInt(bounds.getWidth()), getClosestInt(bounds.getHeight()));
                        }
                    }
                }
                if (mSelection != null)
                {
                    RGB rgb = PreferenceConverter.getColor(Activator.getDefault().getPreferenceStore(), IMPROVConstants.MARQUEE_SELECTION);
                    g2.setColor(new Color(rgb.red, rgb.green, rgb.blue));
                    g2.drawRect(mSelection.x, mSelection.y, mSelection.width, mSelection.height);
                    g2.setColor(new Color(rgb.red, rgb.green, rgb.blue, 100));
                    g2.fillRect(mSelection.x, mSelection.y, mSelection.width, mSelection.height);
                }

                RGB cRGB = PreferenceConverter.getColor(Activator.getDefault().getPreferenceStore(), IMPROVConstants.CLUSTER_HIGHLIGHT);
                if (mClusterMouseOver != null)
                {
                    Rectangle2D clusterBounds = fromWorld(mClusterMouseOver.getBounds());
                    g2.setColor(new Color(cRGB.red, cRGB.green, cRGB.blue));
                    g2.drawRect(getClosestInt(clusterBounds.getX()), getClosestInt(clusterBounds.getY()),
                            getClosestInt(clusterBounds.getWidth()), getClosestInt(clusterBounds.getHeight()));
                }
                if (!mClustersToHighlight.isEmpty())
                {
                    for (NodeItem n : mClustersToHighlight)
                    {
//						RGB rgb = PreferenceConverter.getColor(Activator.getDefault().getPreferenceStore(), IMPROVConstants.CLUSTER_HIGHLIGHT);						
                        if (n != null)
                        {
                            Rectangle2D clusterBounds = fromWorld(n.getBounds());
                            g2.setColor(new Color(cRGB.red, cRGB.green, cRGB.blue));
                            g2.drawRect(getClosestInt(clusterBounds.getX()), getClosestInt(clusterBounds.getY()),
                                    getClosestInt(clusterBounds.getWidth()), getClosestInt(clusterBounds.getHeight()));
                            g2.setColor(new Color(cRGB.red, cRGB.green, cRGB.blue, 70));
                            g2.fillRect(getClosestInt(clusterBounds.getX()), getClosestInt(clusterBounds.getY()),
                                    getClosestInt(clusterBounds.getWidth()), getClosestInt(clusterBounds.getHeight()));
                        }
                    }
                }
                if (mItemHighlight > 0)
                {
                    Graph g = (Graph) m_vis.getGroup(TREE_GROUP);
                    g2.setColor(new Color(cRGB.red, cRGB.green, cRGB.blue));
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

                IPreferenceStore store = Activator.getDefault().getPreferenceStore();
                if (!mClustersToLabel.isEmpty() && store.getBoolean(IMPROVConstants.GV_NODE_LABELS))
                {
                    String level = store.getString(IMPROVConstants.GV_NODE_LABEL_LEVEL);
                    int depth = level.equals("Cluster") ? 1 : (level.equals("Protein") ? 2 : 3);
                    FontRenderContext frc = g2.getFontRenderContext();
                    int fontSize = store.getInt(IMPROVConstants.GV_NODE_FONT_SIZE);
                    Font f = new Font("Helvetica", Font.BOLD, fontSize);
                    for (NodeItem n : mClustersToLabel.get(depth))
                    {
                        if (n != null)
                        {
                            Rectangle2D cBounds = fromWorld(n.getBounds());
                            String query = "SELECT name FROM " + mDataSource.getNodeTable() + " WHERE id = " + n.getInt("id");
                            Object[] rows = mDataSource.rawSqlSelect(query);
                            String s = new String(((Object[]) rows[0])[0].toString());
                            TextLayout tl = new TextLayout(s, f, frc);
                            Rectangle2D tBounds = tl.getBounds();
                            int width = (int) tBounds.getWidth();
                            int height = (int) tBounds.getHeight();
                            int x = (int) cBounds.getCenterX();
                            int y = (int) cBounds.getCenterY();
                            int startX = x - (width / 2);
                            g2.setColor(Color.WHITE);
                            if ((x + width / 2 + 5) > getBounds().width)
                            {
                                startX = getBounds().width - width - 5;
                            }
                            g2.fillRect(startX - 5, y - (height / 2) - 5, width + 10, height + 10);
                            g2.setColor(Color.BLACK);
                            g2.setFont(f);
                            g2.drawString(s, startX, y + (height / 2));
                        }
                    }
                }

                g2.dispose();
                mDonePainting = true;
            }

        });
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
            } else
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
        if (this.mDataSource != null)
        {
            PaintListener[] listeners = getListeners(PaintListener.class);
            for (PaintListener l : listeners)
            {
                removePaintListener(l);
            }

            this.mTree.getNodes().clear();
            this.mTree.getNodeTable().clear();
            this.mTree.getEdges().clear();
            this.mTree.getEdgeTable().clear();
            this.mTree.clear();
            this.mTree.removeAllGraphModelListeners();

            this.mDataRangeMap.clear();

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
        if (this.mBorderColorAction == null)
        {
            this.mBorderColorAction = new BorderColorAction(TREE_NODES_GROUP, this.mBorderColors);
        }
        if (this.mFillColorAction == null)
        {
            this.mFillColorAction = new FillColorAction(TREE_NODES_GROUP);
        }
        if (this.mStrokeAction == null)
        {
            this.mStrokeAction = new StrokeAction(TREE_NODES_GROUP, new BasicStroke(0));
        }

        Range range = this.mDataRangeMap.get(new FieldLevel(this.mScoreName, this.mScoreLevel));
        double max = 2.0;
        if (range != null)
        {
            max = range.getMax();
        }
        double min = max * this.mColorMapMax / 100.0;
        this.mFillColorAction.setColorMapSize(min, max);

        this.mColorsAction = new ActionList();
        this.mColorsAction.add(mFillColorAction);
        this.mColorsAction.add(mStrokeAction);
        this.mColorsAction.add(mBorderColorAction);
        return this.mColorsAction;
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

    public void changeBorderColor(int level, RGB color)
    {
        this.mBorderColors[level] = color;
        mBorderColorAction.setBorderColor(level, color);
        updateColoring();
    }

    public int getCurrentLayout()
    {
        return this.mLayout.getCurrentLayout();
    }

    public String getCurrentLayoutName()
    {
        int layout = getCurrentLayout();
        switch (layout)
        {
            case CustomSquarifiedTreeMapLayout.MODCOUNT:
                return "mod_count";
            case CustomSquarifiedTreeMapLayout.SCANCOUNT:
                return "scancounts";
            case CustomSquarifiedTreeMapLayout.PEPTIDE:
                return "uniquepeptides";
            case CustomSquarifiedTreeMapLayout.CLUSTER_COVERAGE:
                return "cluster_coverage";
            default:
                return CustomSquarifiedTreeMapLayout.AREA;
        }
    }

    public void doCurrentLayout()
    {
        mDonePainting = false;
        if (mDataSource != null)
        {
            changeLayout(this.mLayout.getCurrentLayout(), true);
        }
    }

    public void prepareLayout(int width, int height)
    {
        mDonePainting = false;
        this.mClustersToHighlight.clear();
        this.mClustersToLabel.clear();
        //		this.mSearchItemsIds = null;
        this.mSelection = null;
        this.mLayoutWidth = width;
        this.mLayoutHeight = height;
    }

    public void changeLayout(int layoutType, boolean runImmediately)
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
        this.mLayout.setLayoutType(layoutType);

        Range range = this.mDataRangeMap.get(new FieldLevel(this.mScoreName, this.mScoreLevel));
        double max = 2.0;
        if (range != null)
        {
            max = range.getMax();
        }
        this.mLayout.setScoreField(this.mScoreName, max * ((double) this.mColorMapMax / 100.0));

        this.mLayout.setLayoutBounds(new Rectangle2D.Double(0.0, 0.0, mLayoutWidth, mLayoutHeight));
        setLayoutAction();

        if (runImmediately)
        {
            runLayout();
        }
    }

    private void setLayoutAction()
    {
        if (m_vis.getAction("layout") != null)
        {
            m_vis.cancel("layout");
            m_vis.cancel("colors");
            m_vis.removeAction("layout");
        }
        ActionList layoutAction = getLayoutActionList(this.mLayout);
        m_vis.putAction("layout", layoutAction);
    }

    private void runLayout()
    {
        m_vis.run("layout");
        m_vis.run("repaint");
    }

//	public void updateLabels(boolean visible) {		
//		this.mLabelRenderer.setVisible(visible);		
//	}
    /**
     * Revises the node coloring to reflect any changes to control parameters.
     */
    private void updateColoring()
    {
        if (this.mDataSource != null)
        {
            // Get the maximum score (given the current combination of level and field)
            Range range = this.mDataRangeMap.get(new FieldLevel(this.mScoreName, this.mScoreLevel));
            double max = 2.0;
            if (range != null)
            {
                max = range.getMax();
            }
            double min = max * this.mColorMapMax / 100.0;
            this.mFillColorAction.setColorMapSize(min, max);

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

    /**
     * Computes statistics for the tree.
     */
    private void gatherTreeStatistics()
    {
        // Determine how many levels it has
        getTreeDepth();
        System.out.println("tree depth = " + mTreeDepth);

        // For each integer attribute
        String select = "SELECT";
        boolean first = true;
        List<String> cols = new ArrayList<String>();
        Map<String, String> colsTypes = mDataSource.getColumnAndTypesForTable(mDataSource.getNodeTable());
        for (String col : colsTypes.keySet())
        {
            String type = colsTypes.get(col);
            if (!col.equals("id") && !col.equals("nid") && !col.equals("visible"))
            {
                if (type.equals("INTEGER") || type.equals("REAL") || col.equals("protein_count"))
                {
                    if (first)
                    {
                        first = false;
                    } else
                    {
                        select += ",";
                    }
                    select += " min(" + col + "), max(" + col + ")";
                    cols.add(col);
                }
            }
        }
        select += " FROM " + mDataSource.getNodeTable() + " WHERE id > 0 AND entity=";
        Object[] cRow = (Object[]) mDataSource.rawSqlSelect(select + "'cluster'")[0];
        Object[] pRow = (Object[]) mDataSource.rawSqlSelect(select + "'protein'")[0];
        Object[] peRow = (Object[]) mDataSource.rawSqlSelect(select + "'peptide'")[0];

        int i = 0;
        for (String col : cols)
        {
            Range range1 = new Range(Double.parseDouble(cRow[i].toString()), Double.parseDouble(cRow[i + 1].toString()));
            Range range2 = new Range(Double.parseDouble(pRow[i].toString()), Double.parseDouble(pRow[i + 1].toString()));
            Range range3 = new Range(Double.parseDouble(peRow[i].toString()), Double.parseDouble(peRow[i + 1].toString()));
            this.mDataRangeMap.put(new FieldLevel(col, 1), range1);
            this.mDataRangeMap.put(new FieldLevel(col, 2), range2);
            this.mDataRangeMap.put(new FieldLevel(col, 3), range3);
            System.out.println(col + "(" + 1 + ") : " + range1);
            System.out.println(col + "(" + 2 + ") : " + range2);
            System.out.println(col + "(" + 3 + ") : " + range3);
            i += 2;
        }
    }

    public void calcStats()
    {
        Range range = this.mDataRangeMap.get(new FieldLevel(this.mScoreName, this.mScoreLevel));
        double max = 0.0;
        if (range != null)
        {
            max = range.getMax();
        }
        double min = max * ((double) this.mColorMapMax / 100.0);
        List<Integer> proteinIds = new ArrayList<Integer>();
        Iterator iter = new TreeNodeIterator(mTree.getRoot(), true);
        while (iter.hasNext())
        {
            Node n = (Node) iter.next();
            int id = n.getInt("id");
            int depth = n.getDepth();
            int clusterCoverage = 0;
            if (n.getChildCount() > 0 && depth == 2)
            {
                Node c = n.getFirstChild();
                for (; c != null; c = c.getNextSibling())
                {
                    int peptideCoverage = mDataSource.getSingleIntValue("SELECT count(id) FROM " + mDataSource.getNodeTable()
                            + " WHERE id=" + c.getInt("id") + " AND " + this.mScoreName + " > " + min);
                    clusterCoverage = clusterCoverage | peptideCoverage;
                    if (clusterCoverage > 0)
                    {
                        break;
                    }
                }
                if (clusterCoverage > 0)
                {
                    proteinIds.add(id);
                }
            }
        }
//		Object[] rows = mDataSource.rawSqlSelect("SELECT id FROM " + mDataSource.getNodeTable() + " WHERE entity='protein' AND " + mScoreName + " > " + min);
//		for (int j = 0; j < rows.length; j++) {
//			proteinIds.add((Integer)((Object[])rows[j])[0]);
//		}
//		rows = null;

        Node root = mTree.getRoot();
        final double[][] clusterToCoverage = new double[root.getChildCount()][3];
        int c = 0;
        iter = root.children();
        while (iter.hasNext())
        {
            Node cluster = (Node) iter.next();
            if (cluster.getChildCount() > 0)
            {
                int proteins = 0;
                int protCount = 0;
                Node child = cluster.getFirstChild();
                while (child != null)
                {
                    if (proteinIds.contains(child.getInt("id")))
                    {
                        proteins++;
                    }
                    child = child.getNextSibling();
                    protCount++;
                }
                double coverage = (double) proteins / protCount * 100.0;
                clusterToCoverage[c][0] = coverage;
                clusterToCoverage[c][1] = proteins;
                clusterToCoverage[c][2] = cluster.getInt("id");
                c++;
            }
        }
        Arrays.sort(clusterToCoverage, 0, c, new Comparator<double[]>()
        {
            @Override
            public int compare(double[] o1, double[] o2)
            {
                if (o1[0] > o2[0])
                {
                    return -1;
                }
                if (o1[0] < o2[0])
                {
                    return 1;
                }
                if (o1[1] > o2[1])
                {
                    return -1;
                }
                if (o1[1] < o2[1])
                {
                    return 1;
                }
                if (o1[2] > o2[2])
                {
                    return -1;
                }
                if (o1[2] < o2[2])
                {
                    return 1;
                }
                return 0;
            }

        });

        DecimalFormat formatter = new DecimalFormat("#.####");
//		TreeNode[] top5 = new TreeNode[5];
        List<TreeNode> top5List = new ArrayList<TreeNode>();
        for (int k = 0; k < 5 && k < clusterToCoverage.length; k++)
        {
            double cc = clusterToCoverage[k][0];
            int proteinCount = (int) clusterToCoverage[k][1];
            int id = (int) clusterToCoverage[k][2];
            String name = mDataSource.getSingleValue("SELECT name FROM " + mDataSource.getNodeTable() + " WHERE id=" + id).toString();
            Stat s = new Stat((k + 1) + ": " + name, formatter.format(cc) + "% (" + proteinCount + ")");
            TreeNode n = new TreeNode(s);
//			top5[k] = n;
            top5List.add(n);
        }
        TreeNode[] top5 = top5List.toArray(new TreeNode[top5List.size()]);
//		StatisticsManager.getInstance().removeDsNode(top5Node);
        TreeNode top5Node = new TreeNode("Top 5 Expressed Clusters");
        top5Node.setChildren(top5);

        StatisticsManager.getInstance().deleteDsChildNode(mDataSource.getName(), top5Node.getValue().toString());
        StatisticsManager.getInstance().addDsChildNode(mDataSource.getName(), top5Node);
    }

    public Tree getTree()
    {
        return this.mTree;
    }

    /**
     * Gets the depth of a tree.
     */
    public int getTreeDepth()
    {
        if (this.mTreeDepth == 0)
        {
            // Apparently not known, so compute it
            Table nodeTable = this.mTree.getNodeTable();
            int rowCount = nodeTable.getRowCount();

            for (int row = 0; row < rowCount; row++)
            {
                int level = this.mTree.getDepth(row);
                this.mTreeDepth = Math.max(this.mTreeDepth, level);
            }
        }
        return this.mTreeDepth;
    }

    /**
     * Gets a copy of the palette being used.
     */
    public int[] getPalette()
    {
        int[] copy = (int[]) mPalette.clone();
        return copy;
    }

    /**
     * Gets the tree level to use for scoring.
     */
    public int getScoreLevel()
    {
        return this.mScoreLevel;
    }

    public void setDefault(String name, int percentOfMax, int level)
    {
        this.mScoreName = name;
        this.mScoreLevel = level;
        this.mColorMapMax = percentOfMax;

        mBorderColorAction.setBorderColor(0, DEFAULT_BORDER_COLORS[0]);
        mBorderColorAction.setBorderColor(1, DEFAULT_BORDER_COLORS[1]);
        mBorderColorAction.setBorderColor(2, DEFAULT_BORDER_COLORS[2]);

        updateColoring();
    }

    /**
     * Sets the field to use for scoring.
     */
    public void setScoreField(String name, boolean run)
    {
        this.mScoreName = name;
        if (run)
        {
            updateColoring();
        }
    }

    /**
     * Sets the tree level to use for scoring.
     */
    public void setScoreLevel(int level)
    {
        this.mScoreLevel = level;
        updateColoring();
    }

    /**
     * Sets the color map maximum score as a percentage of the overall maximum
     * score.
     */
    public void setColorMapMax(int percentOfMax)
    {
        this.mColorMapMax = percentOfMax;
        updateColoring();
    }

    public void setPalette(RGB[] palette)
    {
        this.mPalette = CustomColorMap.getUserPreferredPalette(palette[0], palette[1], palette[2], palette[3]);
        updateColoring();
    }

    // ------------------------------------------------------------------------
    /**
     * Set the stroke color for drawing treemap node outlines. A graded
     * grayscale ramp is used, with higher nodes in the tree drawn in lighter
     * shades of gray.
     */
    private class BorderColorAction extends ColorAction
    {

        /**
         * Whether or not to show a border around items at the bottom level.
         */
        private RGB[] borderColors;// = new RGB[] { DEFAULT_BORDER_COLORS[0], DEFAULT_BORDER_COLORS[1], DEFAULT_BORDER_COLORS[2] };

        public BorderColorAction(String group, RGB[] borderColors)
        {
            super(group, VisualItem.STROKECOLOR);
            this.borderColors = borderColors;
        }

        public void setBorderColor(int level, RGB color)
        {
            if (level < borderColors.length)
            {
                borderColors[level] = color;
            }
        }

        public int getColor(VisualItem item)
        {
            NodeItem nitem = (NodeItem) item;
            if (nitem.isHover())
            {
                return ColorLib.rgb(99, 130, 191);
            }

            int depth = nitem.getDepth();
            if (depth == 1)
            {
                return ColorLib.rgb(borderColors[0].red, borderColors[0].green, borderColors[0].blue);
            } else if (depth == 2)
            {
                return ColorLib.rgb(borderColors[1].red, borderColors[1].green, borderColors[1].blue);
            } else if (depth == 3)
            {
                return ColorLib.rgba(borderColors[2].red, borderColors[2].green, borderColors[2].blue, 50);
            } else
            {
                return ColorLib.gray(50);
            }
        }

    }

    /**
     * Set fill colors for treemap nodes. Search items are colored in pink,
     * while normal nodes are shaded according to their depth in the tree.
     */
    private class FillColorAction extends ColorAction
    {

        private ColorMap cmap = new ColorMap(
                ColorLib.getInterpolatedPalette(10, ColorLib.rgb(0, 255, 0), ColorLib.rgb(0, 25, 0)), 0, 25);
        private Table t;
        private double threshold = 0.0;

        public FillColorAction(String group)
        {
            super(group, VisualItem.FILLCOLOR);
//			setColorMapSize(0,100);
        }

        public FillColorAction(String group, Predicate condition)
        {
            super(group, condition, VisualItem.FILLCOLOR);
        }

        /**
         * Sets the size of the color map.
         */
        public void setColorMapSize(double min, double max)
        {
//			size = Math.max(size, 2);
            threshold = min;
            cmap = new ColorMap(mPalette, threshold, max);
        }

        public int getColor(VisualItem item)
        {
            if (item instanceof NodeItem)
            {
                NodeItem nitem = (NodeItem) item;
                return getColorByScore(nitem);
            } else
            {
                return cmap.getColor(0);
            }
        }

        public int getColorByScore(NodeItem nitem)
        {
            double score = 0;
            try
            {
                if (nitem.canGetInt(mScoreName))
                {
                    score = nitem.getInt(mScoreName);
                } else
                {
                    score = t.getDouble(nitem.getRow(), mScoreName);
                }
            } catch (NumberFormatException e)
            {
                // Treat as no score
            }

            int color = CustomColorMap.CLEAR;
            int nodeLevel = nitem.getDepth();
            if (nodeLevel == mScoreLevel && score > threshold)
            {
                color = cmap.getColor(score);
            }
            return color;
        }

        @Override
        protected void setup()
        {
            t = mDataSource.getTable("SELECT " + mScoreName + " FROM " + mDataSource.getNodeTable());
            super.setup();
        }

        @Override
        protected void finish()
        {
            t = null;
            super.finish();
        }

    } // end of inner class TreeMapColorAction

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

        protected Shape getRawShape(VisualItem item)
        {
            m_bounds.setRect(item.getBounds());
            return m_bounds;
        }

    } // end of inner class NodeRenderer

}
