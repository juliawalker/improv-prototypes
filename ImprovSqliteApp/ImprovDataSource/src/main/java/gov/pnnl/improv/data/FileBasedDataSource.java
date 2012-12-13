package gov.pnnl.improv.data;

import gov.pnnl.improv.data.DataSourceType;
import gov.pnnl.improv.data.IDataSource;


public class FileBasedDataSource implements IDataSource {

	private DataSourceType mType;
	private String mName;
	private String mLocation;
	private String mDescription;
	private boolean mActive;
	
	public FileBasedDataSource(String name, String location, String description) {
		this.mName = name;
		this.mLocation = location;
		this.mDescription = description;
		this.mActive = false;
		this.mType = DataSourceType.FILEBASED;
	}
	
	@Override
	public String getLocation() {		
		return this.mLocation;
	}

	@Override
	public String getName() {
		return this.mName;
	}

	@Override
	public DataSourceType getType() {
		return DataSourceType.FILEBASED;
	}

	@Override
	public void setLocation(String location) {
		this.mLocation = location;
	}

	@Override
	public void setName(String newName) {
		this.mName = newName;
	}

	@Override
	public String getDescription() {
		return this.mDescription;
	}

	@Override
	public void setDescription(String newDesc) {
		this.mDescription = newDesc;		
	}
	
	@Override
	public void setActive(boolean active) {
		this.mActive = active;
	}

	@Override
	public boolean isActive() {	
		return this.mActive;
	}

	@Override
	public void setType(DataSourceType newType) {
				
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}	

}
