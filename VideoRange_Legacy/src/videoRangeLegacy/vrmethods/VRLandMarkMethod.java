package videoRangeLegacy.vrmethods;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.plaf.LayerUI;

import videoRangeLegacy.LandMark;
import videoRangeLegacy.LandMarkGroup;
import videoRangeLegacy.VRControl;
import videoRangeLegacy.VRHeightData;
import videoRangeLegacy.VRMeasurement;
import videoRangeLegacy.VRSymbolManager;
import videoRangeLegacy.panels.AcceptMeasurementDialog;
import videoRangeLegacy.panels.VRPanel;
import videoRangeLegacy.panels.VRParametersDialog;
import videoRangeLegacy.panels.VRSidePanel;
import PamUtils.LatLong;
import PamUtils.PamArrayUtils;
import PamUtils.PamUtils;
import PamView.PamColors;
import PamView.PamSymbol;
import PamView.PamSymbolType;
import PamView.dialog.PamDialog;
import PamView.dialog.PamLabel;
import PamView.panel.PamPanel;

/**
 * The LandMark uses landmarks at known positions or at known angles from the position the image was taken to calculate an animal's position. Two Landmark's essentially give you the four out of the five vital bits of information required to get a true location (on the sea surface). 
 * Image bearing, pitch and tilt and degrees per pixel. The fifth bit of information , the height from which the image was taken, is defined by the user.
 * <p>
 * Angular convention for this module and method. 
 * <p>
 * Bearing- 0==north, 90==east 180=south, 270==west
 * <p>
 * Pitch- 90=-g, 0=0g, -90=g
 * <p>
 * Tilt 0->180 -camera turning towards left to upside down 0->-180 camera turning right to upside down
 * <p>
 * 
 * @author Jamie Macaulay
 */
public class VRLandMarkMethod extends AbstractVRMethod {
	
	
	private final static int SET_LANDMARK=0;
	private final static int SET_LANDMARK_READY=1;
	
	private VRControl vrControl; 
	private int currentStatus=SET_LANDMARK;
	
	private ArrayList<LandMark> setLandMarks;
	private ArrayList<Point> 	landMarkPoints;
	
	private PamPanel sidePanel;
	private PamLabel instruction;
	private JButton clearLM;
	private ImageRotRibbonPanel ribbonPanel;
	private LandMarkMethodUI landMarkUI;
	private JComboBox<LandMarkGroup> landMarkList;
	private JButton lmSettings;
	
	//orientation values
	/**
	 * The mean tilt from landmark measurments. 
	 */
	private Double tilt;
	/**
	 * The tilt values from all combinations of landmarks
	 */
	private ArrayList<Double> tiltVals;
	
	/**
	 * Bearing values for animal based on calculations from each landmark in radians. 0= north, pi=south, pi/2=east. Ideally all values in array should be the same. 
	 */
	private ArrayList<Double> animalBearingVals;
	
	/**
	 * Pitch values for animal based on calculations from each landmark in radians. Ideally should be the same. -pi degrees =downwards, +pi degrees. Ideally all values in array should be the same. 
	 */
	private ArrayList<Double> animalPitchVals;
	
	//calibration values;
	/**
	 * mean calibration value
	 */
	private Double calMean;
	/**
	 * Calibration values (pixels per degree) for each combination of landmarks 
	 */
	private ArrayList<Double> calValues;

	
	
	//Graphics
	final static float dash1[] = {2.0f};
	final static BasicStroke dashedtmplate =
	        new BasicStroke(2.0f,
	                        BasicStroke.CAP_BUTT,
	                        BasicStroke.JOIN_MITER,
	                        5.0f, dash1, 0.0f);
	
	 final static BasicStroke solid =
		        new BasicStroke(2f);
	 
	public final static Color landMarkCol=Color.GREEN;
	public final static Color animalCol=Color.CYAN;
	 
	public static PamSymbol landMarkMarker = new PamSymbol(PamSymbolType.SYMBOL_CIRCLE, 12, 12, false, landMarkCol, landMarkCol);
	public static PamSymbol animalMarker = new PamSymbol(PamSymbolType.SYMBOL_CIRCLE, 12, 12, false, animalCol, animalCol);
	public static PamSymbol candAnimalMarker = new PamSymbol(PamSymbolType.SYMBOL_CIRCLE, 12, 12, false, Color.red, Color.red);
	public static VRSymbolManager landMarksSymbol = new VRSymbolManager(landMarkMarker, "Landmark");
	public static VRSymbolManager animalMark = new VRSymbolManager(animalMarker, "LandMark Animal");


	public VRLandMarkMethod(VRControl vrControl){
		super(vrControl);
		this.vrControl=vrControl;
		this.sidePanel=createSidePanel();
		this.landMarkUI=new LandMarkMethodUI(vrControl);
		this.ribbonPanel=new ImageRotRibbonPanel(vrControl);
	}
	
	
	@Override
	public String getName() {	
		return "Landmark Method";
		
	}
	
	public PamPanel createSidePanel(){
		PamLabel lab;
		PamPanel panel=new PamPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx=0;
		c.gridy=0;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, lab=new PamLabel("Landmark Measurement "), c);
		lab.setFont(PamColors.getInstance().getBoldFont());
		
