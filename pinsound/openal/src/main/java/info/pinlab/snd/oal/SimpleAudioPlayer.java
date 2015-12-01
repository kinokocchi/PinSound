package info.pinlab.snd.oal;




import info.pinlab.pinsound.PlayerDeviceController;
import info.pinlab.pinsound.WavClip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleAudioPlayer{
	public static Logger LOG = LoggerFactory.getLogger(SimpleAudioPlayer.class);
	
//	private final WavClip wav;
	private final PlayerDeviceController player;
	volatile long pos = 0;
	private Runnable onFinishAction = null;
//	private Runnable onPauseAction = null;
//	private Thread onFinishActionThread = null;
//	private boolean localPlayer = false;
	
	
	

	
	public SimpleAudioPlayer(WavClip wav){
		this(new OpenAlPlayer(), wav);
//		localPlayer = true;
	}
	public SimpleAudioPlayer(PlayerDeviceController player, WavClip wav){
//		this.wav = wav;
		this.player = player;
		this.player.initWav(wav);
	}
	
	public void runOnFinish(Runnable run){
		onFinishAction = run;
	}
	

	private int playResolution = 100;
	private volatile boolean isInterrupted = false;
	
	synchronized private boolean isInterrupted(){		return isInterrupted;	}
	synchronized private void isInterrupted(boolean b){	isInterrupted = b;		}

	public long pause(){
		isInterrupted(true);
		pos = player.pause();
		return pos;
	}
	
	public void play(){
		player.setCursorPosInMs(pos);
		player.play();
		isInterrupted(false);
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(player.isPlaying()){
					try {
						Thread.sleep(playResolution);
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					}
				}
				if(!isInterrupted())
					new Thread(onFinishAction).start();
			}
		}).start();
	}
	
	
	public void stop(){
		if(player.isPlaying()){
			isInterrupted(true);
			player.stop();
		}//-- if not playing just rewind!
		pos = 0;
	}
	
	public void dispose(){
		player.stop();
		player.dispose();
	}
	
	
	public static void main(String[] args) throws Exception {
//		BasicConfigurator.configure();
		SimpleAudioPlayer player = new SimpleAudioPlayer(new WavClip("data/snd/sample.wav"));
		player.runOnFinish(new Runnable() {
			@Override
			public void run() {
				System.out.println("FINISHED!");
			}
		});
		
		player.play();
		for (int i = 1 ; i < 10 ; i++){
//			System.out.println(i);
			Thread.sleep(95);
		}
		player.pause();
		
		Thread.sleep(2000);
		player.play();
		Thread.sleep(2000);
		
//		System.out.println("PLAYING");
		player.dispose();
		
		
	}
}
