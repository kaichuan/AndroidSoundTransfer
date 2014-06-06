package com.chinamworld.soundtools.process;

import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.AsyncTask;

import com.chinamworld.soundtools.activity.ReceiverActivity;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class Receiver extends AsyncTask<Integer, Double, Long> {

	private ReceiverActivity receiverActivity;

	public Receiver(ReceiverActivity receiverActivity) {
		this.receiverActivity = receiverActivity;

	}

	@Override
	protected Long doInBackground(Integer... integers) {
		// store each frequencies's energy
		Double[] energys = new Double[Common.FREQUENCY_TO_WATCH];
		// calculate minimal buffer size by Common parameters
		int minBufferSize = AudioRecord.getMinBufferSize(Common.SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		// marker that separate each transmitting data
		final byte[] marker = new byte[] { 2, 2, 0, 0 };
		// codeIndex is the code that represent by each 10ms sound wave
		byte codeIndex = 0;
		// byteIndex is the index of the transmitting data byte
		byte byteIndex = 0;
		// if isDecording mean the queue is start decoding the data
		boolean isDecording = false;
		// indicate if the queue is synchronized
//		boolean isSync = false;

		byte[] result = new byte[5];
		byte[] resultForLong = new byte[8];
		
		long resultNumber, preGet = 0;

		

		// construct audioRecord
		AudioRecord audioRecord = new AudioRecord(AudioSource.MIC,
				Common.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
		// sample buffer
		short[] buffer = new short[Common.FFT_SIZE];
		// offset sample buffer
		short[] offsetBuffer = new short[Common.FFT_SIZE];

		// FFT space
		double[] source = new double[Common.FFT_SIZE * 2];

		int readByteCount;
		DoubleFFT_1D fft = new DoubleFFT_1D(Common.FFT_SIZE);
		// initial bytes Result
		for (int i = 0; i < result.length; i++)
			result[i] = 0;
		audioRecord.startRecording();
		// whether use offset buffer

		int indexMax = 0;
		double energyMax = 0;
		
		int failCount = 0;
		int offSet = 0;
		
//		int deltaMax = 0;
//		int syncOffset = 0;
//		final int syncDelta = 41;

		while (!isCancelled()) {
			
			// start sync with the queue
//			if (!isSync) {
//				short[] syncBuffer = new short[Common.FFT_SIZE * 2];
//				readByteCount = audioRecord.read(syncBuffer, 0, syncBuffer.length);
//				energyMax = 0;
//				syncOffset = 0;
//				startTime = System.currentTimeMillis();
//				for (int i = 0; i < 10; i++){
//					syncOffset = syncDelta * i;
//					buffer = Arrays.copyOfRange(syncBuffer, syncOffset, syncOffset+Common.FFT_SIZE);
//					for (int j = 0; j < buffer.length; j++) {
//						source[2 * j] = buffer[j];
//						source[2 * j + 1] = 0;
//					}
//					fft.complexForward(source);
//					for (int k = 0; k < Common.FREQUENCY_TO_WATCH; k++) {
//						energys[k] = Math
//								.sqrt(Math.pow(
//										source[Common.targetBins[k] * 2], 2)
//										+ Math.pow(
//												source[Common.targetBins[k] * 2 + 1],
//												2));
//						if (energys[k] > energyMax) {
//							energyMax = energys[k];
//							indexMax = k;
//							deltaMax = i;
//						}
//						if (energyMax < 10000){
////							Log.i("ttt", "energy:" + energyMax);
//							continue line;
//						}
//					}
//				}
//				endTime = System.currentTimeMillis();
//				Log.i("ttt", "Stime:" + (endTime - startTime) + " offset:" + deltaMax);
//				
//				
//				
//				
//			} else {
				readByteCount = audioRecord.read(buffer, 0, Common.FFT_SIZE);
				
				if (readByteCount == Common.FFT_SIZE) {
					for (int i = 0; i< buffer.length - offSet; i++) {
						source[2 * i] = offsetBuffer[i];
						source[2 * i + 1] = 0;
					}
					for (int i = 0; i < offSet; i++) {
						source[2 * (i + buffer.length - offSet)] = buffer[i];
						source[2 * (i + buffer.length - offSet) + 1] = 0;
					}
					for (int i = 0; i < buffer.length - offSet; i++) {
						offsetBuffer[i] = buffer[i+offSet];
					}
					
					
//					startTime = System.currentTimeMillis();
					fft.complexForward(source);
//					endTime = System.currentTimeMillis();

//					if (!isDecording) {
//						Log.i("ttt", "" + (endTime - startTime));
//					}

					energyMax = 0;
					for (int i = 0; i < Common.FREQUENCY_TO_WATCH; i++) {
						energys[i] = Math
								.sqrt(Math.pow(
										source[Common.targetBins[i] * 2], 2)
										+ Math.pow(
												source[Common.targetBins[i] * 2 + 1],
												2));
						if (energys[i] > energyMax) {
							energyMax = energys[i];
							indexMax = i;
						}
					}
					if (!isDecording) {
						if (energyMax < 8000)
							continue;
						if (indexMax == marker[codeIndex]) {
							codeIndex++;
						} else {
							codeIndex = 0;
							failCount ++;
						}
						if (failCount == 44){
							failCount = 0;
							offSet += Common.FFT_SIZE/11;
							offSet = offSet % Common.FFT_SIZE;
							continue;
						}
						if (codeIndex == marker.length) {
							isDecording = true;
							codeIndex = 0;
							continue;
						}
					}
					if (isDecording) {
						if (energyMax < 8000) {
							isDecording = false;
							continue;
						}
						result[byteIndex] = (byte) ((indexMax << 2 * codeIndex) | result[byteIndex]);
						codeIndex++;
						if (codeIndex == 4) {
							byteIndex++;
							codeIndex = 0;
						}
						if (byteIndex == result.length) {
							isDecording = false;
							byteIndex = 0;
							for (int s = 0; s < 3; s++)
								resultForLong[s] = 0;
							for (int s = 3; s < 8; s++) {
								resultForLong[s] = result[s - 3];
							}
							for (int i = 0; i < result.length; i++)
								result[i] = 0;
							ByteBuffer bb = ByteBuffer.wrap(resultForLong);
							resultNumber = bb.getLong();

							 if (resultNumber == preGet) {
							 audioRecord.stop();
							 return resultNumber;
							 } else
							 preGet = resultNumber;
//							audioRecord.stop();
//							return resultNumber;
						}
					}
					this.publishProgress(energys);
				}
//			}
		}
		audioRecord.stop();
		return null;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected void onPostExecute(Long result) {
		super.onPostExecute(result);
		this.receiverActivity.finished(result);
	}

	@Override
	protected void onProgressUpdate(Double... values) {
		super.onProgressUpdate(values);
		this.receiverActivity.setReceiverFreqText(values);

	}

	@Override
	protected void onCancelled(Long aVoid) {

	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
	}

}
