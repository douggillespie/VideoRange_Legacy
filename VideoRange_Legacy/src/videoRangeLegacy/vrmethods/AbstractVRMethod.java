package videoRangeLegacy.vrmethods;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import PamUtils.LatLong;
import PamUtils.PamUtils;
import PamView.PamSymbol;
import PamView.dialog.PamDialog;
import PamView.dialog.PamLabel;
import PamView.panel.PamPanel;
import videoRangeLegacy.LocationManager;
import videoRangeLegacy.VRCalibrationData;
import videoRangeLegacy.VRControl;
import videoRangeLegacy.VRHeightData;
import videoRangeLegacy.VRHorzCalcMethod;
import videoRangeLegacy.VRMeasurement;
import videoRangeLegacy.panels.AcceptMeasurementDialog;
import videoRangeLegacy.panels.VRPanel;
import videoRangeLegacy.panels.VRParametersDialog;
import videoRangeLegacy.panels.VRSidePanel;

/**
 * AbstractVRMethod provides a significant amount of the basic functionality required to determine the range of an animal from an image. It also contains generic components, such as calibration lists, generic instructions and image location indicators for making a vrMethod
 * @author Jamie Macaulay
 *
 */
public abstract class AbstractVRMethod implements VRMethod {
	
	protected static final int MEASURE_ANIMAL = 2;
	protected static final int MEASURE_DONE = 3;
	
	//generic components
	private PamLabel calibrationData;
	private JComboBox<VRCalibrationData> calibrations;
	protected JTextField mapText;
	protected JButton imuSettings;
	private JTextField gps;
	private JButton gpsLocSettings;
	protected VRControl vrControl; 
	
	//Horizon tilt in radians. 
	protected double horizonTilt=0;
	
	//Heading
	protected Double imageHeading;

	//measurements
	protected Point horizonPoint1, horizonPoint2;
	//possible animal measurment
	protected VRMeasurement candidateMeasurement;

	public AbstractVRMethod(VRControl vrControl){
		this.vrControl=vrControl;
	}
	
	
	/**
	 * Creates a panel containing the calibration combo box and labels. 
	 * @return
	 */
	public PamPanel createCalibrationList(){
		
		PamPanel panel=new PamPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx=0;
		c.gridwidth = 1;
		PamDialog.addComponent(panel, new PamLabel("Calibration"), c);
		c.gridx++;
		PamDialog.addComponent(panel, calibrationData = new PamLabel(""), c);
		c.gridy++;
		c.gridy++;
		c.gridx=0;
		c.gridwidth = 2;
		PamDialog.addComponent(panel, calibrations = new JComboBox<VRCalibrationData>(), c);
		calibrations.addActionListener(new SelectCalibration());
		
		return panel; 
		
	}
	
	/**
	 * Creates a panel allowing the user to select a map and view map file. 
	 * @return
	 */
	public PamPanel createMapFilePanel(){
		
		PamPanel panel=new PamPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx=0;
		c.gridy++;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, new PamLabel("Map File"), c);
		c.weightx=1;
		c.gridx=0;
		c.gridy++;
		c.gridwidth = 3;
		PamDialog.addComponent(panel, mapText=new JTextField(10), c);
		mapText.setEditable(false);
		setMapText(); 
		c.weightx=0;
		c.gridx =3;
		c.gridwidth = 1;
		PamDialog.addComponent(panel, imuSettings=new JButton(VRSidePanel.settings), c);
		imuSettings.setPreferredSize(VRPanel.settingsButtonSize);
		imuSettings.addActionListener(new SelectMap());
		
