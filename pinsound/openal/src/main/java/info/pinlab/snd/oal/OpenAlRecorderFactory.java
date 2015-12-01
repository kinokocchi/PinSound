package info.pinlab.snd.oal;

import java.util.List;

import info.pinlab.pinsound.RecorderDeviceController;
import info.pinlab.pinsound.WavClip;

public class OpenAlRecorderFactory implements RecorderDeviceController{

	private final OpenAlRecorder singleton;
	
	public OpenAlRecorderFactory(){
		singleton = OpenAlRecorder.getInstance();
		singleton.setRecFormat(1, 16000, 16);
	}
	
	@Override
	public List<String> getRecorderDeviceNames() {
		return singleton.getRecorderDeviceNames();
	}

	@Override
	public boolean setRecorderDevice(int ix) {
		return singleton .setRecorderDevice(ix);
	}

	@Override
	public void setRecFormat(int chanelN, int hz, int bit) {
		singleton.setRecFormat(chanelN, hz, bit);
	}

	@Override
	public void startRec() {
		singleton.startRec();
	}

	@Override
	public boolean isRecording() {
		return singleton.isRecording();
	}

	@Override
	public void stopRec() {
		singleton.stopRec();
	}

	@Override
	public WavClip getWavClip() {
		return singleton.getWavClip();
	}

	@Override
	public void dispose() {
		singleton.dispose();
	}
}
