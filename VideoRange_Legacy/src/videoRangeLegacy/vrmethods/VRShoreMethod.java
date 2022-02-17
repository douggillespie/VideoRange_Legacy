package videoRangeLegacy.vrmethods;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.plaf.LayerUI;

import Map.MapContour;
import Map.MapFileManager;
import PamUtils.LatLong;
import PamUtils.PamUtils;
import PamView.PamColors;
import PamView.PamSymbol;
import PamView.PamSymbolType;
import PamView.dialog.PamDialog;
import PamView.dialog.PamLabel;
import PamView.panel.PamPanel;
import videoRangeLegacy.ShoreManager;
import videoRangeLegacy.VRCalibrationData;
import videoRangeLegacy.VRControl;
import videoRangeLegacy.VRHeightData;
import videoRangeLegacy.VRHorzCalcMethod;
import videoRangeLegacy.panels.VRPanel;

/**
 * AbstractVRMethod contains many useful functions to create panels, update methods , 
 * @author Jamie Macaulay
 *
 */
public class VRShoreMethod extends AbstractVRMethod {
	
	//shore manager
	private  ShoreManager shoreManager;	
	private double[] shoreRanges;

	//PamPanel components
	private PamLabel lab;
	private JButton clearShore;
	PamPanel sidePanel;
	
	protected static final int NEED_GPS_MAP=5;
	protected static final int NEED_BEARING=6;
	protected static final int MEASURE_SHORE = 4;
	
	///Symbols.
	private Color landColour = Color.BLACK;
	private PamSymbol landPointSymbol = new PamSymbol(PamSymbolType.SYMBOL_POINT, 1, 1, true, Color.RED, Color.RED);
	
	private int currentStatus=4; 
	
	private VRCalibrationData calData;

	private ShoreMethodUI shoreMethodUI;
	private PamLabel instruction;
	private JButton mapSettings;
	private JTextField gps;
	private JButton gpsLocSettings;
	private Point shorePoint;
	private ImageAnglePanel imageAnglePanel;



	public VRShoreMethod(VRControl vrControl) {
		super(vrControl);
		this.vrControl=vrControl;
		this.shoreManager = new ShoreManager(vrControl.getMapFileManager());
		this.shoreMethodUI=new ShoreMethodUI(vrControl);
		this.imageAnglePanel=new ImageAnglePanel(vrControl,this);
		this.sidePanel=createSidePanel();

	}

	@Override
	public String getName() {
		return "Shore Method";
	}

	@Override
	public PamPanel getSidePanel() {
		return sidePanel;
	}
	
	private PamPanel createSidePanel(){
		
		PamPanel panel=new PamPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		
		c.gridx=0;
		c.gridy=0;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, lab=new PamLabel("Shore Measurement "), c);
		lab.setFont(PamColors.getInstance().getBoldFont());
		
		c.gridy++;
		c.gridx=0;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, createCalibrationList(), c);

		c.gridy++;
		PamDialog.addComponent(panel, createMapFilePanel(), c);
	
		c.gridy++;
		PamDialog.addComponent(panel, createLocationListPanel(), c);

		c.gridy++;
		c.gridx=0;
		PamDialog.addComponent(panel, instruction = new PamLabel("...."), c);
		instruction.setFont(PamColors.getInstance().getBoldFont());
		
		c.gridx=0;
		c.gridy++;
		c.gridwidth = 4;
		PamDialog.addComponent(panel, clearShore = new JButton("Clear Shore"), c);
		clearShore.addActionListener(new ClearShore());
		c.gridy++;
		c.gridy++;
		
		newCalibration();
		
		setInstruction(currentStatus);
		
		return panel;
	}
	
	private class ClearShore implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			 clearOverlay(); 
		}
	}
	
	private void clearShore(){
		clearPoints();
		horizonPointsFromShore(horizonTilt);
		setVrSubStatus(MEASURE_SHORE);
	}
	
	@Override
	protected void clearPoints() {
		super.clearPoints(); 
		shorePoint=null; 
	}
	