		return panel;
		
	}
	
	private class SelectMap implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			selectMap();
		}
	}
	
	private void selectMap(){
		vrControl.settingsButton(null,VRParametersDialog.SHORE_TAB);
	}
	
	protected void setMapText(){
		if (mapText==null) return;
		if (vrControl.getVRParams().shoreFile!=null) mapText.setText(vrControl.getVRParams().shoreFile.getName());
		else mapText.setText("no map file");
	}
	
	/**
	 * Create a panel to show GPS location and allow user to change GPS location method. 
	 */
	public PamPanel createLocationListPanel(){
		PamPanel panel=new PamPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx=1;
		c.gridx=0;
		c.gridy++;
		c.gridwidth = 1;
		PamDialog.addComponent(panel, new PamLabel("GPS Location"), c);
		c.gridy++;
		c.gridx=0;
		c.gridwidth = 3;
		PamDialog.addComponent(panel, gps=new JTextField(10), c);
		gps.setEditable(false);
		gps.setText("no GPS data");
		c.weightx=0;
		c.gridx =3;
		c.gridwidth = 1;
		PamDialog.addComponent(panel, gpsLocSettings=new JButton(VRSidePanel.settings), c);
		gpsLocSettings.setPreferredSize(VRPanel.settingsButtonSize);
		gpsLocSettings.addActionListener(new SelcetGPSLoc());
		return panel;
	}
	
	private class SelcetGPSLoc implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			vrControl.settingsButton(null,VRParametersDialog.CAMERA_POS);
		}
		
	}
	
	/**
	 * Get the lat long from the location mnanager for a given time. 
	 * @param timeMillis
	 * @return the co-ordinates of the camera location at timeMillis
	 */
	protected LatLong getGPSinfo(long timeMillis){
		return vrControl.getLocationManager().getLocation(timeMillis);
	}
	
	/**
	 * Get the current image co-ordinates based on the image time stamp. 
	 * @return the co-ordinates the current image was taken from. 
	 */
	protected LatLong getGPSinfo(){
		//System.out.println(vrControl.getLocationManager().getLocation(vrControl.getImageTime()));
		return vrControl.getLocationManager().getLocation(vrControl.getImageTime());
	}
	
	@Override
	public void update(int updateType){
		switch (updateType){
		case VRControl.SETTINGS_CHANGE:
			setGPSText(getGPSinfo(),gps);
			setMapText();
			newCalibration();
			enableControls();
		break;
		case VRControl.IMAGE_CHANGE:
			setGPSText(getGPSinfo(),gps);
		break;
		case VRControl.METHOD_CHANGED:
			break;
		}
	}
	
	protected void setInstruction(JLabel instruction){
		
		if (vrControl.getCurrentImage()==null){ 
			instruction.setText("No Image...");
			return;
		}
		if (vrControl.getVRParams().getCurrentheightData()==null){
			instruction.setText("Add height data...");
			return;
		}
		if (vrControl.getVRParams().getCurrentCalibrationData()==null){
			instruction.setText("Add a calibration value...");
			return;
		}
	}
	
	public void setGPSText(LatLong gpsInfo, JTextField gps){
		if (gps==null) return;
		if (gpsInfo==null){
			gps.setText("no GPS Data");
			gps.setToolTipText("no GPS Data");
			return;
		}
		if (vrControl.getLocationManager().getLastSearch()==LocationManager.NO_GPS_DATA_FOUND)  {
			gps.setText("no GPS Data");
			gps.setToolTipText("no GPS Data");
			return;
		}
		String type=vrControl.getLocationManager().getTypeString(vrControl.getLocationManager().getLastSearch());
		gps.setText(type+": "+ gpsInfo);
		gps.setToolTipText(type+": "+ gpsInfo);
		gps.setCaretPosition(0);

	}

	
	public void enableControls(){
		//TODO
	}
	
	
	/**
	 * Add calibration data to the calibration combo box;
	 */
	protected void newCalibration(){
		//TODO
		if (calibrations==null) return; 
		
		ArrayList<VRCalibrationData> calData = vrControl.getVRParams().getCalibrationDatas();
		VRCalibrationData currentSelection = vrControl.getVRParams().getCurrentCalibrationData();
		int currIndex = 0;
		calibrations.removeAllItems();

		System.out.println("calData: "+ vrControl.getVRParams().getCalibrationDatas());
		if (calData != null) {
			for (int i = 0; i < calData.size(); i++) {
				calibrations.addItem(calData.get(i));
				if (calData.get(i) == currentSelection) {
					currIndex = i;
				}
			}
			if (currIndex >= 0) {
				calibrations.setSelectedIndex(currIndex);
			}
		}
		vrControl.getVRParams().setCurrentCalibration(currentSelection);
	}

	
	void newCalibration(boolean rebuildList) {
		VRCalibrationData vcd = vrControl.getVRParams().getCurrentCalibrationData();
		if (vcd == null) return;
		if (rebuildList) newCalibration();
		calibrations.setSelectedItem(vcd);
		calibrationData.setText(String.format("%.5f\u00B0/px", vcd.degreesPerUnit));
	}
	
	
	class SelectCalibration implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			selectCalibration((VRCalibrationData) calibrations.getSelectedItem());
		}
	}
	
	public void selectCalibration(VRCalibrationData vrCalibrationData) {
		vrControl.getVRParams().setCurrentCalibration(vrCalibrationData);
		newCalibration(false);
	}
	
	///Calculations////
	/**
	 * Gets the horizon pixel number when horizon points have beenmarked. 
	 * <p>
	 * Only this one needed, since VRControl immediately works out the horizon
	 * position whenever the shore point is set or the angle changes. 
	 * @param x c coordinate on screeen
	 * @return horizon pixel number
	 */
	protected Double getHorizonPixel_H(int x) {
		Point p1 = horizonPoint1;
		Point p2 = horizonPoint2;
		if (p1 == null) {
			return null;
		}
		if (p2 == null) {
			return (double) p1.y;
		}
		// extrapolate / interpolate to allow for slanty horizon. 
		return p1.y + (double) (p2.y-p1.y) / (double) (p2.x-p1.x) * (x - p1.x);
	}
	
	/**
	 * Get's the Y coordinate of the horizon at a given X.
	 * @param x
	 * @return
	 */
	protected Double getHorizonPixel(int x) {
		Double y = null;
		y = getHorizonPixel_H(x);
		if (y == null) {
			/*
			 * Work something out based on the tilt so that the 
			 * shore line gets shown at the top of the image in any case. 
			 */
			double dx = x - (vrControl.getVRPanel().getImageWidth()/2);
			y = -dx * Math.tan(getHorizonTilt() * Math.PI / 180);
		}
		return y;
	}
	
	
	/**
	 * Gets called when a second horizon point is added and then 
	 * calculates the horizon tilt, based on the two points. 
	 *
	 */
	protected void calculateHorizonTilt() {
		if (horizonPoint1 == null ||horizonPoint2 == null) {
			return;
		}
		Point l, r;
		if (horizonPoint1.x < horizonPoint2.x) {
			l = horizonPoint1;
			r = horizonPoint2;
		}
		else {
			l = horizonPoint2;
			r = horizonPoint1;
		}
		double tilt = Math.atan2(-(r.y-l.y), r.x-l.x) * 180 / Math.PI;
		setHorizonTilt(tilt);
	}
	
	protected int bearingTox(double centreAngle, double objectBearing) {
		/*
		 * centreAngle should have been taken from the little top panel
		 * and is the bearing at the centre of the image. Work out the
		 * x pixel from the bearing difference. 
		 */
		VRCalibrationData calData = vrControl.getVRParams().getCurrentCalibrationData();
		if (calData == null) return 0;
		double ad = PamUtils.constrainedAngle(objectBearing - centreAngle, 180);
		return vrControl.getVRPanel().getImageWidth() / 2 + (int) (ad / calData.degreesPerUnit); 
	}
	
	/**
	 * Measure an animnal based on two points defining the horizon in a picture. Note this method is not just used in the direct horizon method but any method which can reconstruct the horizon, such as using shore points. 
	 * @param animalPoint
	 * @return
	 */
	protected boolean newAnimalMeasurement_Horizon(Point animalPoint) {
		
		if (horizonPoint1 == null || horizonPoint2 == null) {
			return false;
		}
		VRCalibrationData calData = vrControl.getVRParams().getCurrentCalibrationData();
		if (calData == null) {
			return false;
		}
		VRHeightData heightData = vrControl.getVRParams().getCurrentheightData();
		if (heightData == null) {
			return false;
		}
		
		VRHorzCalcMethod vrMethod = vrControl.getRangeMethods().getCurrentMethod();
		if (vrMethod == null) {
			return false;
		}
		
		if (vrControl.getMeasuredAnimals()==null){
			vrControl.setMeasuredAnimals(new ArrayList<VRMeasurement>());
		}
		

		candidateMeasurement = new VRMeasurement(horizonPoint1, horizonPoint2, animalPoint);
		
		candidateMeasurement.vrMethod=vrControl.getCurrentMethod(); 
		candidateMeasurement.imageTime = vrControl.getImageTime();
		candidateMeasurement.imageName = new String(vrControl.getImageName());
		candidateMeasurement.imageAnimal = vrControl.getMeasuredAnimals().size();
		candidateMeasurement.calibrationData = calData.clone();
		candidateMeasurement.heightData = heightData.clone();
		candidateMeasurement.rangeMethod = vrMethod;
		
		// try to get an angle
//		AngleDataUnit heldAngle = null;
//		if (angleDataBlock != null) {
//			heldAngle = angleDataBlock.getHeldAngle();
//		}
//		if (heldAngle != null) {
//			candidateMeasurement.cameraAngle = heldAngle.correctedAngle;
//		}
		/*
		 * Held angles would have been put straight into the imageAngle field. 
		 * Even if they haven't, it's possible that someone will have entered a
		 * value manually into the field, so try using it. 
		 */
		candidateMeasurement.imageBearing = getImageHeading();
		
		double angle = candidateMeasurement.getAnimalAngleRadians(calData);
		double range = vrMethod.getRange(vrControl.getCurrentHeight(), angle);
		candidateMeasurement.locDistance = range;
		// see how accurate the measurement might be based on a single pixel error
		double range1 = vrMethod.getRange(vrControl.getCurrentHeight(), angle + Math.PI/180*calData.degreesPerUnit);
		double range2 = vrMethod.getRange(vrControl.getCurrentHeight(), angle - Math.PI/180*calData.degreesPerUnit);
		double error = Math.abs(range1-range2)/2;
		candidateMeasurement.pixelAccuracy = error;
		
		// calculate the angle correction
		int imageWidth = vrControl.getVRPanel().getImageWidth();
		double angCorr = (animalPoint.x - imageWidth/2) * calData.degreesPerUnit;
		candidateMeasurement.angleCorrection = angCorr;
				
		VRMeasurement newMeasurement = AcceptMeasurementDialog.showDialog(null, vrControl, candidateMeasurement);
		if (newMeasurement != null) {
			System.out.println("new measurment!");
			vrControl.getMeasuredAnimals().add(newMeasurement);
			vrControl.getVRProcess().newVRLoc(newMeasurement);
		}
		candidateMeasurement = null;
				
		return true;
	}
	
	protected void addAnimals(Graphics g) {
		ArrayList<VRMeasurement> vrms = vrControl.getMeasuredAnimals();
		if (vrms != null) {
			for (int i = 0; i < vrms.size(); i++) {
				drawAnimal(g, vrms.get(i), VRPanel.animalSymbol.getPamSymbol());
			}
		}
		if (candidateMeasurement!= null) {
			drawAnimal(g, candidateMeasurement, VRPanel.candidateSymbol.getPamSymbol());
		}
	}
	

	protected void drawAnimal(Graphics g, VRMeasurement vr, PamSymbol symbol) {
		Point p1, p2;
		symbol.draw(g, p1 = vrControl.getVRPanel().imageToScreen(vr.animalPoint));
		p2 = vrControl.getVRPanel().imageToScreen(vr.horizonPoint);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
	}
	
	protected void drawMarksandLine(Graphics g, Point p1, Point p2, PamSymbol symbol, boolean drawLine, Point currentMouse) {

		Point sp1 = null, sp2 = null;
		if (p1 != null) {
			symbol.draw(g, sp1 = vrControl.getVRPanel().imageToScreen(p1));
		}
		if (p2 != null) {
			symbol.draw(g, sp2 = vrControl.getVRPanel().imageToScreen(p2));
			if (sp1 != null) {
				g.setColor(symbol.getLineColor());
				g.drawLine(sp1.x, sp1.y, sp2.x, sp2.y);
			}
		}
		if (sp1 != null && sp2 == null && currentMouse != null && drawLine) {
			g.setColor(symbol.getLineColor());
			g.drawLine(sp1.x, sp1.y, currentMouse.x, currentMouse.y);
		}
	}
	
	protected void clearPoints() {
		horizonPoint1 = horizonPoint2 =null;
		vrControl.setMeasuredAnimals(null);
		candidateMeasurement = null;
	}
	
	public VRMeasurement getCandidateMeasurement() {
		return candidateMeasurement;
	}
	
	public double getHorizonTilt() {
		return horizonTilt;
	}

	public void setHorizonTilt(double horizonTilt) {
		this.horizonTilt = horizonTilt;
	}
	
	public Double getImageHeading() {
		return imageHeading;
	}
	
	/**
	 * Calculates the lat long of an animal based on heading, range and the image origin. 
	 * @param vrMeasurment- the lat long is added to a the imageLatLong field in the measurment. 
	 */
	public void calcLocLatLong(VRMeasurement vrMeasurment){
		//nned three components to calcualte a latLong for the animal
		if (vrMeasurment.locBearing==null || vrMeasurment.locDistance==null || vrMeasurment.imageOrigin==null) return;
		vrMeasurment.locLatLong=vrMeasurment.imageOrigin.travelDistanceMeters(vrMeasurment.locBearing, vrMeasurment.locDistance);
	}
	
}
