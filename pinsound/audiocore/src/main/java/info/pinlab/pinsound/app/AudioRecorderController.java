package info.pinlab.pinsound.app;

import info.pinlab.pinsound.RecorderDeviceController;
import info.pinlab.pinsound.WavClip;

public interface AudioRecorderController{
	public WavClip getWavClip();

	public void dispose();
	public void setMaxRecLenInMs(long ms);
	public void setAudioRecorderView(AudioRecorderView view);
	public void setRecorderDevice(RecorderDeviceController recorder);
	public void setAfterRecHook(Runnable afterRecHook);
	public boolean isRecording();
	
	
}
