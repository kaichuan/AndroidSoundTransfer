package com.chinamworld.soundtools.process;



public class Common {
	public static final int FREQUENCY_TO_WATCH = 4;
	public static final int FFT_SIZE = 441;
	public static final int SAMPLE_RATE = 44100;
	public static final float minFrequency = 17500;
	public static final float maxFrequency = 18250;
	
	public static float[] frequencys = new float[FREQUENCY_TO_WATCH];
	public static int[] targetBins = new int[FREQUENCY_TO_WATCH];
	
	static {
		float freqSize = Common.SAMPLE_RATE / FFT_SIZE;
		float indexFreq = 0;
		int indexBin = 0;
		while (indexFreq < minFrequency){
			indexFreq += freqSize;
			indexBin++;
		}
		int i=1;
		while ((indexFreq + (FREQUENCY_TO_WATCH-1) * freqSize * i)<maxFrequency)
			i++;
		i--;
		for (int j=0; j< FREQUENCY_TO_WATCH; j++ ){
			indexFreq += i * freqSize;
			frequencys[j] = indexFreq;
			indexBin += i;
			targetBins[j] = indexBin;
//			Log.i("xxx", ""+indexBin);
		}
	}
}
