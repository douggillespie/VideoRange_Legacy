package videoRangeLegacy;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataUnit;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;


/**
 * Class which holds photos or other images and reads metadata.
 * @author Jamie Macaulay
 *
 */
public class PamImage extends PamDataUnit {
	
	//image
	private File imageFile;
	private Metadata metadata;
	private BufferedImage image; 
	private boolean imageOK=false; 
	
	//metadata
	private ArrayList<String> metaDataText=new ArrayList<String>();
	private Date date;
	private LatLong geoTag; 
	
	/**
	 * Open a photo an dcalculate metadata information. 
	 * @param imageFile
	 * @throws ImageProcessingException
	 * @throws IOException
	 */
	public PamImage(File imageFile){
		super(0);
		this.imageFile=imageFile; 
		//try and load the image
		try {
			long time0=System.currentTimeMillis();
			image = ImageIO.read(imageFile);
			long time1=System.currentTimeMillis();
			System.out.println("PamImage: ImageIO.read(file): "+(time1-time0));

		}
		catch (IOException ex) {
			ex.printStackTrace();
			return;
		}
		//try and load the metadata
		try {
			this.metadata = ImageMetadataReader.readMetadata(imageFile);
		}
		catch (Exception e){
			e.printStackTrace();
		}
		processPhotoData();
		if (image!=null) imageOK=true; 
	}
	
	/**
	 * Sometimes a photo will have no metadata. In this case the constructor can just consist of the image. 
	 * @param bufferedImage
	 */
	public PamImage(BufferedImage bufferedImage) {
		super(0);
		this.image=bufferedImage;
		if (image!=null) imageOK=true; 
	}
	
	/**
	 * Find the relevent data within the photos metadata. 
	 */
	public void processPhotoData(){
		
	     try{
	            // Read Exif Data
	            Directory directory = metadata.getFirstDirectoryOfType( ExifSubIFDDirectory.class );
	            if( directory != null ){
	                // Read the date
	            	//we want PAMGUARD to assume any time in the meta data is UTC. 
	            	date=directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, TimeZone.getTimeZone("UTC"));
//	            	date=directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);

	    
	            	super.setTimeMilliseconds(date.getTime());
//	                int year = df.getCalendar().get( Calendar.YEAR );
//	                int month = df.getCalendar().get( Calendar.MONTH ) + 1;
	            }
	                
	             // Read gps data
	            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
	            if (gpsDirectory!=null){
		            GeoLocation geoLocation = gpsDirectory.getGeoLocation();
		            if (geoLocation == null || geoLocation.isZero()) geoTag=null;   
		            else geoTag=new LatLong(geoLocation.getLatitude(), geoLocation.getLongitude());
	            }
	                
	            //read all the metadata as text
	            for (Directory direc : metadata.getDirectories()) {
	                   for (Tag tag : direc.getTags()) {
	                    metaDataText.add( "\t" + tag.getTagName() + " = " + tag.getDescription() );
	                  }
	            }   

	       }
	       catch( Exception e ){	
	        	System.out.println( "Could not read metadata: " + e.getMessage());
//	            e.printStackTrace();
	        }
		
	}
	
	public BufferedImage getImage(){
		return image;
	}
	
	public boolean imageOK(){
		return imageOK;
	}
	
	public File getImageFile(){
		return imageFile;
	}

	/**
	 * Get an array of strings describing metadata for this photgraph. 
	 * @return
	 */
	public ArrayList<String> getMetaDataText() {
		return metaDataText;
	}
	
	///metadata///
	
	public String getName(){
		if (imageFile!=null) return imageFile.getName();
		else return image.toString();
	}
	
	public String getTimeString(){
		if (date==null) return null; 
       return (PamCalendar.formatDateTime(super.getTimeMilliseconds()));
	}

	/**
	 * Get the gep tag for this image. Returns null if no geotag is avalaible. 
	 * @return
	 */
	public LatLong getGeoTag() {
		return geoTag;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date=date;
	}

	
	
	

	
}
