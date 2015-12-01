package info.pinlab.pinsound.app;

import info.pinlab.pinsound.PlayerDeviceController;
import info.pinlab.pinsound.WavClip;

public interface AudioPlayerController {
	
	public void setAudioPlayerView(AudioPlayerView view);
	public AudioPlayerView  getAudioPlayerView();

	public void setPlayerDevice(PlayerDeviceController player);
	public PlayerDeviceController getPlayerDevice();

	public void setAudio(WavClip wav) throws IllegalStateException;

	public void setAfterPlayHook(Runnable afterPlayHook);
	public boolean isPlaying();
	
	public void dispose();
}
