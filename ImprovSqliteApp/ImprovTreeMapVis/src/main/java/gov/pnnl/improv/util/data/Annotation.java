package gov.pnnl.improv.util.data;

/**
 * Annotation - name-value pair for the Annotation Viewer
 *
 * @author Kelly Domico, D3P056 Pacific Northwest National Laboratory
 * @since	11.13.2009
 */
public class Annotation
{

    private String mName;
    private Object mValue;

    public Annotation(String name, Object value)
    {
        this.mName = name;
        this.mValue = value;
    }

    public String getName()
    {
        return this.mName;
    }

    public Object getValue()
    {
        return this.mValue;
    }

}
