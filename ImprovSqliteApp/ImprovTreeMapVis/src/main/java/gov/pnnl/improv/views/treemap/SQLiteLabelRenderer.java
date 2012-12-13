package gov.pnnl.improv.views.treemap;

import gov.pnnl.improv.datasource.AbstractSQLiteDataSource;
import prefuse.render.LabelRenderer;
import prefuse.visual.VisualItem;


public class SQLiteLabelRenderer extends LabelRenderer
{

    private AbstractSQLiteDataSource mDatasource;
    private String mLabel;
    private boolean mVisible;

    public SQLiteLabelRenderer(AbstractSQLiteDataSource datasource, String label, boolean visible)
    {
        this.mLabel = label;
        this.mDatasource = datasource;
        this.mVisible = visible;
    }

    public void setVisible(boolean visible)
    {
        this.mVisible = visible;
    }

    @Override
    protected String getText(VisualItem vi)
    {
        if (this.mVisible)
        {
            String q = "SELECT " + this.mLabel + " FROM " + this.mDatasource.getNodeTable() + " WHERE id=" + vi.getInt("id");
            String labelText = ((Object[]) this.mDatasource.rawSqlSelect(q)[0])[0].toString();
            return labelText;
        } else
        {
            return null;
        }
    }

}
