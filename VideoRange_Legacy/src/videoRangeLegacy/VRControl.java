package videoRangeLegacy;

import java.util.List;
import java.awt.Point;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.ListIterator;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import pamScrollSystem.AbstractPamScrollerAWT;
import pamScrollSystem.PamScroller;
import videoRangeLegacy.externalSensors.AngleListener;
import videoRangeLegacy.externalSensors.IMUListener;
import videoRangeLegacy.importTideData.TideManager;
import videoRangeLegacy.panels.VRPanel;
import videoRangeLegacy.panels.VRParametersDialog;
import videoRangeLegacy.panels.VRTabPanelControl;
import videoRangeLegacy.vrmethods.AddCalibrationMethod;
import videoRangeLegacy.vrmethods.IMUMethod;
import videoRangeLegacy.vrmethods.VRHorizonMethod;
import videoRangeLegacy.vrmethods.VRLandMarkMethod;
import videoRangeLegacy.vrmethods.VRMethod;
import videoRangeLegacy.vrmethods.VRShoreMethod;
import Map.GebcoMapFile;
import Map.MapFileManager;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamControllerInterface;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamUtils.PamFileChooser;
import PamView.dialog.PamDialog;

/**
 * The videoRange module determines location information based on photographs of objects (photogrammetry). The ideal for determining the location of an animal is to know the heading, pitch, roll, focal length, GPS location and height of a camera along with the height of the tide.
 * <p>
 * However, not all these parameters are necessarily needed to gain some sort of location information. For example, tilt and pitch, focal length and height can determine the distance to animal without the need for heading information or a GPS location. 
 * <p>
 * There are various ways a user can determine required parameters, either directly from a photograph or external data. The simplest example is using the horizon to determine the pitch and tilt of the camera and manually inputting height data along with a calibration
 * (pixels per degree/rad) value. Users may want to use external IMU data, either in real time or for post processing. Geo-tagged photos are another option. 
 * <p>
 * This module is designed to provide a framework for photogrammetry. The core functionality allows users to open and view photographs/images along with any meta data. VRMethod classes are then used to provide the user interface for determining the l.ocation of an object
 * <p>
 * VRmethods use a JLayer over the photograph to allow users to select locations, draw lines etc. Each VRMethod has it's own side panel and ribbon panel to allow users to input information and select relevant. The VRMethod is responsible for calculating location information which is passed back to VRControl to be saved as a generic video range data unit. 
 *<p>
 *<p>
 *Generic Information Handled outside VRMethods (these are used by one or more VRMethods but handled solely within a VRMethod class)
 *<p>
 *GPS location of camera
 *<p>
 *Calibration Values
 *<p>
 *Landmark Locations
 *<p>
 *Tide Information
 *<p>
 *Imported Maps
 *<p>
 *External Sensor data
 *<p>
 *Photo meta data
 *
 * @author Doug Gillespie and Jamie Macaulay
 *
 */
public class VRControl extends PamControlledUnit implements PamSettings {
	
	/**
	 * Panel containing all gui components
	 */
	public VRTabPanelControl vrTabPanelControl;
	
	/**
	 * Video range paramters. Stores settings. 
	 */
	protected VRParameters vrParameters = new VRParameters();
	
	/**
	 * Different methods for determining range from image measurements. 
	 */
	protected VRHorzMethods rangeMethods;
	
	/**
	 * PamProcess for the video range module
	 */
	protected VRProcess vrProcess;
	
	/**
	 * PamProcess to listen for heading angle measurements (AngleMeasurment)
	 */
	protected AngleListener angleListener;
	
	/**
	 * PamProcess to listen for IMU measurments (IMUModule)
	 */
	protected IMUListener imuListener;
			
	/**
	 * Manages map information for VR Methods
	 */
	protected MapFileManager mapFileManager;
	
	/**
	 * Manages TIDE information for VR Methods
	 */
	protected TideManager tideManager;
	
