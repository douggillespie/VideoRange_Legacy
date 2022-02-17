package videoRangeLegacy.vrmethods;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.plaf.LayerUI;

import angleMeasurement.AngleDataUnit;
import videoRangeLegacy.VRControl;
import videoRangeLegacy.VRMeasurement;
import videoRangeLegacy.panels.AcceptMeasurementDialog;
import videoRangeLegacy.panels.VRPanel;
import videoRangeLegacy.panels.VRParametersDialog;
import videoRangeLegacy.panels.VRSidePanel;
import IMU.IMUDataBlock;
import PamUtils.PamArrayUtils;
import PamView.PamColors;
import PamView.PamSymbol;
import PamView.dialog.PamDialog;
import PamView.dialog.PamLabel;
import PamView.panel.PamPanel;

/**
 * Uses sensor information, heading, pitch and roll, plus GPS co-ordinates and height to calculated the position of an animal. Also needs calibration data.
 * Note-Sensor convention
 *  <p>
 * Bearing- 0==north, 90==east 180=south, 270==west
 * <p>
 * Pitch- 90=-g, 0=0g, -90=g
 * <p>
 * Tilt 0->180 -camera turning towards left to upside down 0->-180 camera turning right to upside down
 * <p>
 * All angles are in RADIANS. 
 * @author Jamie Macaulay
 *
 */
public  class IMUMethod extends AbstractVRMethod  {

	//components
	private JButton clearLM;
	private PamPanel sidePanel;
	private IMUMarkMethodUI imuLayerOverlay;
	private PamLabel instruction;
	private ImageRotRibbonPanel ribbonPanel;
	private JTextField imuDatText; 
	//labels showing the Euler angles of the image. 
	private JLabel imageHeadingLabel;
	private JLabel imagePitchLabel;
	private JLabel imageTiltLabel;

	
	//current status 
	private int currentStatus=MEASURE_ANIMAL;

	/**
	 * Temporary data unit to hold IMU data. Note that the angles stored in this data unit include any offsets. 
	 */
	private AngleDataUnit currentIMUData=null;

	
	//the interval to average imu units of (milliseconds)
	private static long searchInterval=1000; 


	public IMUMethod(VRControl vrControl) {
		super(vrControl);
		this.sidePanel=createSidePanel();
		this.imuLayerOverlay=new IMUMarkMethodUI(vrControl);
		this.ribbonPanel=new ImageRotRibbonPanel(vrControl);
	}

	@Override
	public String getName() {
		return "IMU Data";
	}

	@Override
	public PamPanel getSidePanel() {
		return sidePanel;
	}
	
	
	public PamPanel createSidePanel(){
		PamLabel lab;
		PamPanel panel=new PamPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx=0;
		c.gridy=0;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, lab=new PamLabel("IMU Method "), c);
		lab.setFont(PamColors.getInstance().getBoldFont());

