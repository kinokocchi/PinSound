package info.pinlab.pinsound.app;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import info.pinlab.pinsound.RecorderDeviceController;
import info.pinlab.pinsound.WavClip;

public class AudioRecorder implements AudioRecorderListener, AudioRecorderController{
	public static Logger LOG = LoggerFactory.getLogger(AudioRecorder.class);
	
	AudioRecorderView recView ;
	RecorderDeviceController recorder;
	RecViewUpdater viewUpdater;

	Runnable afterRecHook = null;
	
	long maxRecLenInMs = 2*1000;
	WavClip recordedWav ;
	
	Thread viewUpdaterThread = null;

	
	private class RecViewUpdater implements Runnable{
		int refreshInMs = 30;
		
		@Override
		public void run(){
			if(recorder==null){
				LOG.error("Recorder device controller is not set!");
				return;
			}
			final long t0 = System.currentTimeMillis();
//			System.out.println("Start rec!");
			recorder.startRec();
//			new Thread(new Runnable() {
//				@Override
//				public void run() {
//					recorder.startRec();
//				}
//			}).start();
			new Thread(new Runnable(){ //-- do checks from a separate thread!
				@Override
				public void run(){
					while(recorder.isRecording()){
						long t = System.currentTimeMillis() - t0;
						recView.setRecPosInMs(t);
						try {
							Thread.sleep(refreshInMs);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						}
						t = System.currentTimeMillis() - t0;
						if(t >= maxRecLenInMs){ //-- stop over time
							recorder.stopRec();
						}
					}
					//-- after recording has finished!
					recView.setBusyState();
					//-- get the recorded wav
//					try {
//						Thread.sleep(1000);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					recordedWav = recorder.getWavClip();
					//-- ready to record again!
					recView.setReadyToRecState();					
					//-- do after recording hook
					afterRecHook.run();
//					if(afterRecHook!=null){
//						new Thread(afterRecHook).start();
//					}
				}
			}).start();
		}
	}
	
	@Override
	public WavClip getWavClip(){
		if (recorder==null){
			LOG.warn("Recorder device controller is not set!");
			return null;
		}
		this.reqRecStop();
		//-- this automatically harvest WavClip int recordedWav
		if(viewUpdaterThread!=null){
//			try {
//				viewUpdaterThread.join(); //-- wait for recorder view 
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}
		return recordedWav;
	}
	
	
	public void doRec(){
		if(viewUpdater==null){
			viewUpdater = new RecViewUpdater();
		}
		recView.setRecMaxPosInMs(maxRecLenInMs);
		recView.setRecordingState();
		viewUpdaterThread = new Thread(viewUpdater);
		viewUpdaterThread.start();
	}

	@Override
	public void setAudioRecorderView(AudioRecorderView view) {
		this.recView = view;		
	}

	@Override
	public void setRecorderDevice(RecorderDeviceController recorder) {
		this.recorder = recorder;
	}

	@Override
	public void reqRecStart() {
//		System.out.println("REQ Rec start!");
		if(recorder==null){
			LOG.error("Recorder device controller is not set!");
			return;
		}
		if(recorder.isRecording()){
			LOG.warn("Recorder is already recording!");
			return;
		}
		doRec();
	}

	@Override
	public void reqRecStop() {
//		System.out.println("REQ Rec stop!");
		if(recorder==null || !recorder.isRecording()){
			return; //-- not recording
		}
		recorder.stopRec();
	}
	
	
	@Override
	public void dispose() {
		if(recorder!=null)
			recorder.dispose();
	}

	@Override
	public void setAfterRecHook(Runnable afterRecHook) {
		this.afterRecHook = afterRecHook;
	}

	@Override
	public void setMaxRecLenInMs(long ms) {
		maxRecLenInMs = ms;
	}

	@Override
	public boolean isRecording() {
		return recorder.isRecording();
	}

}