	/*
	 *update flags 
	 */
	/**
	 * Some settings in the vrparams may have changed
	 */
	public static final int SETTINGS_CHANGE = 0;
	/**
	 * The image has changed
	 */
	public static final int IMAGE_CHANGE=1; 
	/**
	 * The scale of the image has been changed.
	 */
	public static final int IMAGE_SCALE_CHANGE=2; 
	/**
	 * The time of the image has been changed.
	 */
	public static final int IMAGE_TIME_CHANGE=3; 
	/**
	 * The heading of the image has been changed (usually manually).
	 */
	public static final int HEADING_UPDATE=4;
	/**
	 * The pitch of the image has been changed (usually manually)
	 */
	public static final int PITCH_UPDATE=5; 
	/**
	 * The roll of the image has been changed (usually manually), note: roll is often referred to as tilt. 
	 */
	public static final int TILT_UPDATE=6; 
	/**
	 * The vrMethod has changed
	 */
	public static final int METHOD_CHANGED=7;

	/*
	 * flags for general status of what we're trying to do
	 */
	public static final int NOIMAGE = -1;
	
	public static final int DBCOMMENTLENGTH = 50;
		
	private int vrSubStatus = -1;
	
	/**
	 * An array of animal measurments. This holds all measurments for the current image. When a new image is selected this is set to null. 
	 */
	private ArrayList<VRMeasurement> measuredAnimals;
	
	/**
	 * List of methods used to determine location information from an image. 
	 */
	private ArrayList<VRMethod> vrMethods;

	/**
	 * The currently selected method. 
	 */
	private VRMethod currentVRMethod;

	/**
	 * The currently selected image. Null if no image is currently loaded. 
	 */
	private PamImage currentImage;

	/**
	 * Manager for determining the location of the camera, deals with GPS data, image geo tags etc. 
	 */
	private LocationManager locationManager;

	private PamScroller vrScroller;
	
	/**
	 * The load time for external data either side of the image in millis
	 */
	private static long loadTime=10*60*1000;


	public VRControl(String unitName) {

		super("Video Range Measurement", unitName);
		
		locationManager=new LocationManager(this);
		
		PamSettingManager.getInstance().registerSettings(this);
		
		mapFileManager = new GebcoMapFile();
		
		tideManager = new TideManager(this);
		
		vrMethods=createMethods();
		
		currentVRMethod=vrMethods.get(0);

		vrTabPanelControl = new VRTabPanelControl(this);

		setTabPanel(vrTabPanelControl);

//		setSidePanel(vrSidePanel = new VRSidePanel(this));
		
		rangeMethods = new VRHorzMethods(this);

		addPamProcess(vrProcess = new VRProcess(this));
		//process for input of compass bearings
		addPamProcess(angleListener = new AngleListener(this));
		//process for input of IMU data
		addPamProcess(imuListener = new IMUListener(this));
	
		update(SETTINGS_CHANGE);
				
		//load map file
		mapFileManager.readFileData(vrParameters.shoreFile, false);
		
		//load tide data
		tideManager.savePolpredTextFile(vrParameters.currTideFile);
		
		//create a scroller which we never display but helps loads viewer data. 
		vrScroller=new PamScroller("vrScroller", AbstractPamScrollerAWT.HORIZONTAL, 1000, 360000, false);

	}
	
	 
	
	
	/**
	 * This is where to define all the methods which can be used by the module.
	 * @return list of all the vr methods. 
	 */
	private ArrayList<VRMethod> createMethods(){
		ArrayList<VRMethod> vrMethods= new ArrayList<VRMethod>();
		vrMethods.add(new VRHorizonMethod(this));
		vrMethods.add(new VRShoreMethod(this));
		vrMethods.add(new AddCalibrationMethod(this));
		vrMethods.add(new VRLandMarkMethod(this));
		vrMethods.add(new IMUMethod(this));
		return vrMethods;	
	}
	
	/**
	 * Get an arraylist of all the vr methods currently available.
	 * @return
	 */
	public ArrayList<VRMethod> getMethods() {
		return vrMethods;
	}
	
	/**
	 * Set the current method. 
	 * @param method- integer specifying method position in vrMethods ArrayList. 
	 */
	public void setVRMethod(int method) {
		this.currentVRMethod=vrMethods.get(method);
		update(METHOD_CHANGED); 
	}
	
