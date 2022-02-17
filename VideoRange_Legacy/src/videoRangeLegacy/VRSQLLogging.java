package videoRangeLegacy;

import java.sql.Types;

import PamguardMVC.PamDataUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;
import generalDatabase.SQLTypes;

public class VRSQLLogging extends SQLLogging {

	VRControl vrControl;
	
	VRProcess vrProcess;
	
	PamTableDefinition vrTable;
	
	PamTableItem imageTime, range, rangeError, pixels, degrees, image, heightValue, heightName, 
		method, calibrationValue, calibrationName, imageAnimal, angleCorrection, animalBearing, comment,
		imageBearing, imagePitch, imageTilt,imageLat,imageLong,locLat,locLong,vrMethod;
	
	public VRSQLLogging(VRControl vrControl, VRProcess vrProcess) {
		super(vrProcess.getVrDataBlock());
		this.vrControl = vrControl;
		this.vrProcess = vrProcess;
		setUpdatePolicy(SQLLogging.UPDATE_POLICY_WRITENEW);
		
		vrTable = new PamTableDefinition(vrControl.getUnitName(), SQLLogging.UPDATE_POLICY_WRITENEW);
		//when the image was taken
		vrTable.addTableItem(imageTime 			= new PamTableItem("Image_Time", Types.DOUBLE));
		//information on animal location
		vrTable.addTableItem(animalBearing 		= new PamTableItem("Heading", Types.DOUBLE));
		vrTable.addTableItem(angleCorrection 	= new PamTableItem("Heading_Correction", Types.DOUBLE));
		vrTable.addTableItem(range				= new PamTableItem("Range", Types.DOUBLE));
		vrTable.addTableItem(rangeError 		= new PamTableItem("Range_Error", Types.DOUBLE));
		vrTable.addTableItem(locLat				= new PamTableItem("latitude", Types.DOUBLE));
		vrTable.addTableItem(locLong 			= new PamTableItem("longitude", Types.DOUBLE));
		vrTable.addTableItem(imageAnimal 		= new PamTableItem("Animal_No.", Types.INTEGER));

		//information on image
		vrTable.addTableItem(image 				= new PamTableItem("Image_Name", Types.CHAR, 20));
		vrTable.addTableItem(imageBearing 		= new PamTableItem("Image_Bearing", Types.DOUBLE));
		vrTable.addTableItem(imagePitch 		= new PamTableItem("Image_Pitch", Types.DOUBLE));
		vrTable.addTableItem(imageTilt 			= new PamTableItem("image_Tilt", Types.DOUBLE));
		vrTable.addTableItem(imageLat 			= new PamTableItem("image_Lat", Types.DOUBLE));
		vrTable.addTableItem(imageLong 			= new PamTableItem("image_Long", Types.DOUBLE));
		vrTable.addTableItem(heightValue	 	= new PamTableItem("Height", Types.DOUBLE));
		vrTable.addTableItem(heightName 		= new PamTableItem("Height Name", Types.CHAR, 20));
		//calc methods
		vrTable.addTableItem(vrMethod 			= new PamTableItem("VR_Method", Types.CHAR, 20));
		vrTable.addTableItem(method 			= new PamTableItem("Calc_Method", Types.CHAR, 20));
		vrTable.addTableItem(calibrationValue 	= new PamTableItem("Calibration", Types.DOUBLE));
		vrTable.addTableItem(calibrationName 	= new PamTableItem("Calibration Name", Types.CHAR, 20));
		//random stuff
		//--
		//comment on this image
		vrTable.addTableItem(comment = new PamTableItem("Comment", Types.CHAR, VRControl.DBCOMMENTLENGTH));
		
		setTableDefinition(vrTable);
	}

	@Override
	public void setTableData(SQLTypes sqlTypes, PamDataUnit pamDataUnit) {

		VRDataUnit vrDataUnit = (VRDataUnit) pamDataUnit;
		VRMeasurement vrm = vrDataUnit.getVrMeasurement();

		imageTime						.setValue(vrm.imageTime);
		angleCorrection					.setValue(vrm.angleCorrection);
		animalBearing					.setValue(vrm.locBearing);
		range							.setValue(vrm.locDistance);
		rangeError						.setValue(vrm.locDistanceError);
		if (vrm.imageOrigin!=null){
			locLat						.setValue(vrm.locLatLong.getLatitude());
			locLong						.setValue(vrm.locLatLong.getLongitude());
		}
		imageAnimal						.setValue(vrm.imageAnimal);
		image							.setValue(vrm.imageName);
		imageBearing					.setValue(vrm.imageBearing);
		imagePitch						.setValue(vrm.imagePitch);
		imageTilt						.setValue(vrm.imageTilt);
		if (vrm.imageOrigin!=null){
			imageLat					.setValue(vrm.imageOrigin.getLatitude());
			imageLong					.setValue(vrm.imageOrigin.getLongitude());
		}
		heightValue						.setValue(vrm.heightData.height);
		heightName						.setValue(vrm.heightData.name);
		vrMethod						.setValue(vrm.vrMethod.getName());
		method							.setValue(vrm.rangeMethod.getName());
		
		if (vrm.calibrationData!=null){
			calibrationValue			.setValue(vrm.calibrationData.degreesPerUnit);
		}
		
		angleCorrection					.setValue(vrm.angleCorrection);
		
		comment.setValue(vrm.comment);
	}
	
	
	@Override
	protected PamDataUnit createDataUnit(SQLTypes sqlTypes, long dataTime, int iD) {
		
		VRMeasurement vrm = new VRMeasurement();
		
		vrm.imageTime=(long) imageTime.getDoubleValue();
		
		VRDataUnit vrDataUnit=new VRDataUnit(dataTime,vrm);
		return vrDataUnit;
		
	}
	

}
