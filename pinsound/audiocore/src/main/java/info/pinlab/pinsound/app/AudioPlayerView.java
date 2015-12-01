package info.pinlab.pinsound.app;

public interface AudioPlayerView {
	public void setPlayingState();
	public void setReadyToPlayState();
//	public void setPauseState();
	
	public void setPlayMaxLenInMs(final long ms);
	public void setPlayPosInMs(final long ms);
	public void setPlayEnabled(boolean b);
	
	public void setPlayActionListener(AudioPlayerListener l);
}