		c.gridx=0;
		c.gridy++;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, createCalibrationList(), c);
		
		c.gridx=0;
		c.gridy++;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, createIMUImportPanel(), c);
		

		c.gridx=0;
		c.gridy++;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, imageHeadingLabel=new JLabel(), c);

		c.gridx=0;
		c.gridy++;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, imagePitchLabel=new JLabel(), c);

		c.gridx=0;
		c.gridy++;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, imageTiltLabel=new JLabel(), c);

		c.insets = new Insets(15,0,0,0);  //top padding

		c.gridy++;
		PamDialog.addComponent(panel, instruction = new PamLabel("...."), c);
		instruction.setFont(PamColors.getInstance().getBoldFont());
		
		c.insets = new Insets(0,0,0,0);  //no padding


		c.gridx=0;
		c.gridy++;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, clearLM = new JButton("Clear Marks"), c);
		clearLM.addActionListener(new ClearMarks());
		c.gridy++;
		c.gridy++;
	
		newCalibration();
		setIMUText(); 
		
		return panel;
	}
	
	private PamPanel createIMUImportPanel(){
		
		PamPanel panel=new PamPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=0;
		c.gridy++;
		PamDialog.addComponent(panel, new PamLabel("IMU File"), c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridy++;
		c.gridwidth = 3;
		PamDialog.addComponent(panel, imuDatText=new JTextField(10), c);
		imuDatText.setEditable(false);
		imuDatText.setText("No IMU Data: ");
		c.weightx = 0;
		c.gridx =3;
		c.gridwidth = 1;
		PamDialog.addComponent(panel, imuSettings=new JButton(VRSidePanel.settings), c);
		imuSettings.setPreferredSize(VRPanel.settingsButtonSize);
		imuSettings.addActionListener(new SelectIMUData());
		
		return panel;
	}
	
	/**
	 * Set the text for the number of IMU data units found for the image and the average values of these measurments. 
	 */
	private void setIMUText() {
		if (currentIMUData==null){ 
			imuDatText.setText("No IMU Data: ");
			imageHeadingLabel.setText("Heading: "+"-"+(char) 0x00B0);
			imagePitchLabel.setText("Pitch:   "+"-"+(char) 0x00B0);
			imageTiltLabel.setText("Tilt:    "+"-"+(char) 0x00B0);
		}
		else{ 
			imuDatText.setText("Found: "+ currentIMUData.getNUnits()+" IMU units in" + vrControl.getIMUListener().getIMUDataBlock().getLoggingName());
			imageHeadingLabel.setText("Heading: "+Math.toDegrees(currentIMUData.getTrueHeading())+(char) 0x00B0);
			imagePitchLabel.setText("Pitch:   "+Math.toDegrees(currentIMUData.getPitch())+(char) 0x00B0);
			imageTiltLabel.setText("Tilt:    "+Math.toDegrees(currentIMUData.getTilt())+(char) 0x00B0);
		}
		imuDatText.setCaretPosition(0);
	}

	private class SelectIMUData implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			vrControl.settingsButton(null,VRParametersDialog.ANGLE_TAB);
		}
	}
	
	private class ClearMarks implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			clearOverlay() ;
		}
	}
	
	/**
	 * JLayer overlay panel for the horizon method. 
	 * @author Jamie Macaulay
	 *
	 */
	@SuppressWarnings("serial")
	private class IMUMarkMethodUI extends VRAbstractLayerUI {
		
		private VRControl vrControl;

		public IMUMarkMethodUI(VRControl vRControl){
			super(vRControl);
			this.vrControl=vRControl; 
		}
		
		 @Override
		 public void paint(Graphics g, JComponent c) {
		    super.paint(g, c);
		    addMeasurementMarks(g);
			addAnimals(g);
		}


		 @Override
			public void mouseClick(Point mouseClick) {
				super.mouseClick(mouseClick);
				switch (currentStatus) {
				case MEASURE_ANIMAL:
					measureAnimal(mouseClick);
					break; 

				}
				vrControl.getVRPanel().repaint();
		}
		
	}
	
	private void measureAnimal(Point imPoint){
		VRMeasurement possibleMeasurement=new VRMeasurement(imPoint);
		newAnimalMeasuremnt_IMU(imPoint, possibleMeasurement);
		if (vrControl.getMeasuredAnimals()!=null){
			if (vrControl.getMeasuredAnimals().size()>0)
			setAnimalLabels(vrControl.getMeasuredAnimals().get(vrControl.getMeasuredAnimals().size()-1));
		}
		vrControl.getVRPanel().repaint();
	}
	
	private boolean newAnimalMeasuremnt_IMU(Point imPoint, VRMeasurement possibleMeasurement){
		if (currentIMUData==null) return false;
		
		//set image tilt
		possibleMeasurement.imageBearing=Math.toDegrees(currentIMUData.getTrueHeading()); 
		possibleMeasurement.imageBearingErr=Math.toDegrees(currentIMUData.getErrorHeading()); 
		possibleMeasurement.imagePitch=Math.toDegrees(currentIMUData.getPitch()); 
		possibleMeasurement.imagePitchErr=Math.toDegrees(currentIMUData.getErrorPitch()); 
		possibleMeasurement.imageTilt=Math.toDegrees(currentIMUData.getTilt()); 
		possibleMeasurement.imageTiltErr=Math.toDegrees(currentIMUData.getErrorTilt()); 
		
		//set calibration value
		possibleMeasurement.calibrationData=vrControl.getVRParams().getCalibrationDatas().get( vrControl.getVRParams().getCurrentCalibrationIndex());
		Point imageCentre=new Point(vrControl.getVRPanel().getImageWidth()/2, vrControl.getVRPanel().getImageHeight()/2);
		
		//calculate the bearing relative to the centre of the image 
		double animalBearing=VRLandMarkMethod.calcAnimalBearing(currentIMUData.getTilt(), imageCentre, imPoint, 1/vrControl.getVRParams().getCalibrationDatas().get( vrControl.getVRParams().getCurrentCalibrationIndex()).degreesPerUnit);
		possibleMeasurement.locBearing=Math.toDegrees(animalBearing)+possibleMeasurement.imageBearing;
		possibleMeasurement.locBearingError=possibleMeasurement.imageBearingErr;
		possibleMeasurement.angleCorrection=Math.toDegrees(animalBearing);
		
		//calculate the pitch relative to the centre of the image 
		double animalPitch=-VRLandMarkMethod.calcAnimalPitch(currentIMUData.getTilt(), imageCentre,  imPoint, 1/vrControl.getVRParams().getCalibrationDatas().get( vrControl.getVRParams().getCurrentCalibrationIndex()).degreesPerUnit);
		possibleMeasurement.locPitch=Math.toDegrees(animalPitch)+possibleMeasurement.imagePitch;
		possibleMeasurement.locPitchError=possibleMeasurement.imagePitchErr;
		
		//paint the photo with possible measurements.
		candidateMeasurement=possibleMeasurement;
		//add symbols
		vrControl.getVRPanel().repaint();
		
		if (vrControl.getMeasuredAnimals()==null){
			vrControl.setMeasuredAnimals(new ArrayList<VRMeasurement>());
		}
		
		candidateMeasurement.vrMethod=this; 

		candidateMeasurement.imageTime=vrControl.getImageTime();
		candidateMeasurement.imageName = new String(vrControl.getImageName());
		candidateMeasurement.imageAnimal = vrControl.getMeasuredAnimals().size();
		candidateMeasurement.heightData = vrControl.getVRParams().getCurrentheightData().clone();
		candidateMeasurement.rangeMethod = vrControl.getRangeMethods().getCurrentMethod();
		
		//calc and set the range.
		candidateMeasurement.locDistance=vrControl.getRangeMethods().getCurrentMethod().getRange(vrControl.getCurrentHeight(), -Math.toRadians(candidateMeasurement.locPitch));
		
		//set distance errors
		//calc errors due to pitch error. The pitch error is calculated from the standard deviation of the averaged IMU data. 
		double range1Er=candidateMeasurement.locDistance-Math.abs(vrControl.getRangeMethods().getCurrentMethod().getRange(vrControl.getCurrentHeight(), -Math.toRadians(candidateMeasurement.locPitch+candidateMeasurement.imagePitchErr)));
		double range2Er=candidateMeasurement.locDistance-Math.abs(vrControl.getRangeMethods().getCurrentMethod().getRange(vrControl.getCurrentHeight(), -Math.toRadians(candidateMeasurement.locPitch-candidateMeasurement.imagePitchErr)));
		candidateMeasurement.locDistanceError=Math.abs(range1Er-range2Er)/2;
		//calc pixel errors
		double range1Pxl = vrControl.getRangeMethods().getCurrentMethod().getRange(vrControl.getCurrentHeight(), -Math.toRadians(candidateMeasurement.locPitch + candidateMeasurement.calibrationData.degreesPerUnit));
		double range2Pxl = vrControl.getRangeMethods().getCurrentMethod().getRange(vrControl.getCurrentHeight(), -Math.toRadians(candidateMeasurement.locPitch - candidateMeasurement.calibrationData.degreesPerUnit));
		double pxlError = Math.abs(range1Pxl-range2Pxl)/2;
		System.out.println("pxl Error: "+pxlError);
		
		//add pixel error and error due to averaging imu pitch. 
		candidateMeasurement.locDistanceError=Math.sqrt(Math.pow(pxlError,2)+Math.pow(candidateMeasurement.locDistanceError,2));
		
		//try and work out a location for the animal
		candidateMeasurement.imageOrigin=vrControl.getLocationManager().getLocation(vrControl.getImageTime());
		calcLocLatLong(candidateMeasurement);
		
		System.out.println("IMU Method: Animal Loc.: Bearing: "+candidateMeasurement.locBearing+ " Pitch: "+candidateMeasurement.locPitch);
		
		VRMeasurement newMeasurement = AcceptMeasurementDialog.showDialog(null, vrControl, candidateMeasurement);

		
		if (newMeasurement != null) {
			vrControl.getMeasuredAnimals().add(newMeasurement);
			vrControl.getVRProcess().newVRLoc(newMeasurement);
		}
		else {
			candidateMeasurement = null;
			return false;
		}
		
		candidateMeasurement = null;
				
		return true; 	
		
	}
	
	private void addMeasurementMarks(Graphics g) {
		drawImageTilt(g, VRLandMarkMethod.landMarkMarker);
	}

	/**
	 * Draw the image tilt cross. 
	 * @param g-graphics handle.
	 * @param landMarkMarker
	 */
	private void drawImageTilt(Graphics g, PamSymbol tiltSymbol) {	
		if (currentIMUData==null) return;
		//don't draw if there are no measurements. 
		if (candidateMeasurement==null && vrControl.getMeasuredAnimals()==null) return;
		//set the colour
		Graphics2D g2=(Graphics2D) g;
		g2.setStroke(VRLandMarkMethod.dashedtmplate);
		g2.setColor(Color.green);
		
		Point imageScreenSize=new Point(vrControl.getVRPanel().getImageWidth(), vrControl.getVRPanel().getImageHeight());

		//create a cross showing the image tilt 		
		//bearing line;
		double yOffset=Math.tan(currentIMUData.getTilt())*imageScreenSize.x/2;
		Point p1=new Point(0, (int) (imageScreenSize.y/2+yOffset));
		Point p2=new Point(imageScreenSize.x, (int) (imageScreenSize.y/2-yOffset));
		p1=vrControl.getVRPanel().imageToScreen(p1);
		p2=vrControl.getVRPanel().imageToScreen(p2);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
		
		//pitch line
		double xOffset=Math.tan(currentIMUData.getTilt())*imageScreenSize.y/2;
		p1=new Point((int) (imageScreenSize.x/2-xOffset), 0);
		p2=new Point((int) (imageScreenSize.x/2+xOffset), imageScreenSize.y);
		p1=vrControl.getVRPanel().imageToScreen(p1);
		p2=vrControl.getVRPanel().imageToScreen(p2);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
	}

	@Override
	protected void addAnimals(Graphics g) {
		if (candidateMeasurement!=null) drawAnimal(g, candidateMeasurement, VRLandMarkMethod.candAnimalMarker);
		drawAnimals(g, VRLandMarkMethod.animalMarker);
	}   
	
	
	private void drawAnimals(Graphics g, PamSymbol pamSymbol) {

		//check for previous animals
		if (vrControl.getMeasuredAnimals()==null) return;
			
		for (int i=0; i<vrControl.getMeasuredAnimals().size(); i++){
			drawAnimal( g, vrControl.getMeasuredAnimals().get(i), pamSymbol); 
		}
	}
	
	@Override
	protected void drawAnimal(Graphics g, VRMeasurement vr, PamSymbol symbol) {
		
		Graphics2D g2=(Graphics2D) g;
		g2.setStroke(VRLandMarkMethod.solid);
		//draw a line from the centre of the image to the animal
		Point imageCentre=new Point(vrControl.getVRPanel().getImageWidth()/2, vrControl.getVRPanel().getImageHeight()/2);
		//draw the animal, form the center of the image to the 
		Point p1, p2;
		symbol.draw(g, p1 = vrControl.getVRPanel().imageToScreen(vr.animalPoint));
		//get a second point at the centre of the image; 
		symbol.draw(g, p2 = vrControl.getVRPanel().imageToScreen(imageCentre));
		//line from the center of the image to the selected animal location.
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
		
		g2.setStroke(VRLandMarkMethod.dashedtmplate);
		//show the bearing and pitch of the animal aligned with the tilt of the image. 
		//get the animal bearing and pitch. 
		double calVal=(1/vr.calibrationData.degreesPerUnit);
		double bearingDiff=	(vr.locBearing-vr.imageBearing);
		double pitchDiff=	(vr.locPitch-vr.imagePitch);
		//show bearing on tilt line....
		double yOffset=Math.sin(Math.toRadians(vr.imageTilt))*bearingDiff*calVal;
		double xOffset=Math.cos(Math.toRadians(vr.imageTilt))*bearingDiff*calVal;
		p2=vrControl.getVRPanel().imageToScreen(new Point(new Point((int) (imageCentre.x+xOffset),(int) (imageCentre.y-yOffset))));
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
		//show pitch on tilt line....
		xOffset=Math.sin(Math.toRadians(vr.imageTilt))*pitchDiff*calVal;
		yOffset=Math.cos(Math.toRadians(vr.imageTilt))*pitchDiff*calVal;
		p2=vrControl.getVRPanel().imageToScreen(new Point(new Point((int) (imageCentre.x-xOffset),(int) (imageCentre.y-yOffset))));
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
	}


	@SuppressWarnings("rawtypes")
	@Override
	public LayerUI getJLayerOverlay() {
		return imuLayerOverlay;
	}

	@Override
	public void clearOverlay() {
		//clear animal measurements in abstract class
		super.clearPoints();
		currentStatus=MEASURE_ANIMAL;
		//clear ribbon panel
		clearRibbonPanel();
		//repaint the vr panel
		vrControl.getVRPanel().repaint();
	}
	
	private void clearRibbonPanel(){
		ribbonPanel.clearImageLabels();
		ribbonPanel.clearAnimalLabels();
	}
	

	@Override
	public PamPanel getRibbonPanel() {
		return ribbonPanel;
	}
	
	private void setImageLabels(AngleDataUnit imageMeasurement){
		if (imageMeasurement==null) ribbonPanel.clearImageLabels();
		else {
			ribbonPanel.setImageBearing(imageMeasurement.getTrueHeading(),imageMeasurement.getErrorHeading());
			ribbonPanel.setImagePitch(imageMeasurement.getPitch(),imageMeasurement.getErrorPitch());
			ribbonPanel.setImageTilt(imageMeasurement.getTilt(),imageMeasurement.getErrorTilt());
		}
	}
	
	private void setAnimalLabels(VRMeasurement imageMeasurement){
		if (imageMeasurement.locBearing==null)  ribbonPanel.clearImageLabels();
		else{
			ribbonPanel.setAnimalBearing(Math.toRadians(imageMeasurement.locBearing), Math.toRadians(imageMeasurement.locBearingError));
			ribbonPanel.setAnimalDistance(imageMeasurement.locDistance, imageMeasurement.locDistanceError);
		}
	}

	@Override
	public PamPanel getSettingsPanel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(int updateType) {
		super.update(updateType);
		switch (updateType){
		case VRControl.SETTINGS_CHANGE:
			findIMUData();
			updateLabels();
		break;
		case VRControl.IMAGE_CHANGE:
			findIMUData();
			updateLabels();
		break;
		case VRControl.METHOD_CHANGED:
			findIMUData();
			updateLabels();
			break;
		}
		
	}
	
	private void updateLabels(){
		setImageLabels(currentIMUData);
		setInstruction();
		setIMUText();
	}
	
	/**
	 * Try and find IMU data. Set the currentIMUDData to null if no data found.
	 */
	private void findIMUData(){
		if (vrControl.getCurrentMethod()==this){
			if (vrControl.getIMUListener()!=null){
				if (vrControl.getIMUListener().getIMUDataBlock()!=null){
					 this.currentIMUData=searchIMUDataBlock(vrControl.getIMUListener().getIMUDataBlock(), vrControl.getImageTime(),getSearchInterval());
				}
			}
		}
	}
	
	/**
	 * Search the datablock for angle data. 
	 * @param imuDataBlock- the current IMU datablock
	 * @param timeMillis- the image time
	 * @param searchInterval- the interval to search for units between
	 * @return an AngledatUnit containing average values of units within the search window.
	 */
	public static AngleDataUnit searchIMUDataBlock(IMUDataBlock imuDataBlock, long timeMillis, long searchInterval){
		
		long millisStart=timeMillis-searchInterval/2;
		long millisEnd=timeMillis+searchInterval/2;
		ArrayList<AngleDataUnit> units=imuDataBlock.findUnitsinInterval(millisStart,millisEnd);
		
		if (units==null) return null;
		if (units.size()<=1) return null; 
		else {
			
			double[] calVals=imuDataBlock.getCalibrationVals();
			
			ArrayList<Double> headings=new ArrayList<Double>();
			ArrayList<Double> pitch=new ArrayList<Double>();
			ArrayList<Double> tilts=new ArrayList<Double>();
			
			for (int i=0; i<units.size(); i++){
				headings.add(units.get(i).getTrueHeading());
				pitch.add(units.get(i).getPitch());
				tilts.add(units.get(i).getTilt());
			}
			
			System.out.println("heading.get(0): "+headings.get(0)+ " pitch.get(0): "+pitch.get(0)+" tilts.get(0): "+tilts.get(0));
			//work out mean values and errors
			Double[] imuVals=new Double[3];
			Double[] imuErrors=new Double[3]; 
			imuVals[0]=PamArrayUtils.mean(headings, 0)+calVals[0];
			imuVals[1]=PamArrayUtils.mean(pitch, 0)+calVals[1];
			imuVals[2]=PamArrayUtils.mean(tilts, 0)+calVals[2];
			
			System.out.println("heading mean: "+imuVals[0]+ " pitch mean: "+imuVals[1]+" tilts. mean: "+imuVals[2]);

			imuErrors[0]=PamArrayUtils.std(headings, 0);
			imuErrors[1]=PamArrayUtils.std(pitch, 0);
			imuErrors[2]=PamArrayUtils.std(tilts, 0);
			//create a new AngleDataUnit to hold data.
			AngleDataUnit anglDataUnit=new AngleDataUnit(timeMillis, imuVals, imuErrors);
			anglDataUnit.setNUnits(headings.size());
			return anglDataUnit;
	
		}
	}
	
	//TODO-make changeable param; 
	public long getSearchInterval(){
		return searchInterval; 
	}
	
	public void setInstruction(){
		if (currentIMUData==null) instruction.setText("Add IMU data ");
		if (currentIMUData!=null) instruction.setText("Click Animal");
		super.setInstruction(instruction);
	}
	
	

}
