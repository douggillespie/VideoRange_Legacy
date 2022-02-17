package videoRangeLegacy.importTideData;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamUtils.TxtFileUtils;
import PamView.dialog.PamFileBrowser;
import videoRangeLegacy.VRControl;

/**
 * The tide manager open tide data and holds it in memory. If very large data sets are to be used then this will need to be changed. 
 * @author Jamie Macaulay
 *
 */
public class TideManager extends ImportTideData {
	
	private VRControl vrControl;
	private TideDataBlock tideDataBlock; 

	public TideManager(VRControl vrControl) {
		this.vrControl=vrControl; 
		this.tideDataBlock=new TideDataBlock();
	}
	

	public File getFile() {
		File file=vrControl.getVRParams().currTideFile;
		return file; 
	}

	
	/**
	 * Open a browser to find a file. The fuile must be a polpred text file. 
	 * @return 
	 */
	public String findFile(File lastFile) {
		String dir; 
		if (lastFile!=null){
				dir=lastFile.getParent();
		}
		else dir=null;
			
		String newFile=PamFileBrowser.fileBrowser(vrControl.getPamView().getGuiFrame(),dir,PamFileBrowser.OPEN_FILE,"txt");
		
		return newFile;
	}
	
	public void savePolpredTextFile(File file){
		if (file==null) return; 
		//surround with try since this is accessing external data:
		try{
			if (file.exists()) savePolpredTextFile( file.getPath());
		}
		catch (Exception e){
			e.printStackTrace();
		}

	}
	
	/**
	 * Go through the file and save data. 
	 */
	@SuppressWarnings("unchecked")
	public void savePolpredTextFile(String filePath){
	
		//load the file and create an array of strings
		ArrayList<ArrayList<String>> txtData= TxtFileUtils.importTxtDataToString(filePath,"\\s+");
		TxtFileUtils.printData(txtData);
		
		//next get the location
		LatLong latLong=getLocationPOLPRED(txtData.get(1));

		//now convert data from string to numvbers and add to datablock. 
		TideDataUnit tideDataUnit; 
		for (int i=0; i<txtData.size(); i++){
			tideDataUnit=convertToTideDataPOLPRED(txtData.get(i),latLong);
			if (tideDataUnit!=null){
				tideDataBlock.addPamData(tideDataUnit);
				System.out.println("Time: "+PamCalendar.formatDate(tideDataUnit.getTimeMilliseconds())+" Level: "+tideDataUnit.getLevel()+ " speed: "+tideDataUnit.getSpeed()+" direction: "+Math.toDegrees(tideDataUnit.getAngle()));
			}
		}
		
	}
	
	/**
	 * Get the height of the tide above mean sea level at a given time. 
	 * @param timeMillis
	 * @return
	 */
	public double getHeightOffset(long timeMillis){
		TideDataUnit dataUnit= getInterpData( timeMillis);
		if (dataUnit==null) return 0; 
		else return dataUnit.getLevel();
	}
	
	public TideDataUnit getInterpData(long timeMillis){
		return tideDataBlock.findInterpTideData(timeMillis);
	}
	
	public double getHeightOffset() {
		if (vrControl.getImageTime()==0) return 0; 
		return getHeightOffset(vrControl.getImageTime());
	}


	
	public static LatLong getLocationPOLPRED(ArrayList<String> tideData){
		
		System.out.println("Latlong: String: "+tideData);
		String latString=(tideData.get(1)+ tideData.get(2).substring(0, tideData.get(2).length()-1) + tideData.get(3).replace("\"", "'")+ " "+tideData.get(4));
		String longString=(tideData.get(5)+ tideData.get(6).substring(0, tideData.get(6).length()-1) + tideData.get(7).replace("\"", "'")+ " "+tideData.get(8));
		System.out.println("Latlong: Formatted String:  "+latString+ "  "+longString);
		
		LatLong latLong=new LatLong((latString+","+longString));
		
		return latLong; 
	}

	
	/**
	 * Convert string data from a POLPRED output file.  
	 * @param tideData
	 * @return
	 */
	public static TideDataUnit convertToTideDataPOLPRED(ArrayList<String> tideData, LatLong location){
		
		try{
		if (tideData.size()==7) tideData.remove(0);
		if (tideData.size()==6){
			////date/////
			//day
			int length0=tideData.get(0).length();
			int day=Integer.valueOf(tideData.get(0).substring(0, length0-1));
			//month
			String[] splitString=tideData.get(1).split("/");
			int month=Integer.valueOf(splitString[0]);
			//year
			int year=Integer.valueOf(splitString[1]);
			
			/////time/////
			//hour
			String[] splitStringTime=tideData.get(2).split(":");
			int hour=Integer.valueOf(splitStringTime[0]);
			//minute
			int minute=Integer.valueOf(splitStringTime[0]);
			
			//convert to time (millis)
			Calendar cal = new GregorianCalendar ();
			cal.set(year,month-1,day,hour,minute,0);
			Date date=cal.getTime();
			long time=date.getTime();
			
			//now convert other data
			double level=Double.valueOf(tideData.get(3));
			double speed=Double.valueOf(tideData.get(4));
			double direction=Math.toRadians(Double.valueOf(tideData.get(5)));
			
			TideDataUnit tideDataUnit=new TideDataUnit(time, level, speed, direction, location);

			return tideDataUnit;
			
		}
		return null; 
		}
		catch (Exception e){
			e.printStackTrace();
			return null; 
		}
		
	}
}
