package com.chinamworld.soundtools.process;

import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.AsyncTask;
import android.util.Log;

import com.chinamworld.soundtools.activity.ReceiverActivity;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class Receiver extends AsyncTask<Integer, Double, Long> {

	private static Double[] energys = new Double[Common.FREQUENCY_TO_WATCH];

	private AudioRecord audioRecord;
	private int minBufferSize = AudioRecord.getMinBufferSize(
			Common.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
			AudioFormat.ENCODING_PCM_16BIT);
	private ReceiverActivity receiverActivity;
	private final byte[] marker = new byte[] { 2, 2, 0, 0 };
	private byte matchIndex = 0;
	private byte matchIndex2 = 0;
	private boolean isDecording = false;
	private byte[] result = new byte[5];
	private byte[] resultForLong = new byte[8];
	private byte byteIndex = 0;
	private long preGet;

	public Receiver(ReceiverActivity receiverActivity) {
		this.receiverActivity = receiverActivity;

	}

	@Override
	protected Long doInBackground(Integer... integers) {
		// construct audioRecord
		this.audioRecord = new AudioRecord(AudioSource.MIC, Common.SAMPLE_RATE,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
				this.minBufferSize);
		// sample buffer
		short[] buffer = new short[Common.FFT_SIZE];
		short[] buffer2 = new short[Common.FFT_SIZE];

		// FFT space
		double[] source = new double[Common.FFT_SIZE * 2];

		int readByteCount;
		DoubleFFT_1D fft = new DoubleFFT_1D(Common.FFT_SIZE);
		// init bytes Result
		for (int i = 0; i < result.length; i++)
			result[i] = 0;
		this.audioRecord.startRecording();
		// whether use offset buffer
		boolean isOffset = false;
		int indexMax2 = 0;
		int indexMax = 0;
		double energyMax = 0;
		
		while (!isCancelled()) {
			readByteCount = this.audioRecord.read(buffer, 0, Common.FFT_SIZE);
			if (readByteCount == Common.FFT_SIZE) {
				
				
					for (int i = 0; i < buffer.length; i++) {
						source[2 * i] = buffer[i];
						source[2 * i + 1] = 0;
					}

					fft.complexForward(source);
					
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
					if (energyMax < 10000) continue;
					if (indexMax == marker[matchIndex]) {
						matchIndex++;
					} else {
						matchIndex = 0;
					}
					
					if (matchIndex == marker.length) {
						isDecording = true;
						matchIndex = 0;
						continue;
					}
				}
				if (isDecording) {
					if (energyMax < 10000) {
						isDecording = false;
						continue;
					}
					result[byteIndex] = (byte) ((indexMax << 2 * matchIndex) | result[byteIndex]);
					matchIndex++;
					if (matchIndex == 4) {
						byteIndex++;
						matchIndex = 0;
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
						long resultNumber = bb.getLong();
						
//						if (resultNumber == preGet) {
//							this.audioRecord.stop();
//							return resultNumber;
//						} else
//							preGet = resultNumber;
						this.audioRecord.stop();
						return resultNumber;
						
						
					}
				}
				this.publishProgress(energys);
			}
		}
		this.audioRecord.stop();
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
