package info.pinlab.snd.app;

import static org.junit.Assert.assertTrue;
import info.pinlab.pinsound.PlayerDeviceController;
import info.pinlab.pinsound.WavClip;
import info.pinlab.pinsound.app.AudioPlayer;
import info.pinlab.pinsound.app.AudioPlayerListener;
import info.pinlab.pinsound.app.AudioPlayerView;
import info.pinlab.snd.oal.OpenAlPlayer;

import java.io.InputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AudioPlayerTest {
	static WavClip wav;
	static PlayerDeviceController playerDev;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception{
		String path = "sample.wav";
//		InputStream is = AudioPlayerTest.class.getClassLoader().getResourceAsStream(path);
		InputStream is = AudioPlayerTest.class.getResourceAsStream(path);
		assertTrue( "Can't read wav file!", is!=null);
		wav = new WavClip(is);
		
		playerDev = new OpenAlPlayer();
	}

	
	@AfterClass
	public static void tearDownAfterClass() throws Exception{
		OpenAlPlayer.disposeAll();
	}
	
	static class CliView implements AudioPlayerView{
		@Override
		public void setPlayingState() {	System.out.println("..playing");		}
		@Override
		public void setReadyToPlayState() {	System.out.println("..stopped");		}
//		@Override
//		public void setPauseState() {	System.out.println("..paused");			}
		@Override
		public void setPlayMaxLenInMs(long ms) {		}
		@Override
		public void setPlayPosInMs(long ms) {
			System.out.println(ms +"ms");
		}
		@Override
		public void setPlayEnabled(boolean b) {
		}
		@Override
		public void setPlayActionListener(AudioPlayerListener l) {
		}
	}
	
	
	@Test
	public void dummy(){
		
	}
	
	@Test
	public void playSound() throws Exception{
		AudioPlayer player = new AudioPlayer();
		player.setPlayerDevice(playerDev);
		player.setAudioPlayerView(new CliView());
		player.setAudio(wav);
		
		player.reqPlay();
		Thread.sleep(400);
		
		player.reqPauseToggle();
		Thread.sleep(250);
		
//		assertTrue(!player.isPlaying());
		Thread.sleep(250);
		
		player.reqPauseToggle();
		Thread.sleep(1500); //-- if it is not long enough - error will occur!
		player.dispose();
	}	
}
