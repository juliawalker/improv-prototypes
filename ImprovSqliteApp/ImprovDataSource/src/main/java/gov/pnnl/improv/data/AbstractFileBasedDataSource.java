package gov.pnnl.improv.data;

import gov.pnnl.improv.data.DataSourceType;
import gov.pnnl.improv.data.IDataSource;
import java.beans.PropertyChangeSupport;

public abstract class AbstractFileBasedDataSource implements IDataSource {
	
	PropertyChangeSupport mPropertyChangeSupport = new PropertyChangeSupport(this);
	
	@Override
	abstract public String getLocation();

	@Override
	abstract public String getName();

	@Override
	abstract public void setLocation(String location);
	
	@Override
	abstract public void setName(String newName);

	@Override
	public DataSourceType getType() {
		return DataSourceType.FILEBASED;	
	}
}
