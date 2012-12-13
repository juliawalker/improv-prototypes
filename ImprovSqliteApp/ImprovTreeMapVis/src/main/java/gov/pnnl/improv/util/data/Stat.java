package gov.pnnl.improv.util.data;

public class Stat
{
    private String key;
    private Object value;

    public Stat(String key, Object value)
    {
        this.key = key;
        this.value = value;
    }

    public String getKey()
    {
        return this.key;
    }

    public Object getValue()
    {
        return this.value;
    }

}
