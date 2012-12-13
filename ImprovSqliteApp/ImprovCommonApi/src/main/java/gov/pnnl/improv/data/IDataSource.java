/**
 *
 */
package gov.pnnl.improv.data;


/**
 * @author D3P056
 *
 */
public interface IDataSource
{
    public String getName();

    public void setName(String newName);

    public String getLocation();

    public void setLocation(String location);

    public String getDescription();

    public void setDescription(String newDesc);

    public DataSourceType getType();

    public void setType(DataSourceType newType);

    public void setActive(boolean active);

    public boolean isActive();

    public boolean isValid();

}
