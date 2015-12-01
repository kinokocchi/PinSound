package info.pinlab.snd.oal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import info.pinlab.pinsound.RecorderDeviceController;
import info.pinlab.pinsound.WavClip;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class OpenAlRecorderTest{
	static RecorderDeviceController recDev;
	

	@BeforeClass
	public static void setUpBeforeClass(){
//		BasicConfigurator.configure();
		OpenAlRecorderFactory recFact = new OpenAlRecorderFactory();
		System.out.println("\nRecording devices\n=================");
		for(String dev : recFact.getRecorderDeviceNames()){
			System.out.println(dev);
		}
		
		recDev = OpenAlRecorder.getInstance();
		recDev.setRecFormat(1, 8000, 8);
//		recDev.setRecFormat(1, 16000, 8);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if(recDev !=null){
			OpenAlRecorder.disposeAll();
			//-- TODO causes OpenALException "Invalid context"
//			OpenAlPlayer.disposeAll(); 
		}
	}
		
	@Test
	public void testRecFunc() throws InterruptedException{
//		Runnable recWorker = new Runnable(){
//			@Override
//			public void run() {
//				System.out.println("  starting recording ... ");
//				recDev.startRec();
//				System.out.println("  recording stopped... ");
//			}
//		};
//		Thread recThread = new Thread(recWorker);
		System.out.println(" start thread");
		recDev.startRec();
//		recThread.start();
//		Thread.sleep(500);
		assertTrue(recDev.isRecording());
		Thread.sleep(500);
		System.out.println(" stop rec");
		recDev.stopRec();
		assertFalse(recDev.isRecording());
		
		WavClip wav =  recDev.getWavClip();
		assertFalse(recDev.isRecording());
		assertTrue(wav != null);
		long ms = wav.getDurInMs();
		System.out.println("   wav length is " + ms + " ms");
		assertTrue(ms > 0);
		
		Thread.sleep(1000);
		
	}
	
}