//	private void selectGPS(){
//	
//	}
	

	@Override
	public LayerUI getJLayerOverlay() {
		return shoreMethodUI;
	}
	
	/**
	 * JLayer overlay panel for the horizon method. 
	 * @author Jamie Macaulay
	 *
	 */
	class ShoreMethodUI extends VRAbstractLayerUI {
		
		private VRControl vrControl;
		Point currentMouse; 

		public ShoreMethodUI(VRControl vRControl){
			super(vRControl);
			this.vrControl=vRControl; 
		}
		
		 @Override
		 public void paint(Graphics g, JComponent c) {
		    super.paint(g, c);

			if (vrControl.getVRParams().getShowShore()) {
				drawLand(g);
			}
			
			addMeasurementMarks(g);
			addAnimals(g);
		}

		@Override
		public void mouseClick(Point mouseClick) {
			super.mouseClick(mouseClick);
			switch (currentStatus) {
				case MEASURE_SHORE:
					System.out.println("shore Point clicked: "+currentStatus);
					shorePoint = new Point(mouseClick);
					if (horizonPointsFromShore(horizonTilt)) {
						setVrSubStatus(MEASURE_ANIMAL);
					}
					else shorePoint=null; 
					break;
				
				case MEASURE_ANIMAL:
					System.out.println("Measure Animal from  shore: "+currentStatus);
					newAnimalMeasurement_Shore(mouseClick);
				break;
			}
			vrControl.getVRPanel().repaint();
		}
		 
	}

	
	/**
	 * Sets the text telling the user what to do. 
	 * @param status- the current status
	 */
	private void setInstruction(int status){

		switch (currentStatus) {
		case MEASURE_SHORE:
			instruction.setText("Click on shore");
			break;
		case MEASURE_ANIMAL:
			instruction.setText("Click animal");
			break;
		}
		//check if there are problems with required params
		if (getGPSinfo(vrControl.getImageTime())==null) instruction.setText("No GPS info");
		if (vrControl.getMapFileManager().getAvailableContours()==null || vrControl.getMapFileManager().getContourCount() <=1) instruction.setText("No Map Points");
		if (imageAnglePanel.getAngle()==null) instruction.setText("No bearing info");


		//check there are no general instructions-e.g. no image
		setInstruction(instruction);
		
	}
	
	private void setVrSubStatus(int status) {
		this.currentStatus=status;
		setInstruction(status);
	}
	
	private void addMeasurementMarks(Graphics g) {
		drawMarksandLine(g, horizonPoint1, horizonPoint2, 
				VRPanel.horizonSymbol.getPamSymbol(), vrControl.getVRParams().drawTempHorizon, shoreMethodUI.currentMouse);
	}
	
	private boolean newAnimalMeasurement_Shore(Point animalPoint) {
		return newAnimalMeasurement_Horizon(animalPoint);
	}
	
	@Override
	protected void drawMarksandLine(Graphics g, Point p1, Point p2, PamSymbol symbol, boolean drawLine, Point currentMouse) {
		super.drawMarksandLine(g, p1, p2, symbol, drawLine, currentMouse);
		Point sp1 = shorePoint;
		if (sp1 != null) {
			symbol.draw(g, vrControl.getVRPanel().imageToScreen(sp1));
		}
		
	}

	/**
	 * Draw the land outline
	 * <p>
	 * Requires knowledge of the horizon or some point on the land and 
	 * also the angle for this to work. 
	 * @param g graphics handle
	 */
	private void drawLand(Graphics g) {
		/**
		 * Will have to get the positions of the land at each x pixel. For this
		 * we will need to convert a distance to a y coordinate. 
		 * The most general way of doing this will be to have a horizon pixel for
		 * any point, and then work out the angle below the horizon for a given angle. 
		 * (can't plot anything above the horizon of course). 
		 * If we're working to the shore, then work out where the horizon should be before 
		 * plotting. 
		 */
		Double landAngle = imageHeading;
		if (landAngle == null) {
			return;
		}
		// check that there are some reference points before drawing
		if (getHorizonPixel(vrControl.getVRPanel().getImageWidth()/2) == null) {
			return; 
		}
		LatLong origin = getGPSinfo();
		MapFileManager mapManager = shoreManager.getMapFileManager();
		if (mapManager == null) {
			return;
		}
		VRHeightData heightData = vrControl.getVRParams().getCurrentheightData();
		if (heightData == null) {
			return;
		}
		
		calData = vrControl.getVRParams().getCurrentCalibrationData();
		if (calData == null) {
			return;
		}
		g.setColor(Color.BLACK);
		Vector<LatLong> contour;
		MapContour mapContour;
		for (int i = 0; i < mapManager.getContourCount(); i++) {
			mapContour = mapManager.getMapContour(i);
			contour = mapContour.getLatLongs();
			for (int l = 0; l < contour.size()-1; l++) {
				drawMapSegment(g, origin, heightData.height, calData.degreesPerUnit, landAngle, contour.get(l), contour.get(l+1));
			}
		}
	}
	
	private void drawMapSegment(Graphics g, LatLong origin, double height, double degreesPerUnit,
	double imageAngle, LatLong ll1, LatLong ll2) {
		Point p1, p2;
		p1 = getObjectPoint(origin, height, degreesPerUnit, imageAngle, ll1,vrControl.getRangeMethods().getCurrentMethod());
		p2 = getObjectPoint(origin, height, degreesPerUnit, imageAngle, ll2,vrControl.getRangeMethods().getCurrentMethod());
		if (p1 == null || p2 == null) {
			return;
		}
		if (p1.x < 0 && p2.x < 0) {
			return;
		}
		if (p1.x > vrControl.getVRPanel().getImageWidth() && p2.x > vrControl.getVRPanel().getImageWidth()) {
			return;
		}
		p1 = vrControl.getVRPanel().imageToScreen(p1);
		p2 = vrControl.getVRPanel().imageToScreen(p2);
		g.setColor(landColour);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
		if (vrControl.getVRParams().showShorePoints) {
			landPointSymbol.draw(g, p1);
			landPointSymbol.draw(g, p2);
		}
	}
	
	
	private Point getObjectPoint(LatLong origin, double height, double degreesPerUnit, 
			double imageAngle, LatLong objectLL, VRHorzCalcMethod vrRangeMethod) {
		if (vrRangeMethod == null) {
			return null;
		}
		double range = origin.distanceToMetres(objectLL);
		double angle = vrRangeMethod.getAngle(height, range);
		if (angle < 0) {
			return null;
		}
		double bearing = origin.bearingTo(objectLL);
		double angDiff = PamUtils.constrainedAngle(imageAngle - bearing, 180);
		if (Math.abs(angDiff) > 90) {
			return null;
		}
		int x = bearingTox(imageAngle, bearing);
		int y = (int) (getHorizonPixel(x) + vrRangeMethod.getAngle(height, range) * 180 / Math.PI / degreesPerUnit);
		return new Point(x,y);
	}
	
	private void calcShoreRanges(){
		if (imageHeading==null) {
			shoreRanges=null;
			return; 
		}
		shoreRanges = shoreManager.getSortedShoreRanges(getGPSinfo(), imageHeading);
	}

	
	private boolean horizonPointsFromShore(double tilt) {
		// work out where the horizon should be based on the shore point. 
		horizonPoint1 = horizonPoint2 = null;
		System.out.println("shore Point: "+shorePoint);
		if (shorePoint == null) {
			return false;
		}
		int imageWidth = vrControl.getVRPanel().getImageWidth();
		if (imageWidth == 0) {
			return false;
		}
		VRCalibrationData calData = vrControl.getVRParams().getCurrentCalibrationData();
		VRHeightData heightData = vrControl.getVRParams().getCurrentheightData();
		VRHorzCalcMethod vrRangeMethod = vrControl.getRangeMethods().getCurrentMethod();
		Double imageBearing = getImageHeading();

		if (vrRangeMethod == null || calData == null || heightData == null || imageBearing == null) {
			return false;
		}
		double pointBearing = imageBearing + (shorePoint.x - imageWidth/2) * calData.degreesPerUnit;
		
		double[] ranges = shoreManager.getSortedShoreRanges(getGPSinfo(), pointBearing);
		
		Double range = getshoreRange(ranges);

		if (range == null) {
			return false;
		}
		// dip from horizon to this point. 
		Double angleTo = vrRangeMethod.getAngle(heightData.height, range);
		if (angleTo < 0) {
			// over horizon
			return false; 
		}
		int y =  (int) (shorePoint.y - angleTo * 180 / Math.PI / calData.degreesPerUnit);
		double xD = shorePoint.x;
		horizonPoint1 = new Point(0, y + (int) (xD * Math.tan(getHorizonTilt() * Math.PI / 180)));
		xD = imageWidth - shorePoint.x;
		horizonPoint2 = new Point(imageWidth, y - (int) (xD * Math.tan(getHorizonTilt() * Math.PI / 180)));
		return true;
	}
	
	/**
	 * Get the shore range we want to use - not necessarily the closest. 
	 * @return shore range to use in VR calculations. 
	 */
	public Double getShoreRange() {
		return getshoreRange(shoreRanges);
	}
	
	public Double getshoreRange(double[] ranges) {
		int want = 0;
		if (ranges == null) {
			return null;
		}
		if (vrControl.getVRParams().ignoreClosest) {
			want = 1;
		}
		if (ranges.length < want+1) {
			return null;
		}
		return ranges[want];
	}
	
	@Override
	public void clearOverlay() {
		clearShore();
		vrControl.getVRPanel().repaint();
	}

	@Override
	public PamPanel getRibbonPanel() {
		return imageAnglePanel;
	}

	@Override
	public PamPanel getSettingsPanel() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void update(int updateType){
		super.update(updateType);
		switch (updateType){
			case VRControl.SETTINGS_CHANGE:
				 calcShoreRanges();
				 vrControl.getVRPanel().repaint();
				 break;
			case VRControl.HEADING_UPDATE:
				imageHeading=imageAnglePanel.getImageHeading(); 
				calcShoreRanges();
				horizonPointsFromShore(horizonTilt);
				vrControl.getVRPanel().repaint();
				break;
			case VRControl.TILT_UPDATE:
				horizonTilt=imageAnglePanel.getTilt();
				horizonPointsFromShore(horizonTilt);
				vrControl.getVRPanel().repaint();
				break;
			case VRControl.IMAGE_CHANGE:
				break;
		}
		setInstruction(currentStatus);
	}
	

}
