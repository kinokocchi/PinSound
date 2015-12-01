package info.pinlab.pinsound;

import java.util.List;

public interface RecorderDeviceController{
	
	
	public List<String> getRecorderDeviceNames();
	public boolean setRecorderDevice(int ix) ;

	public void setRecFormat(int chanelN, int hz, int bit);
	
	public void startRec();
	public boolean isRecording();
	public void stopRec();

	public WavClip getWavClip();
	
	public void dispose();
	
}
