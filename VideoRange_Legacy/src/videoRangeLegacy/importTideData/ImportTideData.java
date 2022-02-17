package videoRangeLegacy.importTideData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Class for importing tide data. 
 * @author Jamie Macaulay
 *
 */
public class ImportTideData {

	public ImportTideData() {
		// TODO Auto-generated constructor stub
	}
	
	public void importTideFile(File file) throws FileNotFoundException{
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("myfile.tab")));

		String line = null;

		try {
			while((line = reader.readLine()) != null) {
			    String[] fiedls = line.split("\\t");
			    // generate your SQL insert statement here
			    // execute SQL insert.
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
