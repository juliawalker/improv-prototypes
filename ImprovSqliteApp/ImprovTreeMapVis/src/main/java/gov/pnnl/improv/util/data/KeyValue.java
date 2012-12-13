package gov.pnnl.improv.util.data;

public class KeyValue
{
    private String mKey;
    private Object mValue;

    public KeyValue(String key, Object value)
    {
        this.mKey = key;
        this.mValue = value;
    }

    public String getKey()
    {
        return this.mKey;
    }

    public Object getValue()
    {
        return this.mValue;
    }

}
