package info.pinlab.pinsound.app;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.pinlab.pinsound.PlayerDeviceController;
import info.pinlab.pinsound.WavClip;

public class AudioPlayer implements AudioPlayerListener, AudioPlayerController{
	public static Logger LOG = LoggerFactory.getLogger(AudioPlayer.class); 
//			Logger.getLogger(AudioPlayer.class);
	AudioPlayerView view = null;
	WavClip wav = null;
	PlayerDeviceController playerDevController = null;

	private AudioWorkerWithViewUpdater audioWorkerWithViewerUpdater = null;
	private AudioWorker audioWorker = null;
	long wavLenInMs = -1;
	
	Runnable afterPlayHook = null;
	volatile boolean isPaused = true;
	volatile boolean isPlaying = false;


	
	private class AudioWorker implements Runnable{
		int refreshInMs = 30;
		long pos = 0; //-- cursor position in view
		
		@Override
		public void run(){
			playerDevController.setCursorPosInMs(pos);
			playerDevController.play();
			new Thread(new Runnable(){ //-- do checks from a separate thread!
				@Override
				public void run() {
					while(playerDevController.isPlaying()){
						pos = playerDevController.getPlayPosInMs();
						try {
							Thread.sleep(refreshInMs);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
					}
					isPlaying(false);
					
					pos = playerDevController.getPlayPosInMs();
					boolean isEnded = pos >= wavLenInMs;
					pos = pos >= wavLenInMs ? 0 : pos;  
					if(isEnded){
						pos = 0;
						playerDevController.setCursorPosInMs(0);
						isPaused = true;
					}
					if(afterPlayHook!= null){
						new Thread(afterPlayHook).start();
					}
				}
			}).run();
		}
	}

	
	
	
	private class AudioWorkerWithViewUpdater implements Runnable{
		int refreshInMs = 30;
		long pos = 0; //-- cursor position in view
		
		@Override
		public void run(){
			playerDevController.setCursorPosInMs(pos);
			playerDevController.play();
			view.setPlayingState();
			new Thread(new Runnable(){ //-- do checks from a separate thread!
				@Override
				public void run() {
					while(playerDevController.isPlaying()){
						pos = playerDevController.getPlayPosInMs();
						view.setPlayPosInMs(pos);
						try {
							Thread.sleep(refreshInMs);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
					}
					isPlaying(false);
					
					pos = playerDevController.getPlayPosInMs();
					boolean isEnded = pos >= wavLenInMs;
					pos = pos >= wavLenInMs ? 0 : pos;  
					if(isEnded){
						pos = 0;
						playerDevController.setCursorPosInMs(0);
						isPaused = true;
						view.setReadyToPlayState();
					}else{
						view.setReadyToPlayState();
					}
					if(afterPlayHook!= null){
						new Thread(afterPlayHook).start();
					}
				}
			}).run();
		}
	}

	
	synchronized private void isPlaying(boolean b){
		isPlaying = b;
	}
	
	private void doPlay(){
		isPlaying = true;
		isPaused = false;
		
		if(view==null){ //-- running without view
			LOG.debug("Playing without view!");
			if(audioWorker == null){
				audioWorker = new AudioWorker();
			}
			new Thread(audioWorker).start();
		}else{ //-- running with updating the view
			LOG.debug("Playing with view!");
			if(audioWorkerWithViewerUpdater == null){
				audioWorkerWithViewerUpdater = new AudioWorkerWithViewUpdater();
			}
			new Thread(audioWorkerWithViewerUpdater).start();
		}
	}
	
	
	
	@Override
	synchronized public void reqStop(){
		playerDevController.stop();
		isPlaying = false;
	}

	@Override
	synchronized public void reqPlay(){
		if(playerDevController==null){
			LOG.debug("No player controller has been set!");
			return ;
		}
		if(playerDevController.isPlaying()){
			LOG.debug("Already plaing");
			return;
		}
		doPlay();
	}
	

	@Override
	synchronized public void reqPauseToggle(){
//		System.out.println(" TOGGLE !!!!!!!!!!!!!! " + isPaused);
		if(isPaused){
			isPaused = false;
			doPlay();
		}else{
			isPaused = true;
			isPlaying = false;
			playerDevController.pause();
		}
	}
	
	
	
	@Override
	synchronized public void reqPosInMs(long ms) {
		playerDevController.setCursorPosInMs(ms);
	}

	
	@Override
	synchronized public void setAudioPlayerView(AudioPlayerView view) {
		this.view = view;
	}

	@Override
	public AudioPlayerView getAudioPlayerView(){
		return this.view;
	}
	
	@Override
	synchronized public void setAudio(WavClip wav) throws IllegalStateException{
		if(wav==null){
			LOG.warn("Audio set to NULL! Dev " + playerDevController);
		}
		LOG.info("Audio set to  " + wav.getDurInMs() +"ms audio " + playerDevController);
		
		this.wav = wav;
		if (playerDevController!=null){
			playerDevController.initWav(wav);
		}
		wavLenInMs = wav.getDurInMs();
		if(view!=null){
			view.setPlayMaxLenInMs(wavLenInMs);
			view.setReadyToPlayState();
		}
	}

	@Override
	synchronized public void setPlayerDevice(PlayerDeviceController player) {
		this.playerDevController = player;
	}

	@Override
	synchronized public PlayerDeviceController getPlayerDevice() {
		return this.playerDevController ;
	}


	

	@Override
	synchronized public void dispose(){
		if(playerDevController!=null)
			playerDevController.stop();
		if(audioWorker!= null)
			audioWorker = null;
		if(audioWorkerWithViewerUpdater !=null)
			audioWorkerWithViewerUpdater = null;
		if(playerDevController!=null)
			playerDevController.dispose();
	}



	@Override
	public void setAfterPlayHook(Runnable afterPlayHook) {
		this.afterPlayHook = afterPlayHook;
	}



	@Override
	synchronized public boolean isPlaying(){
		return isPlaying;
//		if(playerDevController==null)
//			return false;
//		return playerDevController.isPlaying();
	}
}
