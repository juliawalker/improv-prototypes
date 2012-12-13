package gov.pnnl.improv.events;

import gov.pnnl.improv.data.IDataSource;


public interface ActiveDatasourceChangedListener {
	public void activeDatasourceChanged(IDataSource datasource);
}