	/**
	 * Get the current vr Method. 
	 * @return
	 */
	public VRMethod getCurrentMethod(){
		return currentVRMethod;
	}
	
	/**
	 * Get the calibration method. Some dialogs require this.
	 * @return the AddCalibration vrmethod
	 */
	public AddCalibrationMethod getCalibrationMethod() {
		if (getCurrentMethod() instanceof AddCalibrationMethod) return (AddCalibrationMethod) getCurrentMethod();
		else{
			for (int i=0; i<vrMethods.size(); i++ ){
				if (vrMethods.get(i) instanceof AddCalibrationMethod) return (AddCalibrationMethod) vrMethods.get(i);
			}
		}
		return null;
	}
	

	/**
	 * Opens the settings dialog and updates the module if new settings are saved. 
	 * @param frame
	 * @param tab- the tab to open the settings dialog on. 
	 */
	public void settingsButton(JFrame frame, int tab) {
		if (frame == null) {
			frame = getPamView().getGuiFrame();
		}
		VRParameters newParams = VRParametersDialog.showDialog(frame, this, tab);
		if (newParams != null) {
			vrParameters = newParams.clone();
			update(SETTINGS_CHANGE);
		}
	}
	
	/**
	 * Pastes an image from the computers clipboard. 
	 */
	public void pasteButton() {
		if (vrTabPanelControl.pasteImage()) {
			update(IMAGE_CHANGE);
		}
	}

	/**
	 * Opens a file browser and loads the selected image/folder. Also, looks at the folder the image is in and determines the number of image files. 
	 */
	public void fileButton() {
		
		ImageFileFilter filefilter=new ImageFileFilter();
		
		JFileChooser fileChooser = new PamFileChooser();
		fileChooser.setFileFilter(filefilter);
		fileChooser.setCurrentDirectory(vrParameters.imageDirectory);
		fileChooser.setDialogTitle("Select image...");
		//fileChooser.addChoosableFileFilter(filter);
		fileChooser.setFileHidingEnabled(true);
		fileChooser.setApproveButtonText("Select");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		javax.swing.filechooser.FileFilter[] filters = fileChooser
		.getChoosableFileFilters();
		for (int i = 0; i < filters.length; i++) {
			fileChooser.removeChoosableFileFilter(filters[i]);
		}
		fileChooser.setFileFilter(filefilter);

		int state = fileChooser.showOpenDialog(vrTabPanelControl.getVRSidePanel().getPanel());
		
		if (state == JFileChooser.APPROVE_OPTION) {
			vrParameters.currShoreFile=null;
			
			//check if this is a folder in which case take the first image in that folder. 
			if (fileChooser.getSelectedFile().isDirectory()){
				File[] files=fileChooser.getSelectedFile().listFiles();
				for (int i=0; i<files.length; i++){
					if (filefilter.accept(files[i])){
						vrParameters.currShoreFile=files[i];
						vrParameters.imageDirectory = fileChooser.getSelectedFile();
						break;
					}
				}
				
			}
			else {
				vrParameters.currShoreFile = fileChooser.getSelectedFile();
				vrParameters.imageDirectory = fileChooser.getCurrentDirectory();
			}
			
			if (vrParameters.currShoreFile==null) {
				PamDialog.showWarning(this.getPamView().getGuiFrame(), "No image found", "The folder selected did not contain any compatible images");
				return;
			}
			//System.out.println(currFile);
			loadFile(vrParameters.currShoreFile);
		}
		update(IMAGE_CHANGE);
	}
	
