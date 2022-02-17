package videoRangeLegacy.vrmethods;

import java.awt.AWTEvent;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.plaf.LayerUI;

import videoRangeLegacy.VRControl;

class VRAbstractLayerUI extends LayerUI<JPanel> {
	
	private static final long serialVersionUID = 1L;
	
	private VRControl vrControl;
	private Point currentMouse; 
 
	public VRAbstractLayerUI(VRControl vRControl){
		this.vrControl=vRControl;
	}
	
	  @Override
	  public void installUI(JComponent c) {
	    super.installUI(c);
	    JLayer jlayer = (JLayer)c;
	    jlayer.setLayerEventMask(
	      AWTEvent.MOUSE_EVENT_MASK |
	      AWTEvent.MOUSE_MOTION_EVENT_MASK
	    );
	  }

	  @Override
	  public void uninstallUI(JComponent c) {
	    JLayer jlayer = (JLayer)c;
	    jlayer.setLayerEventMask(0);
	    super.uninstallUI(c);
	  }


	 @Override
	 public void paint(Graphics g, JComponent c) {
		 super.paint(g, c);
		 Graphics2D g2 = (Graphics2D) g;
		  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		          RenderingHints.VALUE_ANTIALIAS_ON);
		        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
		          RenderingHints.VALUE_RENDER_QUALITY);
		

	}
	 
	///////////////////mouse events/////////////////////
	@Override
	protected void processMouseMotionEvent(MouseEvent e, JLayer l) {
		vrControl.newMousePoint(vrControl.getVRPanel().screenToImage(e.getPoint()));
		currentMouse = e.getPoint();
	//	checkHoverText(e.getPoint());
	}

	@Override
	protected void processMouseEvent(MouseEvent e, JLayer l) {
//		System.out.println("mouse event");
		
		if (e.getID() == MouseEvent.MOUSE_ENTERED){
			
		} 
		if (e.getID() == MouseEvent.MOUSE_EXITED){
			vrControl.newMousePoint(null);
			currentMouse = null;
		}
		if (e.getID() == MouseEvent.MOUSE_CLICKED){
			if (e.getButton() == MouseEvent.BUTTON1) {
				//System.out.println("mouse clicked");
				mouseClick(vrControl.getVRPanel().screenToImage(e.getPoint()));
			}
		} 
	}
	
	protected void mouseClick(Point point){
		
	}


}

