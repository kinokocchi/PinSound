package info.pinlab.pinsound;

public interface PlayerDeviceMonitor {
	public boolean isPlaying();
	public boolean isStopped();
	public boolean isPaused();
	public long getPlayPosInMs();
	
}
