package gov.pnnl.improv.util.data;

/**
 * Class for bundling field name and hierarchy level as a single map key.
 *
 * @author Grant Nakamura, April 2009
 */
public class FieldLevel
{

    private String fieldName;
    private int hierarchyLevel;

    public FieldLevel(String fieldName, int hierarchyLevel)
    {
        super();

        this.fieldName = fieldName;
        this.hierarchyLevel = hierarchyLevel;
    }

    public int hashCode()
    {
        int hash = (fieldName + hierarchyLevel).hashCode();
        return hash;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof FieldLevel))
        {
            return false;
        }

        FieldLevel that = (FieldLevel) other;
        return (this.hierarchyLevel == that.hierarchyLevel && this.fieldName.equals(that.fieldName));
    }

}
