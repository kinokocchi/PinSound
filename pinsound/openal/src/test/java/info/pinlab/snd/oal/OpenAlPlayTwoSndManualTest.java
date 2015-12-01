package info.pinlab.snd.oal;

import info.pinlab.pinsound.PlayerDeviceController;
import info.pinlab.pinsound.WavClip;
import info.pinlab.pinsound.app.AudioPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAlPlayTwoSndManualTest {
	static public Logger LOG = LoggerFactory.getLogger(OpenAlPlayTwoSndManualTest.class);
	static PlayerDeviceController playerDev;
	static WavClip wav1, wav2;


	
	public static void main(String[] args) throws Exception{
//		BasicConfigurator.configure();
		//-- load sound files --//
		wav1 = AudioUtility.loadWav("sample.wav");
		wav2 = AudioUtility.loadWav("sample2.wav");
		
		
		AudioPlayer player1 = new AudioPlayer(); 
		player1.setPlayerDevice(new OpenAlPlayer());
		AudioPlayer player2 = new AudioPlayer();
		player2.setPlayerDevice(new OpenAlPlayer());
		
		player1.setAudio(wav1);
		player2.setAudio(wav2);

		player1.reqPlay();
		player2.reqPlay();

		Thread.sleep(600); //-- listening time!
		player1.reqStop();
		player2.reqStop();
		
//		player1.dispose();
//		player2.dispose();
		
		OpenAlPlayer.disposeAll();
	}

}
