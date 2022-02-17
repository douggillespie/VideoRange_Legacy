package videoRangeLegacy.vrmethods;

import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.LayerUI;

import PamView.PamColors;
import PamView.dialog.PamDialog;
import PamView.dialog.PamLabel;
import PamView.panel.PamPanel;
import videoRangeLegacy.VRCalibrationData;
import videoRangeLegacy.VRControl;
import videoRangeLegacy.panels.VRCalibrationDialog;

public class AddCalibrationMethod extends AbstractVRMethod {
	
	private VRControl vrControl; 

	private Point calibratePoint1, calibratePoint2;
	
	public static final int CALIBRATE_1 = 4;
	public static final int CALIBRATE_2 = 5;
	
	private int currentStatus=CALIBRATE_1;

	private CalibrateMethodUI calibrateUI;

	//Side Panel components
	private PamPanel sidePanel;
	private PamLabel lab;
	private PamLabel instruction;
	private JButton clearCalibration;
	
	public AddCalibrationMethod(VRControl vrControl) {
		super(vrControl);
		this.vrControl=vrControl; 
		this.calibrateUI=new CalibrateMethodUI(vrControl);
		
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
		PamDialog.addComponent(panel, lab=new PamLabel("Calibration Measurement "), c);
		lab.setFont(PamColors.getInstance().getBoldFont());
		
		c.gridy++;
		c.gridx=0;
		c.gridwidth = 1;
		PamDialog.addComponent(panel, createCalibrationList(), c);

		c.gridy++;
		c.insets = new Insets(10,0,0,0);
//		PamDialog.addComponent(panel, instruction = new PamLabel(""), c);
//		instruction.setFont(PamColors.getInstance().getBoldFont());
		
		c.gridx=0;
		c.gridy++;
		c.gridwidth = 2;
		PamDialog.addComponent(panel, clearCalibration = new JButton("Clear Calibration"), c);
		c.insets=new Insets(0,0,0,0);
		clearCalibration.addActionListener(new ClearCalibration());
		c.gridy++;
		c.gridy++;
		
		newCalibration();
		
		return panel;	
	}
	
	private class ClearCalibration implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			clearOverlay();
		}
	}

	@Override
	public String getName() {
		return "Add Calibration";
	}

	@Override
	public PamPanel getSidePanel() {
		if (sidePanel==null) sidePanel=createSidePanel();
		return sidePanel;
	}

	@Override
	public LayerUI getJLayerOverlay() {
		return calibrateUI;
	}
	
	/**
	 * JLayer overlay panel for the horizon method. 
	 * @author Jamie Macaulay
	 *
	 */
	class CalibrateMethodUI extends VRAbstractLayerUI {
		
		private VRControl vrControl;
		Point currentMouse;

		public CalibrateMethodUI(VRControl vRControl){
			super(vRControl);
			this.vrControl=vRControl; 
		}
		
		 @Override
		 public void paint(Graphics g, JComponent c) {
		    super.paint(g, c);
		    addCalibrationMarks(g);
		}

		@Override
		public void mouseClick(Point mouseClick) {
			super.mouseClick(mouseClick);
			switch (currentStatus) {
			case CALIBRATE_1:
				System.out.println("Calibration Point 1: "+calibratePoint1);
				calibratePoint1 = new Point(mouseClick);
				setCurrentStatus(CALIBRATE_2);
				break;
			case CALIBRATE_2:
				System.out.println("Calibration Point 2: "+calibratePoint2);
				calibratePoint2 = new Point(mouseClick);
				vrControl.getVRPanel().repaint();
				newCalibrationVal();
				setCurrentStatus(CALIBRATE_1);
				break;
	
			}
			vrControl.getVRPanel().repaint();
		}
	}
	
	private void setCurrentStatus(int status){
		this.currentStatus=status;
	}  
	
	private void addCalibrationMarks(Graphics g) {
		System.out.println("Calibration Points: "+calibratePoint1+ "   "+calibratePoint2);
		drawMarksandLine(g, calibratePoint1, calibratePoint2, 
				vrControl.getVRPanel().calibrationSymbol.getPamSymbol(), true, calibrateUI.currentMouse);
	}
	
	private void newCalibrationVal() {
		if (calibratePoint1 == null || calibratePoint2 == null) {
			return;
		}
		VRCalibrationData newCalibration = VRCalibrationDialog.showDialog(null, vrControl, null);
		if (newCalibration != null){
			vrControl.getVRParams().setCurrentCalibration(newCalibration);
		}
		clearOverlay();
		vrControl.update(VRControl.SETTINGS_CHANGE);
	}

	@Override
	public void clearOverlay() {
		calibratePoint1= calibratePoint2=null;
		setCurrentStatus(CALIBRATE_1);
		vrControl.getVRPanel().repaint();
	}

	@Override
	public PamPanel getRibbonPanel() {
		return null;
	}

	@Override
	public PamPanel getSettingsPanel() {
		return null;
	}

	@Override
	public void update(int updateType) {
		super.update(updateType);
		
	}
	
	public Point getCalibrationPoint1(){
		return calibratePoint1;
	}
	
	public Point getCalibrationPoint2(){
		return calibratePoint2;
	}

}