	/**
	 * Find the next file which is approved by the Image file filter in the folder. Returns null if no file is found.  
	 * @param file- current file selected in the directory. If null the first file in the directory is chosen. 
	 * @param filefilter
	 * @param forward- true move forward in the folder, false move backward in the folder. 
	 * @return
	 */
	public File findNextFile(File file, ImageFileFilter filefilter, boolean forward){
		
		File directory=new File(file.getParent());
		if (!directory.isDirectory()) return null; //something seriously wrong
		
		File[] files=directory.listFiles();
		List<File> fileList = Arrays.asList(files);
		ListIterator<File> listIterator = fileList.listIterator();
		//find the file position; 
		boolean fileFound=false;
		for (int i=0; i<files.length; i++){
			if (files[i].getAbsolutePath().equals(file.getAbsolutePath())){
				fileFound=true;
				break;
			}
			listIterator.next();
		}
		
		if (!fileFound) return null;
		if (forward && !listIterator.hasNext()) return null;
		if (!forward && !listIterator.hasPrevious()) return null;
		
		//now we have index of file and list iterator in correct position; 
		File listFile=null; 
		File newFile=null; 
		boolean iterate=true;
		
		while (iterate==true){
			if (forward) {
				listFile=listIterator.next();
				iterate=listIterator.hasNext();
			}
			else {
				listFile=listIterator.previous();
				iterate=listIterator.hasPrevious();
			}
	
			if (filefilter.accept(listFile) && !listFile.getAbsolutePath().equals(file.getAbsolutePath()) && !listFile.isDirectory()){
				System.out.println("file accepted: "+listFile.getAbsoluteFile() +"  "+ file.getAbsolutePath() );
				newFile=listFile;
				iterate=false;
			}
			
		}
		return newFile;
	}		
	
	/**
	 * Force the video range tab to be selected.
	 */
	void showVRTab() {
		PamController.getInstance().showControlledUnit(this);
	}

	/**
	 * Set the current selected height
	 * @param heightIndex
	 */
	public void selectHeight(int heightIndex) {
		vrParameters.setCurrentHeightIndex(heightIndex);
		update(SETTINGS_CHANGE);
	}
	
	/**
	 * Get the  current height of the camera from sea level. Includes any offset calulated by the tide manager. 
	 * @return the current height oif the camera from sea level- (this is fiorm the current sea level)
	 */
	public double getCurrentHeight(){
		if (currentImage==null) return vrParameters.getCameraHeight();
		if (currentImage.getTimeMilliseconds()==0) return vrParameters.getCameraHeight();
		return vrParameters.getCameraHeight()+tideManager.getHeightOffset(currentImage.getTimeMilliseconds());
	}
	
	/**
	 * Se the image brightness
	 * @param brightness
	 * @param contrast
	 */
	public void setImageBrightness(float brightness, float contrast) {
		vrTabPanelControl.getVRPanel().setImageBrightness(brightness, contrast);
	}

	/**
	 * Load an image file
	 * @param file- image file. 
	 */
	public void loadFile(File file) {
		long time1=System.currentTimeMillis();
		if (vrTabPanelControl.loadFile(file)) {
			//now set the scroller to move to to the correct imu or angle data;
			if (currentImage.getTimeMilliseconds()!=0 && !checkViewLoadTime(currentImage.getTimeMilliseconds())){
				vrScroller.setRangeMillis(currentImage.getTimeMilliseconds()-loadTime, currentImage.getTimeMilliseconds()+loadTime, true);
			}
		}
		long time2=System.currentTimeMillis();
		System.out.println("vrControl.loadFile: time to loadfile: "+(time2-time1));
	}
	
	/**
	 * Check that the IMU or angle data is within the load limits. 
	 */
	private boolean checkViewLoadTime(long timeMillisPic){
		if (timeMillisPic==0) return true;  
		if (imuListener.getIMUDataBlock()!=null){
			if (timeMillisPic>imuListener.getIMUDataBlock().getCurrentViewDataStart() && timeMillisPic<imuListener.getIMUDataBlock().getCurrentViewDataEnd() ){
				return true; 
			}
		}
		if (angleListener.getAngleDataBlock()!=null){
			if (timeMillisPic>angleListener.getAngleDataBlock().getCurrentViewDataStart() && timeMillisPic<angleListener.getAngleDataBlock().getCurrentViewDataEnd() ){
				return true; 
			}
		}
		return false;
	}

	public Serializable getSettingsReference() {
		return vrParameters;
	}

	public long getSettingsVersion() {
		return VRParameters.serialVersionUID;
	}

	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {	
		vrParameters = ((VRParameters) pamControlledUnitSettings.getSettings()).clone();
		return true;
	}
	
