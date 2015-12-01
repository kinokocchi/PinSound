package info.pinlab.snd.oal;

import info.pinlab.pinsound.WavClip;
import info.pinlab.pinsound.app.AudioPlayer;


public class OpenAlPlayerFactoryTest {
	static WavClip wav1, wav2;
	
	
	public static void main(String[] args) throws Exception{
//		BasicConfigurator.configure();
		//-- load sound files --//
		wav1 = AudioUtility.loadWav("sample.wav");
		wav2 = AudioUtility.loadWav("sample2.wav");
		

		OpenAlPlayerFactory factory = new OpenAlPlayerFactory();
		AudioPlayer player1 = new AudioPlayer();
		player1.setPlayerDevice(factory.getPlayer(wav1));
		AudioPlayer player2 = new AudioPlayer();
		player2.setPlayerDevice(factory.getPlayer(wav2));
		
		
		player1.reqPlay();
		player2.reqPlay();
		
		Thread.sleep(1000);
		
		factory.disposeAll();

		factory.disposeAll();
	}
	
}
