package info.pinlab.snd.oal;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import info.pinlab.pinsound.PlayerDeviceController;
import info.pinlab.pinsound.WavClip;
import info.pinlab.pinsound.app.AudioPlayer;
import info.pinlab.snd.app.AudioPlayerTest;
import info.pinlab.snd.app.CliAudioPlayerView;

import java.io.InputStream;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class OpenAlPlayerTest{
//	static public Logger logger = LoggerFacto.getLogger(OpenAlPlayerTest.class);
	static WavClip wav;
	static PlayerDeviceController playerDev;

	static{
//		BasicConfigurator.configure();
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		String path = "sample.wav";
		InputStream is = AudioPlayerTest.class.getResourceAsStream(path);
		if(is==null){
			is = AudioPlayerTest.class.getClassLoader().getResourceAsStream(path);
		}
		assertTrue( "Can't read wav file!", is!=null);
		wav = new WavClip(is);
		
		playerDev = new OpenAlPlayer();
	}

	@Test
	public void testIsPlayingWithNaturalEnding() throws Exception {
		final AudioPlayer player = new AudioPlayer();
		player.setPlayerDevice(playerDev);
		player.setAudioPlayerView(new CliAudioPlayerView());
		player.setAudio(wav);
		
		assertFalse(player.isPlaying());
		player.reqPlay();			// PLAY
		player.setAfterPlayHook(new Runnable() {
			@Override
			public void run() {
				assertFalse(player.isPlaying());
			}
		});
		Thread.sleep(wav.getDurInMs() + 100); //-- wait for the end
		player.dispose();
	}
	


	@Test
	public void testIsPlayingWithTwoSounds() throws Exception{
		final AudioPlayer player = new AudioPlayer();
		player.setPlayerDevice(playerDev);
		player.setAudioPlayerView(new CliAudioPlayerView());
		player.setAudio(wav);

		System.out.println("[DEBUG]   " + "testIsPlayingWithTwoSounds - PLAY TEST");
		assertFalse(player.isPlaying());
		player.reqPlay();			// PLAY
		assertTrue(player.isPlaying());
		//-- wait till the end
		System.out.println("[DEBUG]   " + "testIsPlayingWithTwoSounds - FNISHED TEST");
		Thread.sleep(wav.getDurInMs() + 100); //-- wait for the end
		assertFalse(player.isPlaying());

		//-- play again!
		System.out.println("[DEBUG]   " + "testIsPlayingWithTwoSounds - REPLAY TEST");
		player.reqPlay();			// PLAY
		assertTrue(player.isPlaying());
		Thread.sleep(wav.getDurInMs() + 100); //-- wait for the end
		assertFalse(player.isPlaying());
		
		System.out.println("[DEBUG]   " + "testIsPlayingWithTwoSounds - dispose");
		player.dispose();
		assertFalse(player.isPlaying());
	}

	
	
	@Test
	public void testIsPlayingWithForcedStop() throws Exception {
		Thread.sleep(500);
		AudioPlayer player = new AudioPlayer();
		player.setPlayerDevice(playerDev);
		player.setAudioPlayerView(new CliAudioPlayerView());
		player.setAudio(wav);
		
		assertFalse(player.isPlaying());
		
		player.reqPlay();			// PLAY
		assertTrue(player.isPlaying());
		Thread.sleep(400);

		player.reqPauseToggle();  	// PAUSE
		assertFalse(player.isPlaying());
		Thread.sleep(200);

		player.reqPauseToggle();  	// PLAY
		assertTrue(player.isPlaying());
		Thread.sleep(400);

		player.reqPauseToggle();  	// PAUSE
		assertFalse(player.isPlaying());
		Thread.sleep(200);

		
		player.reqStop();			// STOP
		assertFalse(player.isPlaying());

		player.dispose();
		assertFalse(player.isPlaying());
		Thread.sleep(100); //-- give time to shut down
		assertFalse(player.isPlaying());
	}
	
	
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception{
		OpenAlPlayer.disposeAll();
	}

}
