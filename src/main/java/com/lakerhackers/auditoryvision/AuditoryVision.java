package com.lakerhackers.auditoryvision;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.github.sarxos.webcam.Webcam;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.AudioFormat;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import com.ibm.watson.developer_cloud.text_to_speech.v1.util.WaveUtils;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier.VisualClass;

/*
 * @author Shakhar Dasgupta <sdasgupt@oswego.edu>
 */
public class AuditoryVision {

	private static String IMAGE_CAPTURE_PATH = "images.jpg";
	private static String SOUND_CAPTURE_PATH = "sound.wav";
	
	Properties properties;
	
	public AuditoryVision() {
		properties = new Properties();
		try {
			properties.load(new FileInputStream("local.properties"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public File capture() throws IOException {
		List<Webcam> webcams = Webcam.getWebcams();
		Webcam webcam = webcams.get(webcams.size() - 1);
		Dimension[] dimensions = webcam.getViewSizes();
		Dimension largest = dimensions[dimensions.length - 1];
		webcam.setViewSize(largest);
		webcam.open();
		File file = new File(IMAGE_CAPTURE_PATH);
		ImageIO.write(webcam.getImage(), "jpg", file);
		webcam.close();
		return file;
	}

	public String recognize(File file) {
		VisualRecognition service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
		service.setApiKey(properties.getProperty("VISUAL_RECOGNITION_API_KEY"));

		ClassifyImagesOptions options = new ClassifyImagesOptions.Builder().images(file).build();
		VisualClassification result = service.classify(options).execute();
		return createDescription(result);
	}

	private String createDescription(VisualClassification info) {
		List<VisualClass> vcs = info.getImages().get(0).getClassifiers().get(0).getClasses();

		vcs.sort((vc1, vc2) -> (int) Math.floor((vc2.getScore() - vc1.getScore()) * 100));

		VisualClass first = vcs.get(0);
		List<VisualClass> sndThrd = vcs.stream().skip(1).limit(2).collect(Collectors.toList());

		Double certainty = first.getScore() + sndThrd.stream().mapToDouble(vc -> vc.getScore()).sum();

		StringBuilder sb = new StringBuilder();

		if (certainty > 2.50) {
			sb.append("I see ");
		} else if (certainty > 2.00) {
			sb.append("I think I see ");
		} else {
			sb.append("It might be ");
		}

		sb.append(first.getName());
		for (VisualClass vc : sndThrd) {
			sb.append(", " + vc.getName());
		}

		return sb.toString();
	}

	public void speak(String text) {
		TextToSpeech service = new TextToSpeech();
		service.setUsernameAndPassword(properties.getProperty("TEXT_TO_SPEECH_USERNAME"), properties.getProperty("TEXT_TO_SPEECH_PASSWORD"));
		try {
			File file = new File(SOUND_CAPTURE_PATH);
			InputStream stream = service.synthesize(text, Voice.EN_ALLISON, AudioFormat.WAV).execute();
			InputStream in = WaveUtils.reWriteWaveHeader(stream);
			OutputStream out = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
			out.close();
			in.close();
			stream.close();
			play(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void play(File audioFile) {
		try {
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
			javax.sound.sampled.AudioFormat format = audioStream.getFormat();
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
			SourceDataLine audioLine = (SourceDataLine) AudioSystem.getLine(info);
			audioLine.open(format);
			audioLine.start();
			byte[] bytesBuffer = new byte[4096];
			int bytesRead = -1;

			while ((bytesRead = audioStream.read(bytesBuffer)) != -1) {
				audioLine.write(bytesBuffer, 0, bytesRead);
			}
			audioLine.drain();
			audioLine.close();
			audioStream.close();
		} catch (UnsupportedAudioFileException ex) {
			System.out.println("The specified audio file is not supported.");
			ex.printStackTrace();
		} catch (LineUnavailableException ex) {
			System.out.println("Audio line for playing back is unavailable.");
			ex.printStackTrace();
		} catch (IOException ex) {
			System.out.println("Error playing the audio file.");
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {
		AuditoryVision auditoryVision = new AuditoryVision();
		try {
			File file = auditoryVision.capture();
			String text = auditoryVision.recognize(file);
			System.out.println(text);
			auditoryVision.speak(text);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
