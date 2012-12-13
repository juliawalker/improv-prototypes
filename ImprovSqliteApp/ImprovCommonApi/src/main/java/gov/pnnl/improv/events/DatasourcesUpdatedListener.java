package gov.pnnl.improv.events;

import gov.pnnl.improv.data.IDataSource;

public interface DatasourcesUpdatedListener {
	public static final int UPDATE = 3001;
	public static final int ADD = 3002;
	public static final int DELETE = 3003;
	public static final int ACTIVE_CHANGE = 3004;
	
	public void datasourcesUpdated(int updateType, IDataSource updatedDatasource);
}
