package videoRangeLegacy.vrmethods;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.plaf.LayerUI;

import videoRangeLegacy.VRControl;
import videoRangeLegacy.panels.VRPanel;
import PamView.PamColors;
import PamView.dialog.PamDialog;
import PamView.dialog.PamLabel;
import PamView.panel.PamPanel;

/**
 * The horizon method allows a user to click two points to define a horizon and then click an animal to define a RANGE. Requires a calibration value and a height. 
 * @author Doug Gillespie and Jamie Macaulay
 *
 */
public class VRHorizonMethod extends AbstractVRMethod {
	
	
	private VRControl vrControl;
	
	//panel components
	PamLabel lab;
	PamLabel calibrationData;
	PamLabel statusText;
	PamLabel instruction;
	JButton clearHorizon;
	JComboBox<String> calibrations;
	PamPanel sidePanel;
	
	//current status
	protected static final int MEASURE_HORIZON_1 = 0;
	protected static final int MEASURE_HORIZON_2 = 1;

	int currentStatus=MEASURE_HORIZON_1;
	
	private HorizonMethodUI horizonMethodUI;

	private ImageAnglePanel imageAnglePanel;
		
	public VRHorizonMethod(VRControl vrControl){
		super(vrControl); 
		this.vrControl=vrControl;
		this.horizonMethodUI=new HorizonMethodUI(vrControl);
	}

	@Override
	public PamPanel getSidePanel() {
		if (sidePanel==null) sidePanel=createSidePanel();
		return sidePanel;
	}

	@Override
	public PamPanel getSettingsPanel() {
		// TODO Auto-generated method stub
		return null;
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
		c.gridwidth = 1;
		PamDialog.addComponent(panel, lab=new PamLabel("Horizon Measurement "), c);
		lab.setFont(PamColors.getInstance().getBoldFont());
		
		c.gridy++;
		c.gridx=0;
		c.gridwidth = 1;
		PamDialog.addComponent(panel, createCalibrationList(), c);

		c.gridy++;
		PamDialog.addComponent(panel, instruction = new PamLabel("...."), c);
		instruction.setFont(PamColors.getInstance().getBoldFont());
		
		c.gridx=0;
		c.gridy++;
		c.gridwidth = 2;
		PamDialog.addComponent(panel, clearHorizon = new JButton("Clear Horizon"), c);
		clearHorizon.addActionListener(new ClearHorizon());
		c.gridy++;
		c.gridy++;
		
		newCalibration();
		
		setInstruction(currentStatus);
		
		return panel;
		
	}
	
	private class ClearHorizon implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			clearOverlay(); 
		}
	}
	
	public void update(int updateType){
		super.update(updateType);
		switch (updateType){
			case VRControl.HEADING_UPDATE:
				imageHeading=imageAnglePanel.getImageHeading(); 
			break;
			case VRControl.SETTINGS_CHANGE:
				imageAnglePanel.newCalibration();
				imageAnglePanel.newRefractMethod();
			break;
		}
	}

	
	/**
	 * Sets the text telling the user what to do. 
	 * @param status- the current status
	 */
	private void setInstruction(int status){

		switch (currentStatus) {
		case MEASURE_HORIZON_1:
			instruction.setText("Click horizon point 1");
			break;
		case MEASURE_HORIZON_2:
			instruction.setText("Click horizon point 2");
			break;
		case MEASURE_ANIMAL:
			imageAnglePanel.sayHorizonInfo() ;
			imageAnglePanel.setTiltLabel(horizonTilt);
			instruction.setText("Click animal");
			break;
		}
		//check there are no general instructions-e.g. no image
		setInstruction( instruction);
	}
	
	private void setVrStatus(int status){
		this.currentStatus=status;
		setInstruction(status);
	}
	
	@Override
	public String getName() {
		return "Measure Horizon";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public LayerUI getJLayerOverlay() {
		return horizonMethodUI;
	}

	@Override
	public PamPanel getRibbonPanel() {
		if (imageAnglePanel==null) {
			imageAnglePanel=new ImageAnglePanel(vrControl,this);
			imageAnglePanel.removeTiltSpinner();
		}
		return imageAnglePanel;
	}
	
	/**
	 * JLayer overlay panel for the horizon method. 
	 * @author Jamie Macaulay
	 *
	 */
	@SuppressWarnings("serial")
	private class HorizonMethodUI extends VRAbstractLayerUI {
		
		private VRControl vrControl;
		Point currentMouse; 

		public HorizonMethodUI(VRControl vRControl){
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
				case MEASURE_HORIZON_1:
					horizonPoint1 = new Point(mouseClick);
					setVrStatus(MEASURE_HORIZON_2);
					break;
				case MEASURE_HORIZON_2:
					horizonPoint2 = new Point(mouseClick);
					calculateHorizonTilt();
					setVrStatus(MEASURE_ANIMAL);
					break;
				case MEASURE_ANIMAL:
					newAnimalMeasurement_Horizon(mouseClick);
//					setVrStatus(MEASURE_DONE);
					break;
				}
				vrControl.getVRPanel().repaint();
		}
		 
	}
	
	@Override
	public void clearOverlay() {
		//clear points on the screen
		clearPoints();
		setVrStatus(MEASURE_HORIZON_1);
		//clear the image angle panel 
		imageAnglePanel.clearPanel();
		vrControl.getVRPanel().repaint();

	}


	private void addMeasurementMarks(Graphics g) {
		drawMarksandLine(g, horizonPoint1, horizonPoint2, 
				VRPanel.horizonSymbol.getPamSymbol(), vrControl.getVRParams().drawTempHorizon,horizonMethodUI.currentMouse);
	}

}



