package info.pinlab.pinsound.app;

public interface AudioPlayerListener {
	public void reqStop();
	public void reqPlay();
	public void reqPauseToggle();
	public void reqPosInMs(long ms);
}