		c.gridx=0;
		c.gridy++;
		PamDialog.addComponent(panel, createLandMarkPanel(), c);

		c.gridy++;
		PamDialog.addComponent(panel, createLocationListPanel(), c);
		
		c.gridy++;
		PamDialog.addComponent(panel, instruction = new PamLabel("..."), c);
		instruction.setFont(PamColors.getInstance().getBoldFont());
		
		c.gridx=0;
		c.gridy++;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, clearLM = new JButton("Clear LandMarks"), c);
		clearLM.addActionListener(new ClearLandMarks());
		c.gridy++;
		c.gridy++;
	
		
		setInstruction(currentStatus);
		
		return panel;
	}
	
	private PamPanel createLandMarkPanel(){
		PamPanel panel=new PamPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridwidth = 3;
		PamDialog.addComponent(panel, landMarkList=createLandMarkList(), c);
		setLandMarks();
		landMarkList.addActionListener(new SelectLandMarkGroup());
		c.weightx = 0;
		c.gridx =3;
		c.gridwidth = 1;
		PamDialog.addComponent(panel, lmSettings=new JButton(VRSidePanel.settings), c);
		lmSettings.setPreferredSize(VRPanel.settingsButtonSize);
		lmSettings.addActionListener(new SelectLandMarkSettings());
		return panel;
	}
	
	private class SelectLandMarkGroup implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			vrControl.getVRParams().setCurrentLandMarkGroupIndex(landMarkList.getSelectedIndex());
			clearOverlay();
		}
	}
	
	private class SelectLandMarkSettings implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			vrControl.settingsButton(null,VRParametersDialog.LAND_MARK);
		}
	}
	
	private class ClearLandMarks implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			clearOverlay();
		}
	}
	
	/**
	 * Create a landmark list for the landmark combo box on side panel;  
	 */
	public void setLandMarks(){
		
		ArrayList<LandMarkGroup> landMarkGroups=vrControl.getVRParams().getLandMarkDatas();
		landMarkList.removeAllItems();
		if (landMarkGroups==null) return;
		if (landMarkGroups.size()==0) return;

		for (int i=0; i<landMarkGroups.size(); i++){
			landMarkList.addItem(landMarkGroups.get(i));
		}
		
		landMarkList.setSelectedIndex(vrControl.getVRParams().getCurrentLandMarkGroupIndex());
		
	}
	
	public LandMarkGroup getSelectedLMGroup(){
		if (vrControl.getVRParams().getLandMarkDatas()==null) return null;
		if (vrControl.getVRParams().getLandMarkDatas().size()==0) return null;
		return vrControl.getVRParams().getLandMarkDatas().get(vrControl.getVRParams().getCurrentLandMarkGroupIndex());
	}


	private void setInstruction(int currentStatus2) {
		
		switch (currentStatus) {
		case SET_LANDMARK:
			instruction.setText("Add Landmark");
			break;
		case SET_LANDMARK_READY:
			instruction.setText("Add Landmark/Click Animal");
			break;
		case MEASURE_ANIMAL:
			instruction.setText("Click animal");
			break;
		}
		
		super.setInstruction(instruction);
	}
	
	
	private VRMeasurement setImagePosVals(VRMeasurement measurement) {
		
		if (tiltVals==null) return null;
		
		//calculate the middle of the image
		Point imageMiddle=new Point(vrControl.getCurrentImage().getImage().getWidth()/2, vrControl.getCurrentImage().getImage().getHeight()/2);
		//work out image tilt
		double averageTilt=PamArrayUtils.mean(tiltVals, 0);
		double stdTilt=PamArrayUtils.std(tiltVals, 0);
		//work out image bearing
		ArrayList<Double> vals=calcBearingLoc(imageMiddle, averageTilt);
		double averageBearing=PamArrayUtils.mean(vals, 0);
		double stdBearing=PamArrayUtils.std(vals, 0);
		//work out image pitch
		vals=calcPitchLoc(imageMiddle, averageTilt);
		double averagePitch=PamArrayUtils.mean(vals, 0);
		double stdPitch=PamArrayUtils.std(vals, 0);
	
		//TODO-units?
		measurement.imageBearing=Math.toDegrees(averageBearing);
		measurement.imageBearingErr=Math.toDegrees(stdBearing);
		measurement.imagePitch=Math.toDegrees(averagePitch);
		measurement.imagePitchErr=Math.toDegrees(stdPitch);
		measurement.imageTilt=Math.toDegrees(averageTilt);
		measurement.imageTiltErr=Math.toDegrees(stdTilt);

		return measurement;

	}
	
	private void setImageLabels(VRMeasurement imageMeasurement){
		if (imageMeasurement==null) ribbonPanel.clearImageLabels();
		else {
			ribbonPanel.setImageBearing(imageMeasurement.imageBearing,imageMeasurement.imageBearingErr);
			ribbonPanel.setImagePitch(imageMeasurement.imagePitch,imageMeasurement.imagePitchErr);
			ribbonPanel.setImageTilt(imageMeasurement.imageTilt,imageMeasurement.imageTiltErr);
		}
	}
	
	private void setAnimalLabels(VRMeasurement imageMeasurement){
		if (imageMeasurement.locBearing==null)  ribbonPanel.clearImageLabels();
		else{
			ribbonPanel.setAnimalBearing(Math.toRadians(imageMeasurement.locBearing), Math.toRadians(imageMeasurement.locBearingError));
			ribbonPanel.setAnimalDistance(imageMeasurement.locDistance, imageMeasurement.locDistanceError);
		}
	}
	

	private JComboBox<LandMarkGroup> createLandMarkList() {
		return new JComboBox<LandMarkGroup>();
	}

	@Override
	public PamPanel getSidePanel() {
		return sidePanel;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public LayerUI getJLayerOverlay() {
		return landMarkUI;
	}
	

	/**
	 * JLayer overlay panel for the land mark method. Handles most mouse events.
	 * @author Jamie Macaulay
	 *
	 */
	@SuppressWarnings("serial")
	private class LandMarkMethodUI extends VRAbstractLayerUI {
		
		private VRControl vrControl;
		JPopupMenu menu;

		public LandMarkMethodUI(VRControl vRControl){
			super(vRControl);
			this.vrControl=vRControl; 
		}
		
		 @Override
		 public void paint(Graphics g, JComponent c) {
		    super.paint(g, c);
			addMeasurementMarks(g);
			addAnimals(g);
		}
		@SuppressWarnings("rawtypes")
		@Override
		protected void processMouseEvent(MouseEvent e,  JLayer l) {
			
			boolean vis=isMenuVisible();
			
			if (e.isPopupTrigger()){
			    menu=showlmPopUp(e.getPoint());
				menu.show(l, e.getPoint().x, e.getPoint().y);
			}
			//if there are enough landmark points and popup menu is not visible then a double click will measure animal location. 
			else if (e.getClickCount()>1 && e.getButton()==MouseEvent.BUTTON1 && !vis){
				if (isCalcReady()) measureAnimal(e.getPoint());
			}
			vrControl.getVRPanel().repaint();
		}
		
		private boolean isMenuVisible(){
			if (menu==null) return false; 
			return menu.isVisible();
		}
		
	}
	
	/**
	 * Make sure an angle falls between zero and 2pi(360 degrees)
	 * @params t- angle in radians; 
	 * @return angle between 0 and 360;
	 */
	public static double wrap360(double t){
		if (t<0) t=(2*1000*Math.PI)+t; //if negative to get into correct form. Make big incase we have a huge negative angle.
		t=t%(2*Math.PI); 
		return t; 
	}
	
	/**
	 * Create a dynamic pop up menu to allow users to select landmarks and animals. 
	 * @param point- current mouse point. 
	 * @return
	 */
	private JPopupMenu showlmPopUp(Point point){
		
		JPopupMenu popMenu=new JPopupMenu();
		JMenuItem menuItem;
		LandMarkGroup landMarks=vrControl.getVRParams().getLandMarkDatas().get(vrControl.getVRParams().getCurrentLandMarkGroupIndex());
		int n=0;
		if (currentStatus!=MEASURE_ANIMAL){
			for (int i=0; i<landMarks.size(); i++){
				if (!isInList(landMarks.get(i))){
					menuItem=new JMenuItem(landMarks.get(i).getName());
					menuItem.addActionListener(new SetLandMark(landMarks.get(i),point));
					popMenu.add(menuItem);
					n++;
				}
			}
			//if ready to calculate then give the user the option of calculating a position
			if (isCalcReady()){
				if (n!=0) popMenu.addSeparator();
				menuItem=new JMenuItem("Calc Orientation");
				menuItem.addActionListener(new Calculate());
				popMenu.add(menuItem);
			}
			
			popMenu.addSeparator();
		}
		
		if (isCalcReady()){
			menuItem=new JMenuItem("Measure Animal");
			menuItem.addActionListener(new SetAnimal(point));
			popMenu.add(menuItem);
		}
		
		menuItem=new JMenuItem("Clear");
		menuItem.addActionListener(new ClearLandMarks());
		popMenu.add(menuItem);
		
		return popMenu;
	}
	
	private class Calculate implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			currentStatus=MEASURE_ANIMAL;
			calcVals();
			vrControl.getVRPanel().repaint();
			VRMeasurement imageMeasurement=new VRMeasurement();
			setImagePosVals(imageMeasurement);
			setImageLabels(imageMeasurement);
			setInstruction(currentStatus);
		}
		
	}
	
	class SetAnimal implements ActionListener{

		Point point;
		
		protected SetAnimal(Point point){
			this.point=point;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			measureAnimal(this.point);
			setInstruction(currentStatus);
		}
	}
	
	private void measureAnimal(Point point){
		currentStatus=MEASURE_ANIMAL;
		calcVals();
		Point imPoint=vrControl.getVRPanel().screenToImage(point);
		animalBearingVals=calcBearingLoc(imPoint, tilt);
		//TODO-delete
		for (int i=0; i<animalBearingVals.size(); i++){
			System.out.println("Animal bearing vals: "+ Math.toDegrees(animalBearingVals.get(i)));
		}
		animalPitchVals=calcPitchLoc(imPoint, tilt);
		VRMeasurement possibleMeasurement=new VRMeasurement(imPoint);
		//add image position labels
		setImagePosVals(possibleMeasurement);
		setImageLabels(possibleMeasurement);
		//paint the photo with possible meaurements
		candidateMeasurement=possibleMeasurement;
		vrControl.getVRPanel().repaint();
		//calc loc values
		newAnimalMeasurement_LandMark(imPoint, possibleMeasurement);
		//add animal labels
		if (vrControl.getMeasuredAnimals()!=null){
			if (vrControl.getMeasuredAnimals().size()>0)
			setAnimalLabels(vrControl.getMeasuredAnimals().get(vrControl.getMeasuredAnimals().size()-1));
		}
		vrControl.getVRPanel().repaint();
	}
	
	private void calcVals(){
		
		ArrayList<Integer> indexM1 = PamUtils.indexM1(landMarkPoints.size());
		ArrayList<Integer> indexM2 = PamUtils.indexM2(landMarkPoints.size());
		
		//tilt values
		tiltVals=new ArrayList<Double>();
		double tiltMean=0;
		double tiltVal;
		//calibration value
		double bearingDiff;
		double pitchDiff;
		double pixelPerDegree;
		double pPDMean=0;
		calValues=new ArrayList<Double>();

		for (int i=0; i<indexM1.size(); i++){
			
			tiltVal=calcTilt(landMarkPoints.get(indexM1.get(i)), landMarkPoints.get(indexM2.get(i)),  setLandMarks.get(indexM1.get(i)),  setLandMarks.get(indexM2.get(i)), getImagePos());
			tiltVals.add(tiltVal);
			tiltMean+=tiltVal;
			
			bearingDiff=calcBearingDiff(  setLandMarks.get(indexM1.get(i)),  setLandMarks.get(indexM2.get(i)),  getImagePos());
			pitchDiff=calcPitchDiff(  setLandMarks.get(indexM1.get(i)),  setLandMarks.get(indexM2.get(i)),  getImagePos());
			pixelPerDegree=calcPixelsperDegree(landMarkPoints.get(indexM1.get(i)).distance(landMarkPoints.get(indexM2.get(i))),  bearingDiff,  pitchDiff);
			calValues.add(pixelPerDegree);
			pPDMean+=pixelPerDegree;
			
		}
		
		this.calMean=pPDMean/(double) indexM1.size();
		this.tilt=tiltMean/(double) indexM1.size();
		
	}
	
	/**
	 * Calculate the bearing of the animal from the tilt and calibration information from currently set landmarks. 
	 * @param animalPoint-point on full sized image
	 * @param tilt- tilt of image in radians
	 * @return ArrayList of bearing values calculated form each currently set landmark
	 */
	private ArrayList<Double> calcBearingLoc(Point anP, Double tilt){
		ArrayList<Double> bearingVals=new ArrayList<Double>();
		double bearingVal; 
		for (int i=0; i<setLandMarks.size(); i++){
			bearingVal=wrap360(calcBearing(setLandMarks.get(i), getImagePos())+calcAnimalBearing(tilt,landMarkPoints.get(i), anP, calMean));
//			System.out.println("landmark bearing: original: " +setLandMarks.get(i).getName() +" "+ Math.toDegrees(calcBearing(setLandMarks.get(i), getImagePos())) + " animal from landmark: "+Math.toDegrees(calcAnimalBearing(tilt,landMarkPoints.get(i), anP, calMean))  +" wrap360 bearingVal: "+Math.toDegrees(bearingVal));
			bearingVals.add(bearingVal);
		}
		return bearingVals;
	}
	
	/**
	 * Calculate the pitch of the animal from the tilt and calibration information from currently set landmarks.
	 * @param anP- point of the animal
	 * @param tilt- tilt of image in radians
	 * @return ArrayList of pitch values calculated form each currently set landmark
	 */
	private ArrayList<Double> calcPitchLoc(Point anP, Double tilt){
		ArrayList<Double> pitchVals=new ArrayList<Double>();
		for (int i=0; i<setLandMarks.size(); i++){
			pitchVals.add(calcPitch(setLandMarks.get(i), getImagePos())-calcAnimalPitch( tilt, landMarkPoints.get(i), anP, calMean));
		}
		return pitchVals;
	}
	
	private boolean isInList(LandMark landMark) {
		if (setLandMarks==null) return false; 
		return setLandMarks.contains(landMark);
	}


	/**
	 * If there are two landmarks then the method is ready for  an animal measurment. . 
	 * @return true if ready for measurment;
	 */
	private boolean isCalcReady(){
		if (setLandMarks==null) return false;
		if (setLandMarks.size()>=2) return true;
		return false; 
	}
	
	class SetLandMark implements ActionListener{
		
		LandMark landMark;
		Point point;
		
		protected SetLandMark(LandMark landMark, Point point){
			this.landMark=landMark;
			this.point=point;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (landMarkPoints==null || setLandMarks==null){
				landMarkPoints=new ArrayList<Point>();
				setLandMarks=new ArrayList<LandMark>();
			}
			landMarkPoints.add(vrControl.getVRPanel().screenToImage(point));
			setLandMarks.add(landMark);
			if (setLandMarks.size()>=2) currentStatus=SET_LANDMARK_READY;
			setInstruction(currentStatus);
		}
		
	}
	

	
	@Override
	protected void addAnimals(Graphics g) {
		drawCandAnimals(g, candAnimalMarker);
		drawAnimals(g, animalMarker);
	}   
	
	private void drawAnimals(Graphics g, PamSymbol pamSymbol) {

		//check for previous animals
		if (vrControl.getMeasuredAnimals()==null) return;
				
		if (landMarkPoints==null) return;
		if (landMarkPoints.size()==0) return;
		
		for (int i=0; i<vrControl.getMeasuredAnimals().size(); i++){
			drawAnimal( g, vrControl.getMeasuredAnimals().get(i), pamSymbol); 
		}
	}
	
	/**
	 * Draw the candidate measurement.
	 * @param g - graphics handle
	 * @param pamSymbol
	 */
	private void drawCandAnimals(Graphics g, PamSymbol pamSymbol) {

		if (candidateMeasurement==null) return;
				
		if (landMarkPoints==null) return;
		if (landMarkPoints.size()==0) return;
		
		drawAnimal( g, candidateMeasurement, pamSymbol); 
		
	}
	
	
	/**
	 * Draw all animal measurements for the image. 
	 */
	protected void drawAnimal(Graphics g, VRMeasurement vr, PamSymbol symbol) {
				
		Graphics2D g2=(Graphics2D) g;

		Point sp1;
		Point pA;
		Point anP=vrControl.getVRPanel().imageToScreen(vr.animalPoint);
		for (int i=0; i<landMarkPoints.size(); i++){
			sp1=vrControl.getVRPanel().imageToScreen(landMarkPoints.get(i));
			g2.setColor(symbol.getLineColor());
			g2.setStroke(solid);
			g2.drawLine(sp1.x, sp1.y, anP.x, anP.y);
			//draw the bearing and pitch lines
			pA=calcPerpPoint(tilt,calcBearingDiffAnimal(tilt, sp1, anP),anP);
			g2.setStroke(dashedtmplate);
			g.drawLine(sp1.x, sp1.y, pA.x, pA.y);
			g.drawLine(anP.x, anP.y, pA.x, pA.y);
		}
		//draw the animal circle
		symbol.draw(g, anP);

	}


	private void addMeasurementMarks(Graphics g) {
		drawLandMarks(g, landMarksSymbol.getPamSymbol());
	}

	protected void drawLandMarks(Graphics g, PamSymbol symbol){
		
		Graphics2D g2=(Graphics2D) g;
		
		if (landMarkPoints==null) return;
		if (landMarkPoints.size()==0) return;
		
		Point sp1 = null;
		Point sp2 = null;
		
		if (landMarkPoints != null) {
			for (int i=0; i<landMarkPoints.size(); i++){
				symbol.draw(g2, sp1 = vrControl.getVRPanel().imageToScreen(landMarkPoints.get(i)));
			}
		}
		
		if (currentStatus==MEASURE_ANIMAL){
			if (tiltVals==null) return;
			Point perpPoint;
			ArrayList<Integer> indexM1 = PamUtils.indexM1(landMarkPoints.size());
			ArrayList<Integer> indexM2 = PamUtils.indexM2(landMarkPoints.size());
			for (int i=0; i<indexM1.size(); i++){
				g.setColor(symbol.getLineColor());
				sp1=vrControl.getVRPanel().imageToScreen(landMarkPoints.get(indexM1.get(i)));
				sp2=vrControl.getVRPanel().imageToScreen(landMarkPoints.get(indexM2.get(i)));
				//calc the tilt
//				System.out.println("LandMark: "+indexM1.get(i)+" : "+indexM2.get(i)+" tilt: "+Math.toDegrees(tiltVals.get(i)));
				perpPoint=calcPerpPoint(tiltVals.get(i), landMarkPoints.get(indexM1.get(i)), landMarkPoints.get(indexM2.get(i)),  setLandMarks.get(indexM1.get(i)),  setLandMarks.get(indexM2.get(i)), setLandMarks.get(0).getLatLongOrigin());
				perpPoint=vrControl.getVRPanel().imageToScreen(perpPoint);
				g2.setColor(landMarkCol);
				g2.setStroke(solid);
				g2.drawLine(sp1.x, sp1.y, sp2.x, sp2.y);
				g2.setStroke(dashedtmplate);
				g.drawLine(sp1.x, sp1.y, perpPoint.x, perpPoint.y);
				g.drawLine(sp2.x, sp2.y, perpPoint.x, perpPoint.y);
			}
		}
		
	}

	@Override
	public void clearOverlay() {
		//clear animal measurements in abstract class
		super.clearPoints();
		//clear landmark measurements
		setLandMarks=null;
		landMarkPoints=null;
		currentStatus=SET_LANDMARK;
		setInstruction(currentStatus);
		//clear ribbon panle
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

	@Override
	public PamPanel getSettingsPanel() {
		return null;
	}

	@Override
	public void update(int updateType){
		super.update(updateType);
		switch (updateType){
			case VRControl.SETTINGS_CHANGE:
				setLandMarks();
			break;
			case VRControl.HEADING_UPDATE:
			
			break;
			case VRControl.TILT_UPDATE:
				
			break;
			case VRControl.IMAGE_CHANGE:
				setInstruction(currentStatus);
			break;
		}
	}

	
	
	protected boolean newAnimalMeasurement_LandMark(Point animalPoint, VRMeasurement possibleMeasurement) {
		
		if (animalPoint==null) return false;
		
		//calculate pitch and bearing values to animal
		double averageBearing=PamArrayUtils.mean(animalBearingVals, 0);
		double averagePitch=PamArrayUtils.mean(animalPitchVals, 0);
		double stdBearing=PamArrayUtils.std(animalBearingVals, 0);
		double stdPitch=PamArrayUtils.std(animalPitchVals, 0);
			
		candidateMeasurement=possibleMeasurement;
		
		if (vrControl.getMeasuredAnimals()==null){
			vrControl.setMeasuredAnimals(new ArrayList<VRMeasurement>());
		}
		candidateMeasurement.vrMethod=this; 
		
		candidateMeasurement.imageTime=vrControl.getImageTime();
		candidateMeasurement.imageName = new String(vrControl.getImageName());
		candidateMeasurement.imageAnimal = vrControl.getMeasuredAnimals().size();
		candidateMeasurement.heightData = getImageHeight();
		candidateMeasurement.rangeMethod = vrControl.getRangeMethods().getCurrentMethod();
		
		//set location info.
		candidateMeasurement.locDistance=vrControl.getRangeMethods().getCurrentMethod().getRange(getImageHeight().height, -averagePitch);
		candidateMeasurement.locBearing=candidateMeasurement.imageBearing;
		candidateMeasurement.locPitch=Math.toDegrees(averagePitch);
		candidateMeasurement.angleCorrection=Math.toDegrees(averageBearing)-candidateMeasurement.imageBearing;
		
		//calc location errors
		double range1Er=candidateMeasurement.locDistance-Math.abs(vrControl.getRangeMethods().getCurrentMethod().getRange(getImageHeight().height, -(averagePitch+stdPitch)));
		double range2Er=candidateMeasurement.locDistance-Math.abs(vrControl.getRangeMethods().getCurrentMethod().getRange(getImageHeight().height, -(averagePitch-stdPitch)));
		candidateMeasurement.locDistanceError=Math.abs(range1Er-range2Er)/2;
		
		//calc pixel errors
		double range1PxLEr = vrControl.getRangeMethods().getCurrentMethod().getRange(getImageHeight().height, -(averagePitch + (Math.PI/180)/calMean));
		double range2PxLEr = vrControl.getRangeMethods().getCurrentMethod().getRange(getImageHeight().height, -(averagePitch - (Math.PI/180)/calMean));
		double pxlError = Math.abs(range1PxLEr-range2PxLEr)/2;
		//add pixel error and pitch error
		candidateMeasurement.pixelAccuracy=pxlError;
		candidateMeasurement.locDistanceError=Math.sqrt(Math.pow(pxlError,2)+Math.pow(candidateMeasurement.locDistanceError,2));
		//add in bearing errors
		candidateMeasurement.locBearingError=stdBearing;
		candidateMeasurement.locPitchError=stdPitch;
		
		//work out location of animal (LatLong)
		candidateMeasurement.imageOrigin=getImagePos();
		calcLocLatLong(candidateMeasurement);
				
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
	
	
	
	
	//////////////Calculations/////////////
	/**
	 * Calculates the bearing of the target animal based on a landmark point, the animal location and a calibration value; 
	 * @param tilt- tilt of the image in radians.
	 * @param landmrkp1-landmark point.
	 * @param animal-animal point.
	 * @param calValue- calibration value in pixels per degree
	 * @return bearing relative to the landmark - radians.
	 */
	public static double calcAnimalBearing(double tilt, Point  lndmrkPoint, Point animalPoint, double calValue){
		double bearing=calcBearingDiffAnimal( tilt,  lndmrkPoint,  animalPoint);
		System.out.println("Bearing pixels: "+bearing+ " Bearing degrees: "+(bearing/calValue));
		bearing=Math.toRadians(bearing/calValue);
		return bearing; 
	}
	
	/**
	 * Calculates the pitch of the target animal based on a landmark point, the animal location and a calibration value; 
	 * @param tilt- tilt of the image in radians.
	 * @param landmrkp1-a point on the image for which we know the pitch and bearing. 
	 * @param animal-animal point.
	 * @param calValue- calibration value in pixels per degree.
	 * @return pitch relative to the landmark .radians. 
	 */
	public static double calcAnimalPitch(double tilt, Point  lndmrkPoint, Point animalPoint,  double calValue){
		double pitch=calcPitchDiffAnimal( tilt,  lndmrkPoint,  animalPoint);
		pitch=Math.toRadians(pitch/calValue);
		return pitch;
	}
	
	/**
	 * Calculates the bearing of the animal in pixels 
	 * @param tilt- tilt of the image in radians.
	 * @param landmrkp1-a point on the image for which we know the pitch and bearing. 
	 * @param animal-animal point.
	 * @return bearing in pixels. 
	 */
	public static double calcBearingDiffAnimal(double tilt, Point landmrkp1, Point animal){
		
		double dist=landmrkp1.distance(animal);

		double angr=Math.atan2((animal.y-landmrkp1.y),(animal.x-landmrkp1.x));
		
		double adj=Math.cos(angr+tilt)*dist;
//		System.out.println("Animal bearing diff: "+adj);
		return adj;

	}
	
	/**
	 * Calculates the pitch of the animal in pixels 
	 * @param tilt- tilt of the image in radians.
	 * @param landmrkp1-a point on the image for which we know the pitch and bearing. 
	 * @param animal-animal point.
	 * @return bearing in pixels. 
	 */
	public static double calcPitchDiffAnimal(double tilt, Point landmrkp1, Point animal){
		
		double dist=landmrkp1.distance(animal);
				
		double angr=Math.atan2((animal.y-landmrkp1.y),(animal.x-landmrkp1.x));
		
		double opp=Math.sin(angr+tilt)*dist;
//		System.out.println("Animal pitch diff: "+opp);
		return opp;
		
	}
	
	

	/**
	 * Calculates location of the perpendicular vertex of the triangle defined by landmarks 1 and 2, with the side of the triangle following the tilt of the picture.  
	 * @param tilt-tilt of image
	 * @param landmrkP1-landmark 1 point on image
	 * @param landmrkP2-landmark 2 point on image
	 * @param landmrk1-landmark 1
	 * @param landmrk2-landmark 2
	 * @param imagePos-the from which the image was taken from. Note that this can be null if the landmarks are defined by bearings rathar than GPS co-ordinates. 
	 * @return the perpindicular vertex of a tringle where the other two vertex have been defined by landmark points. 
	 */
	public Point calcPerpPoint(double tilt, Point landmrkP1, Point landmrkP2, LandMark landmrk1, LandMark landmrk2, LatLong imagePos){
		double bearingDiff=calcBearingDiff( landmrk1,  landmrk2,  getImagePos());
		double pitchDiff=calcPitchDiff( landmrk1,  landmrk2,  getImagePos());
		double pixelPerDegree=calcPixelsperDegree(landmrkP1.distance(landmrkP2),  bearingDiff,  pitchDiff);
		Point perpPoint=calcPerpPoint( tilt, pixelPerDegree*Math.toDegrees(bearingDiff),  landmrkP2);
		return perpPoint;
	}
	

	
	/**
	 * Calculates the point at which the perpendicular corner of the pitch/bearing triangle resides. 
	 * @param tilt-tilt in radians
	 * @param bearingDiff-difference in bearing angle, in pixels. 
	 */
	public static Point calcPerpPoint(double tilt, double bearingDiff, Point landMark2){
		
		int x=(int) (Math.cos(tilt)*bearingDiff);
		int y=(int) (Math.sin(tilt)*bearingDiff);
		
		Point perpindicularLoc=new Point();
		perpindicularLoc.x=landMark2.x-x;
		perpindicularLoc.y=landMark2.y+y;

		
		return perpindicularLoc;

	}
	
	public static double calcTilt(Point landmrkP1, Point landmrkP2, LandMark landmrk1, LandMark landmrk2, LatLong imagePos){
		double tilt=calcTilt( landmrkP1,  landmrkP2, calcBearingDiff(landmrk1,landmrk2,imagePos), calcPitchDiff(landmrk1,landmrk2,imagePos));
		return tilt;
	}	
	
	/**
	 * Calculate the pixels per degree. 
	 * @param distPix-distance between two points in pixels
	 * @param bearingDiff-bearing difference-radians
	 * @param pitchdiff-pitch difference-radians
	 * @return pixels per degree. 
	 */
	private static double calcPixelsperDegree(double distPix, double bearingDiff, double pitchdiff){
		double sizeDistRad=Math.sqrt(Math.pow(bearingDiff,2)+Math.pow(pitchdiff, 2));
		double sizeDistDeg=Math.toDegrees(sizeDistRad);
		double pixelsPerDeg=distPix/sizeDistDeg;
		return pixelsPerDeg;
	}
	
	/**
	 * Calculate the pitch difference between two landmarks for an image taken at a certain location
	 * @param landmrk1- Landmark 1
	 * @param landmrk2- Landmark 2
	 * @param imagePos- position of the image, including lat, long, height.
	 * @return the bearing difference in radians. 
	 */
	private static double calcBearingDiff(LandMark landmrk1, LandMark landmrk2, LatLong imagePos){
		double bearing1=calcBearing(landmrk1, imagePos);
		double bearing2=calcBearing(landmrk2, imagePos);
//		System.out.println("Bearing1: "+bearing1+" Bearing2: "+bearing2+ "  bearingDiff: "+Math.toDegrees(calcBearingDiff(bearing1, bearing2)));
		return calcBearingDiff(bearing1, bearing2);
	}
	
	/**
	 * We want the bearing difference between two points on a screen. That's the minimum difference between points either way round the circle. 
	 * @param bearing1- bearing 1 in radians
	 * @param bearing2-brearing 2 in radians
	 */
	private static double calcBearingDiff(double bearing1, double bearing2){
		double bearingdiff=bearing2-bearing1;
		if (Math.abs(bearingdiff)>Math.PI){
			if (bearingdiff<0) return Math.PI*2+bearingdiff;
			else return bearingdiff-Math.PI*2; 
		}
		else return  bearingdiff;
	}
	
	/**
	 * Calculate the pitch difference between two landmarks for an image taken at a certain location
	 * @param landmrk1- Landmark 1
	 * @param landmrk2- Landmark 2
	 * @param imagePos- position of the image, including lat, long, height.
	 * @return the pitch difference in radians.
	 */
	private static double calcPitchDiff(LandMark landmrk1, LandMark landmrk2, LatLong imagePos){
		double pitch1=calcPitch(landmrk1, imagePos);
		double pitch2=calcPitch(landmrk2, imagePos);
//		System.out.println("Pitch1: "+Math.toDegrees(pitch1)+" pitch2: "+Math.toDegrees(pitch2));
		return -(pitch1-pitch2);
	}
	
	/**
	 * Calculate the bearing re. North between a landmark and image location. 
	 * @param landMrk
	 * @param imagePos
	 * @return the bearing of the landmark in radians.
	 */
	private static double calcBearing(LandMark landMrk, LatLong imagePos){
		double bearing;
		//if the landMarks have a true GPS location then work out bearing, otherwise the landMark should have a pre-defined bearing from image;
		if (landMrk.getPosition()!=null) { 
			bearing=Math.toRadians(imagePos.bearingTo(landMrk.getPosition()));
		}
		else {
			bearing=Math.toRadians(landMrk.getBearing());
		}
//		System.out.println("VRLandMarkMethod: Landmark bearing: " +Math.toDegrees(bearing));
		return bearing;
	}
	
	/**
	 * Calculate the pitch between a landMark and image location.
	 * @param landMrk-current landmark
	 * @param imagePos-image location
	 * @return
	 */
	public static double calcPitch(LandMark landMrk, LatLong imagePos){
		double pitch;
		//if the landMarks have a true GPS location then work out pitch, otherwise the landMark should have a pre-defined pitch from image;
		if (landMrk.getPosition()!=null) { 
			double distance=landMrk.getPosition().distanceToMetres(imagePos);
//			System.out.println("LndMark Height: "+landMrk.getHeight() + "imagePos Height: "+ imagePos.getHeight() +"Height diff: "+(landMrk.getHeight()-imagePos.getHeight()) + "distance: "+distance);
			pitch=Math.atan((landMrk.getHeight()-imagePos.getHeight())/distance);
		}
		else {
			pitch=Math.toRadians(landMrk.getPitch());
		}
		return pitch;
	}
	
	/**
	 * Calculates the tilt of the image based on two landmarks selected on that image and the known angles between those landmarks. 
	 * @param landmrkP1- the location on the image of landmark1-pixels
	 * @param landmrkP2- the location on the image of landmark2-pixels
	 * @param bearingDiff- the difference in heading between landMark1 and landMark2
	 * @param pitchDiff- the difference in pitch between landMark1 and landMark2
	 * @return tilt in radians
	 */
	public static double calcTilt(Point landmrkP1, Point landmrkP2, double bearingDiff, double pitchDiff){
		
//		System.out.println(" landmrkP2.y: "+landmrkP2.y+" landmrkP1.y "+ landmrkP1.y +" landmrkP2.x "+landmrkP2.x+" landmrkP1.x "+landmrkP1.x);
		//work out angle from horizontal
		int opp=landmrkP2.y-landmrkP1.y;
		int adj=landmrkP2.x-landmrkP1.x;
		
		//tilt angle in radians between the landmark in the image
		double angHorz=Math.atan2((double) opp,(double) adj);
		
		//work out actual true angle between landmarks
		double angActual=Math.atan2(pitchDiff, bearingDiff);
		System.out.println("pitchDiff "+ Math.toDegrees(pitchDiff) + "bearing diff: "+Math.toDegrees(bearingDiff) + "Actual Angle: "+Math.toDegrees(angActual));
		
		//work out tilt
		double tilt=PamUtils.constrainedAngleR(-angActual-angHorz,Math.PI);
				
		return tilt;
	}
	
	/**
	 * Must have a special function here to get the height as the a landmark group may have a predefined height if a theodolite or other instrument was used to get bearings to landmarks
	 * @return the height of the image in meters
	 */
	private VRHeightData getImageHeight() {
		LandMarkGroup landmrkGroup=vrControl.getVRParams().getLandMarkDatas().get(vrControl.getVRParams().getCurrentLandMarkGroupIndex());
		Double height=landmrkGroup.getOriginHeight();
		VRHeightData heightDatas=new VRHeightData();
		
		if (height==null) {
			heightDatas= vrControl.getVRParams().getCurrentheightData().clone();
			heightDatas.height=heightDatas.height+vrControl.getTideManager().getHeightOffset();
		}
		else{
			heightDatas.name="forced landmark height ";
			heightDatas.height=height+vrControl.getTideManager().getHeightOffset();
		}
		
		return heightDatas;
	}
	
	/**
	 * Return the position the image was taken from. This is either the location from which bearings were determined for the current land mark group or uses the location manager to determine a position. 
	 * Note that in the event any landmark is defined by bearings rather than  a latlong then the position of the image has to be the location from which those bearings were calculated. 
	 * @return the position of the image.
	 */
	public LatLong getImagePos(){
		LandMarkGroup lndMrksGroup=getSelectedLMGroup();
		if (lndMrksGroup==null) return null; 
		//if the landmark group contains any landmarks which are define by bearings rather than a lat long and height then we must use the position these bearings were taken from. 
		if (lndMrksGroup.getOriginLatLong()!=null) return lndMrksGroup.getOriginLatLong();
		//otherwise defualt to the position manager.
		LatLong imagePos=getGPSinfo(); //get image position from location manager. 
		imagePos.setHeight(getImageHeight().height);//important to set correct height as used in some calc's here; 
		return imagePos;
	}


}