	/**
	 * Updates the VRControl depending on flag and passes update flag to the VRTabPanel (gui) and the VRMethods. 
	 * @param updateType
	 */
	public void update(int updateType) {
		
		switch (updateType){
		case SETTINGS_CHANGE:
			rangeMethods.setCurrentMethodId(vrParameters.rangeMethod);
			sortExtAngleSource();
			//angleListener.sortAngleMeasurement();
		break;
		case METHOD_CHANGED:
			measuredAnimals=null;
		break;
		case IMAGE_CHANGE:
			currentVRMethod.clearOverlay(); 
			if (currentImage.getImage() != null) {
				showVRTab();
				if (!currentImage.imageOK()) currentImage=null;
			}
		break;
		}
	
		//update other panels. 
		vrTabPanelControl.update(updateType);
		
		//update methods
		for (int i=0; i<vrMethods.size(); i++){
			vrMethods.get(i).update(updateType);
		}
	}

	@Override
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		if (changeType == PamControllerInterface.ADD_CONTROLLEDUNIT || changeType == PamControllerInterface.REMOVE_CONTROLLEDUNIT) {
			angleListener.sortAngleMeasurement();
			imuListener.sortIMUMeasurement();
		}
		if (changeType == PamControllerInterface.HYDROPHONE_ARRAY_CHANGED) {
			update(SETTINGS_CHANGE);
		}
		if (changeType == PamControllerInterface.DATA_LOAD_COMPLETE) {
			update(SETTINGS_CHANGE);
		}
		if (changeType == PamControllerInterface.INITIALIZATION_COMPLETE ){
			vrScroller.addDataBlock(imuListener.getIMUDataBlock());
			vrScroller.addDataBlock(angleListener.getAngleDataBlock());
		}
	}
	
	/**
	 * Sorts out which listener to currently use. Depends on the current datablock stored in vrParams. 
	 */
	private void sortExtAngleSource(){
		angleListener.sortAngleMeasurement();
		imuListener.sortIMUMeasurement();
	}

	public int getVrSubStatus() {
		return vrSubStatus;
	}
	
	public void newMousePoint(Point mousePoint) {
		// called from mousemove.
		vrTabPanelControl.getVRSidePanel().newMousePoint(mousePoint);
	}

	public String getImageName() {
		return currentImage.getName();
	}
	

	public String getImageTimeString() {
		return currentImage.getTimeString();
	}

	public ArrayList<VRMeasurement> getMeasuredAnimals() {
		return measuredAnimals;
	}

	public VRParameters getVRParams() {
		return vrParameters;
	}

	public VRPanel getVRPanel() {
		return vrTabPanelControl.getVRPanel();
	}


	public VRProcess getVRProcess() {
		return vrProcess;
	}
	
	public void setMeasuredAnimals(ArrayList<VRMeasurement> measuredAnimals) {
		this.measuredAnimals=measuredAnimals;
	}
	
	public VRHorzMethods getRangeMethods() {
		return rangeMethods;
	}


	public void setRangeMethods(VRHorzMethods rangeMethods) {
		this.rangeMethods = rangeMethods;
	}


	public MapFileManager getMapFileManager() {
		return mapFileManager;
	}
	
	public LocationManager getLocationManager(){
		return locationManager;
	}
	
	public TideManager getTideManager() {
		return tideManager;
	}

	public void setCurrentImage(PamImage image) {
		this.currentImage=image; 
		if (currentImage!=null){
			vrParameters.currShoreFile=currentImage.getImageFile();
		}
		else {
			vrParameters.currShoreFile=null;
		}
	}

	public PamImage getCurrentImage() {
		return currentImage;
	}
	
	public VRTabPanelControl getVRTabPanel(){
		return vrTabPanelControl;
	}

	public long getImageTime() {
		if (currentImage==null) return -1; 
		return currentImage.getTimeMilliseconds();
	}

	public Date getImageDate() {
		if (currentImage==null) return null; 
		return currentImage.getDate();
	}

	public  IMUListener getIMUListener() {
		return imuListener;
	}




}
