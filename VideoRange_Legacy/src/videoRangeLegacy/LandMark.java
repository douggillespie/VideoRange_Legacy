package videoRangeLegacy;

import java.io.Serializable;

import PamUtils.LatLong;

/**
 * Class for storing Landmark information. A landmark may be known exactly using GPS information, however a landmark may only have a bearing and pitch from a specified location if not possible to fix and exact location. 
 * @author Jamie Macaulay
 *
 */
public class LandMark extends Object implements Serializable, Cloneable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Name of the Landmark	
	 */
	private String name;
	/**
	 * Position of theLandMark including height.
	 */
	private LatLong position;
	
	/**
	 * Bearing to a LandMark
	 */
	private Double bearing;
	/**
	 * Pitch to LandMark
	 */
	private Double pitch;
	/**
	 * LatLong and height from which bearing measurment was taken
	 */
	private LatLong latLongOrigin; 
	
	public LandMark(){
		
	}

	public LandMark(String name, LatLong position, Double height){
		this.name=name;
		this.position=position; 
		position.setHeight(height);
	}
	
	
	public LandMark(String name, double bearing, double pitch , LatLong latLongOrigin){
		this.bearing=bearing;
		this.pitch=pitch;
		this.latLongOrigin=latLongOrigin;
	}
	
	public void update(LandMark newData) {
		bearing=newData.getBearing();
		pitch=newData.getPitch();
		latLongOrigin=newData.getLatLongOrigin();
		position=newData.getPosition();
		name = new String(newData.name);
	}
	
	public String getName() {
		return name;
	}

	public LatLong getPosition() {
		return position;
	}
	
	public double getHeight() {
		return position.getHeight();
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPosition(LatLong position) {
		this.position = position;
	}
	
	public Double getBearing() {
		return bearing;
	}

	public Double getPitch() {
		return pitch;
	}

	public Double getHeightOrigin() {
		if (latLongOrigin==null) return null; 
		return latLongOrigin.getHeight();
	}

	public LatLong getLatLongOrigin() {
		return latLongOrigin;
	}

	public void setBearing(Double bearing) {
		this.bearing = bearing;
	}

	public void setPitch(Double pitch) {
		this.pitch = pitch;
	}

	public void setHeightOrigin(Double heightOrigin) {
		this.latLongOrigin.setHeight(heightOrigin);
	}

	public void setLatLongOrigin(LatLong latLongOrigin) {
		this.latLongOrigin = latLongOrigin;
	}

	
	@Override
	public LandMark clone() {
		try {
			return (LandMark) super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	

}
