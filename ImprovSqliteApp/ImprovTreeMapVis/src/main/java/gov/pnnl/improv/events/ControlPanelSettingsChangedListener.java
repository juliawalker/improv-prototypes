package gov.pnnl.improv.events;

public interface ControlPanelSettingsChangedListener {

	public void settingsChanged(String setting, Object oldValue, Object newValue);
	
}
