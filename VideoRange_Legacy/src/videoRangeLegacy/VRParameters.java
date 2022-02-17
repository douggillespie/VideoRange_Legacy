package videoRangeLegacy;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class VRParameters implements Serializable, Cloneable {

	public static final long serialVersionUID = 0;
	
	public File imageDirectory;
		
	public static final int IMAGE_SCROLL = 0;
	public static final int IMAGE_CROP = 1;
	public static final int IMAGE_SHRINK = 2;
	public static final int IMAGE_STRETCH = 3;
	public static final int IMAGE_SHRINKORSTRETCH = 4;
	public static final int MOUSE_WHEEL_CONTROL=5;
	public int imageScaling = IMAGE_SCROLL;
	public static String[] scaleNames = {"Scroll", "Crop to window", "Shrink to fit"};
	public static String[] shortScaleNames = {"Scroll", "Crop", "Shrink"};

	public boolean maintainAspectRatio = true;
	
	/**
	 * List of camera calibration values
	 */
	private ArrayList<VRCalibrationData> calibrationDatas;
	private int currentCalibrationIndex = 0; 
	
	/**
	 * List of camera heights
	 */
	private ArrayList<VRHeightData> heightDatas;
	private int currentHeightIndex = 0;
	
	/**
	 * List of Land marks. 
	 */
	private ArrayList<LandMarkGroup> landMarks;
	private int currentLandMarkIndex=0; 
	
	/**
	 * List of manually inputed GPS locations
	 */
	private LandMarkGroup manualGPS;
	private int currentManualGPSIndex=0;
		
	public int rangeMethod = VRHorzMethods.METHOD_ROUND;
	
	public boolean drawTempHorizon = true;
	
	public boolean measureAngles;
	
	public String angleDataBlock;
	
	/**
	 * file name for the current image. Null is pasted image. 
	 */
	public File currShoreFile;

	/**
	 * file name for shore data (Gebco format)
	 */
	public File shoreFile;
	
	/**
	 * file name for tide data (POLPRED format)
	 */
	public File currTideFile;
	
	/**
	 * Ignore the closest segment (ie. if operating from on shore) 
	 */
	public boolean ignoreClosest = true;
	
	/** 
	 * draw the shore line on the map
	 */
	public boolean showShore = true;
	
	/**
	 * also highlight points on the map file (to check resolution). 
	 */
	public boolean showShorePoints = true;


	public VRCalibrationData getCurrentCalibrationData() {
		if (currentCalibrationIndex < 0) {
			return null;
		}
		if (calibrationDatas == null || calibrationDatas.size() <= currentCalibrationIndex) {
			return null;
		}
		return calibrationDatas.get(currentCalibrationIndex);
	}
	
	public void setCurrentCalibration(VRCalibrationData currentCalibration) {
		if (currentCalibration == null) {
			return;
		}
		addCalibrationData(currentCalibration);
		currentCalibrationIndex = calibrationDatas.indexOf(currentCalibration);
	}
	
	public int findCalibrationData(VRCalibrationData aCalibration) {
		if (calibrationDatas == null) {
			return -1;
		}
		return calibrationDatas.indexOf(aCalibration);
	}
	
	public void addCalibrationData(VRCalibrationData aCalibration) {
		if (aCalibration == null) {
			return;
		}
		if (calibrationDatas == null) {
			calibrationDatas = new ArrayList<VRCalibrationData>();
		}
		if (findCalibrationData(aCalibration) < 0) {
			calibrationDatas.add(aCalibration);
		}
	}
	
	public void removeCalibrationData(VRCalibrationData aCalibration) {
		if (calibrationDatas == null) {
			return;
		}
		VRCalibrationData curData = getCurrentCalibrationData();
		calibrationDatas.remove(aCalibration);
		if (findCalibrationData(curData) < 0) {
			// have just deleted the current calibration !
			currentCalibrationIndex = 0;
		}
	}
	
	public double getCameraHeight() {
		if (heightDatas == null || heightDatas.size() == 0) {
			return 0;
		}
		if (currentHeightIndex < 0 || currentHeightIndex >= heightDatas.size()) {
			return 0;
		}
		return heightDatas.get(currentHeightIndex).height;
	}

	@Override
	public VRParameters clone() {
		try {
			VRParameters newParams = (VRParameters) super.clone();
			if (calibrationDatas != null) {
				newParams.calibrationDatas = (ArrayList<VRCalibrationData>) calibrationDatas.clone();
			}
			if (heightDatas != null) {
				newParams.heightDatas = (ArrayList<VRHeightData>) heightDatas.clone();
			}
			return newParams;
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public ArrayList<VRCalibrationData> getCalibrationDatas() {
		return calibrationDatas;
	}

	public void setCalibrationDatas(ArrayList<VRCalibrationData> calibrationDatas) {
		this.calibrationDatas = calibrationDatas;
	}

	public int getCurrentCalibrationIndex() {
		return currentCalibrationIndex;
	}

	public void setCurrentCalibrationIndex(int currentCalibrationIndex) {
		this.currentCalibrationIndex = currentCalibrationIndex;
	}

	public ArrayList<VRHeightData> getHeightDatas() {
		if (heightDatas == null) {
			heightDatas = new ArrayList<VRHeightData>();
		}
		return heightDatas;
	}

	public void setHeightDatas(ArrayList<VRHeightData> heightDatas) {
		if (heightDatas == null) {
			heightDatas = new ArrayList<VRHeightData>();
		}
		this.heightDatas = heightDatas;
	}

	public int getCurrentHeightIndex() {
		return currentHeightIndex;
	}

	public void setCurrentHeightIndex(int currentHeightIndex) {
		this.currentHeightIndex = currentHeightIndex;
	}
	
	public VRHeightData getCurrentheightData() {
		if (heightDatas == null) {
			return null;
		}
		if (currentHeightIndex < 0 || currentHeightIndex >= heightDatas.size()) {
			return null;
		}
		return heightDatas.get(currentHeightIndex);
	}

	public boolean getShowShore() {
		return showShore;
	}

	public ArrayList<LandMarkGroup> getLandMarkDatas() {
		return landMarks;
	}
	
	public void  setLandMarkDatas( ArrayList<LandMarkGroup> landMarks) {
		this.landMarks=landMarks;
	}
	
	public void setCurrentLandMarkGroupIndex(int currentlySelected) {
		this.currentLandMarkIndex=currentlySelected;		
	}
	
	public int getCurrentLandMarkGroupIndex() {
		return currentLandMarkIndex;
	}

	public LandMarkGroup getManualGPSDatas() {
		return manualGPS;
	}

	public int getCurrentManualGPSIndex() {
		return currentManualGPSIndex;
	}

	public void setGPSLocData(LandMarkGroup localGPSMarkList) {
		this.manualGPS=localGPSMarkList;
	}

	public void setGPSLocDataSelIndex(int currentlySelected) {
		this.currentManualGPSIndex=currentlySelected;
	}
	
}
