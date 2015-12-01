package info.pinlab.pinsound;

/**
 * @author Gabor PINTER 
 *
 */
public interface AudioClipIF {
	public byte[] getSamples();
	public long getDurInMs();
}
