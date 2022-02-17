package videoRangeLegacy.vrmethods;

import javax.swing.plaf.LayerUI;

import PamView.panel.PamPanel;

public interface VRMethod {
	
	/**
	 * The name of this type of analysis method. 
	 * @return
	 */
	public String getName();
	
	/**
	 * Different video range methods will have different controls in the side panel. This returns the side panel unique to the method. May return null if no panel is needed. 
	 * @return
	 */
	public PamPanel getSidePanel();
	
	/***
	 * Get the overlay panel for the picture unique to this method. In general this will be a translucent panel which will be placed over the picture and will include the interactions for the method. Layer Ui's can be used for a variety of purposes, including adding wait animations etc.  
	 * @return
	 */
	public LayerUI getJLayerOverlay();
	
	/**
	 * Clears all user ineractions and resets overlay. Required for changes to settings, picture being changed and other interactions not directly associated with the VRMethod. 
	 */
	public void clearOverlay();
	
	/**
	 * Some methods may require a panel above the picture for manual input or to view current information. getRibbonPanel() returns the panel unique to this method which may go above the picture. May return null if no panel is needed. 
	 * @return
	 */
	public PamPanel getRibbonPanel();
	
	/**
	 * Different video range methods will have different settings. This panel provide the vr specific settings in the vr settings dialog panel. 
	 * @return
	 */
	public PamPanel getSettingsPanel();
	
	/**
	 * Called from other parts of the module whenever a method panel may needed updated e.g. when new calibration data is manually added in the settings dialog. 
	 * @param updateType
	 */
	public void update(int updateType);

}