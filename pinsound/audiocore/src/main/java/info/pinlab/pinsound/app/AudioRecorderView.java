package info.pinlab.pinsound.app;

public interface AudioRecorderView {
	public void setRecordingState();
	public void setReadyToRecState();
	public void setBusyState();
	
	public void setRecEnabled (boolean b);
	
	public void setRecPosInMs(final long ms);
	public void setRecMaxPosInMs(final long ms);
	
	public void setRecActionListener(AudioRecorderListener l);
}
