package pe.sbk.alwaysontop;

import pe.sbk.alwaysontop.AlwaysOnTopService.LocalBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class SettingActivity extends Activity implements OnClickListener, OnCheckedChangeListener, OnSeekBarChangeListener {
	
	Button okay, cancel;
	RadioButton lefthanded, righthanded;
	SeekBar sensitivity;
	ImageButton pointerImage;
	
	AlwaysOnTopService mService;
	boolean mBound = false;
	
	public static SharedPreferences settings;
	public static SharedPreferences.Editor settingsEdit;
	
	public void onStart() {
		super.onStart();
		Intent intent = new Intent(this, AlwaysOnTopService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}
	
	protected void onStop() {
		super.onStop();
		if(mBound) {
			unbindService(mConnection);
			mBound = false;
		}
		
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
		}
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		WindowManager.LayoutParams settingLayoutParams = new WindowManager.LayoutParams();
		settingLayoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		settingLayoutParams.dimAmount = 0.9f;
		getWindow().setAttributes(settingLayoutParams);
		setContentView(R.layout.settings);

		// 마우스 설정 변경여부	; 1 - 재설정, 0 - 유지 
		// settingsEdit.putInt("reload", 1);
		// 마우스 손잡이		; 1 - 왼손잡이, 0 - 오른손잡이
		// settingsEdit.putInt("pointer_handed", 0);
		// 마우스 감도 (속도)	; 0 ~ 100 사이의 값 ; 50이 기본 
		// settingsEdit.putInt("pointer_sensitivity", 50);
		// 마우스 포인터		; 포인터의 리소스 아이디
		// settingsEdit.putInt("pointer_img", R.drawable.pointer_basic);
		
		// 확인 및 취소 버튼 연결
		okay = (Button) findViewById(R.id.okay);
		okay.setOnClickListener(this);
		cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(this);
		
		// Pointer Hand Option
		lefthanded	= (RadioButton) findViewById(R.id.lefthanded);
		righthanded	= (RadioButton) findViewById(R.id.righthanded);
		lefthanded.setOnCheckedChangeListener(this);
		righthanded.setOnCheckedChangeListener(this);
		
		// Pointer Sensitivity
		sensitivity = (SeekBar) findViewById(R.id.mouseSensitivity);
		sensitivity.setOnSeekBarChangeListener(this);
		sensitivity.setMax(100);
		
		// Pointer Image
		pointerImage = (ImageButton) findViewById(R.id.pointer);
		pointerImage.setOnClickListener(this);
		
		// 마우스의 설정 저장을 위한 SharedPrefereces 및 Editor 설정
		settings = getSharedPreferences("settings", 0);
		settingsEdit = settings.edit();
		
		// Load and Set previous settings to Widgets
		if(settings.getInt("pointer_handed", 0) == 0) righthanded.setChecked(true);
		else lefthanded.setChecked(true);
		sensitivity.setProgress(settings.getInt("pointer_sensitivity", 50));
		pointerImage.setImageResource(settings.getInt("pointer_img", R.drawable.pointer_basic));
		pointerImage.setScaleType(ImageView.ScaleType.FIT_XY);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
		switch(v.getId())
		{
		case R.id.okay:
			settingsEdit.putInt("reload", 1);
			settingsEdit.commit();
			finish();
			mService.realtimeSetting(0);
			
			break;

		case R.id.cancel:
			settingsEdit.putInt("reload", 0);
			settingsEdit.commit();
			finish();
			mService.realtimeSetting(5);
			
			break;
			
		case R.id.pointer:
			Intent intent = new Intent(getApplicationContext(), PointerSelector.class);
			startActivityForResult(intent, 0);
			break;
		}
	}
	
	@Override
	public void onCheckedChanged(CompoundButton v, boolean isChecked) {
		// TODO Auto-generated method stub
		
		if(righthanded.isChecked()) {
			settingsEdit.putInt("pointer_handed", 0);
			settingsEdit.commit();
		} else {
			settingsEdit.putInt("pointer_handed", 1);
			settingsEdit.commit();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		settingsEdit.putInt("pointer_sensitivity", progress);
		settingsEdit.commit();
		
		return;
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
		return;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

		return;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		
		int pointerId;
		
		if(resultCode == 0) {
			if(data != null) {
				pointerId = data.getIntExtra("pointerId", 0);

				pointerImage.setImageResource(pointerId);
				settingsEdit.putInt("pointer_img", pointerId);
				settingsEdit.commit();
			}
		}
	}

}