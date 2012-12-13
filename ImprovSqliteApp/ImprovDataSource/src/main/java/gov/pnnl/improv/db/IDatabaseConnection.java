package gov.pnnl.improv.db;

import prefuse.data.Table;

public interface IDatabaseConnection
{
    public void connect(String relativeDbFilePath) throws Exception;
	public void connect(String relativeDbFilePath, String username, String password) throws Exception;
	public void disconnect() throws Exception;
	public Table getData(String query) throws Exception;
	public void loadData(Table t, String query) throws Exception;
}

