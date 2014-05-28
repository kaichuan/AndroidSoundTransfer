package com.chinamworld.soundtools.activity;

import java.nio.ByteBuffer;
import java.util.Arrays;

import android.app.Activity;
import android.app.Fragment;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.chinamworld.soundtools.R;
import com.chinamworld.soundtools.process.Common;
import com.chinamworld.soundtools.process.Generator;

public class GeneratorActivity extends Activity {
	private static TableLayout spectrumTable;
	private static Switch[] freqSwitches = new Switch[Common.FREQUENCY_TO_WATCH];
	private Generator generator;
	private MediaPlayer mediaPlayer;
	public void toggleSwitch(View view) {
		if (((Switch) view).isChecked()) {
			if (generator == null || generator.isCancelled()){
				EditText editText = (EditText)this.findViewById(R.id.phoneNumber);
				String numberStr = editText.getText().toString();
				if (numberStr.length() > 0){
				Long phoneNumber = Long.parseLong(numberStr);
				byte[] arrayToSend = Arrays.copyOfRange(ByteBuffer.allocate(8).putLong(phoneNumber).array(), 3, 8);
				generator = new Generator(arrayToSend);
				this.generator.execute();	
				
//				mediaPlayer = MediaPlayer.create(this, R.raw.ring1);
//				mediaPlayer.setLooping(true);
//				mediaPlayer.start();
				
				}else {
					((Switch) view).setChecked(false);
				}
			}
			
		} else {
			this.generator.cancel(true);
//			mediaPlayer.release();
//			mediaPlayer = null;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_generator);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.generator, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_generator,
					container, false);
			spectrumTable = (TableLayout) rootView.findViewById(R.id.spectrumTable);
			TextView freqText;
			TableRow row;
			for (int i = 0; i < Common.FREQUENCY_TO_WATCH; i++) {
		        row = new TableRow(getActivity());
		        freqText = new TextView(getActivity());
		        freqSwitches[i] = new Switch(getActivity());
		        freqText.setText(String.valueOf(Common.frequencys[i]));
		        row.addView(freqText);
		        row.addView(freqSwitches[i]);
		        spectrumTable.addView(row,i+1);
		    }
			return rootView;
		}
	}

}
