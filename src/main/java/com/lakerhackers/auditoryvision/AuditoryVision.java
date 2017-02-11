package com.lakerhackers.auditoryvision;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import com.github.sarxos.webcam.Webcam;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;

/*
 * @author Shakhar Dasgupta <sdasgupt@oswego.edu>
 */
public class AuditoryVision {
	
	private static String API_KEY = "7e2f8ff046cedd2ea0ef4ee72fe825ac39a1db13";
	private static String CAPTURE_PATH = "images.jpg";
	
	public File capture() throws IOException {
		Webcam webcam = Webcam.getDefault();
		Dimension[] dimensions = webcam.getViewSizes();
		Dimension largest = dimensions[dimensions.length - 1];
		webcam.setViewSize(largest);
		webcam.open();
		File file = new File(CAPTURE_PATH);
		ImageIO.write(webcam.getImage(), "jpg", file);
		return file;
	}
	
	public String recognize(File file) {
		VisualRecognition service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
		service.setApiKey(API_KEY);

		ClassifyImagesOptions options = new ClassifyImagesOptions.Builder()
		    .images(file)
		    .build();
		VisualClassification result = service.classify(options).execute();
		return result.toString();
	}
	
	
	public static void main(String[] args) {
		AuditoryVision auditoryVision = new AuditoryVision();
		try {
			File file = auditoryVision.capture();
			System.out.println(auditoryVision.recognize(file));
			file.delete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
