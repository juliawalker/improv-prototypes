/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.pnnl.improv.datasource.ui;

/**
 *
 * @author D3X924
 */
public interface IReloadableDataSource
{
    public void reload() throws Exception;
    public void setActive(String name) throws Exception;
    
}
