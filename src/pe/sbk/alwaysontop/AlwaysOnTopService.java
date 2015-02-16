package pe.sbk.alwaysontop;

import net.londatiga.android.*;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.view.GestureDetector.SimpleOnGestureListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlwaysOnTopService extends Service{
	// WindowManager
	private WindowManager mWindowManager;
	
	// AlwaysOnTop Views
	private ImageView mTouchpadView; 						// Touchpad
	private ImageView mMouseView; 							// Mouse Pointer
	private ImageView mHiddenView; 							// Hidden Button
	
	// Parameters of Views
	private WindowManager.LayoutParams mTouchpadParams;		// Layout Parameter of Touchpad
	private WindowManager.LayoutParams mMouseParams; 		// Layout Parameter of Mouse Pointer
	private WindowManager.LayoutParams mHiddenParams; 		// Layout Parameter of Hidden Button
	
	// Variables_About View
	private int SCREEN_SIZE_X, SCREEN_SIZE_Y;				// Screen Size
	private int TOUCHPAD_SIZE_X, TOUCHPAD_SIZE_Y;			// Touchpad Size
	private int HIDDEN_BTN_SIZE_X , HIDDEN_BTN_SIZE_Y;		// Hidden Button Size
	private float RATIO_SCREEN_BY_PAD_X , RATIO_SCREEN_BY_PAD_Y;	// Ratio Screen Size By Touchpad Size
	private static final int MOUSE_POINTER_SPEED = 50;		// Standard Speed for Mouse Pointer
	private int USER_SPECIAL_MODE;							// Left-handed or Right-handed
															// right-handed && HORIZONTAL	= 1;
															// right-handed && VERTICAL		= 2;
															// left-handed && HORIZONTAL	= 3;
															// left-handed && VERTICAL		= 4;
	
	// Variables_About Programmatically Click Event
	private Process process; 								// Process : Execute Click Event
	
	// Variables_About Quick Action Menu
	QuickAction quickAction;								// Quick Action Object
	private static final int ID_STATUS     	= 1;			// Apply Status Bar to ID
	private static final int ID_SETTING     = 2;			// Apply Setting Bar to ID
	private static final int ID_HIDE		= 3;			// Apply Hide Button to ID
	private static final int ID_CLOSE     	= 4;			// Apply Close Button to ID
	private static final int ID_SCREEN		= 5;
	
	// Variables_About Click Sound
	private SoundPool sound_pool;							// Sound Pool Object
	private int sound_beep;									// Sound Files
	Runtime runtime;										// Runtime Object
	static boolean IsProcessKilled = false;					// Is Process Killed?				
	
	// Variables_About Click Event
	private GestureDetector mGesDetector;					// Gesture
	
	// Settings Variables
	public static SharedPreferences settings;
	int pointer_handed;										// Values RIGHT_HANDED(0), LEFT_HANDED(1) can be used for this
	int pointer_sensitivity;
	int pointer_img;
	
	// Realtime Settings Switcher
	// used for "int settingToBeChanged" : realtimeSetting(int settingToBeChnaged)  
	private final int RELOAD				= 0;
	private final int TOUCHPAD				= 1;
	private final int HIDDEN_BUTTON			= 2;
	private final int POINTER_SENSITIVITY	= 3;
	private final int POINTER_IMAGE			= 4;

	private final int RIGHT_HANDED	= 0;
	private final int LEFT_HANDED	= 1;
	
	// onCreate
	@Override
	public void onCreate() {
		super.onCreate();
		
		// Load Previous Saved Settings
		settings = getSharedPreferences("settings", 0);
		
		// Create Objects
		mGesDetector = new GestureDetector(this, mGestureListener);
		
		mTouchpadView = new ImageView(this);
		mTouchpadView.setImageResource(R.drawable.touchpad3);
		mTouchpadView.setSoundEffectsEnabled(false);
		mTouchpadView.setOnTouchListener(mViewTouchListener);
		
		mMouseView = new ImageView(this);
		mMouseView.setImageResource(pointer_img);
		
		mHiddenView = new ImageView(this);
		mHiddenView.setImageResource(R.drawable.t_p);
		mHiddenView.setOnTouchListener(mHiddenViewTouchListener);

		// Setting Touchpad Parameters
		mTouchpadParams = new WindowManager.LayoutParams(
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.TYPE_PHONE,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
			PixelFormat.TRANSLUCENT);
		mTouchpadParams.windowAnimations = android.R.style.Animation_Translucent;
		mTouchpadParams.gravity = Gravity.LEFT | Gravity.TOP;
		
		// Setting Mousepointer Parameters
		mMouseParams = new WindowManager.LayoutParams(
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.WRAP_CONTENT,
			WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
			WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
			PixelFormat.TRANSLUCENT);
		mTouchpadParams.windowAnimations = android.R.style.Animation_Translucent;
		mMouseParams.gravity = Gravity.LEFT | Gravity.TOP;
		
		// Setting Hidden Button Parameters
		mHiddenParams = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				PixelFormat.TRANSLUCENT);
			mHiddenParams.gravity = Gravity.LEFT | Gravity.TOP;
			
		// Load Window Manager
		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		
		// Set user hand option & display direction : right-handed && VERTICAL MODE
		USER_SPECIAL_MODE = 2; 
		
		getScreenSize(); 							// Get Screen Size
		getHiddenBtnSize();							// Get Hidden Button Size
		getTouchpadSize();							// Get Touchpad Size
		setInitialMouseSensitivity();				// Set Initial Mouse Sensitivity
		setInitialMousepointerPosition();			// Set Mousepointer Position
		initSound();								// Init Sound

		// Hide Hidden Button
		mHiddenView.setVisibility(View.GONE);

		// Add Views to WindowManager
		mWindowManager.addView(mTouchpadView, mTouchpadParams);
		mWindowManager.addView(mMouseView, mMouseParams);
		mWindowManager.addView(mHiddenView, mHiddenParams);
		
		LayoutParams params = (LayoutParams) mTouchpadView.getLayoutParams();
		params.width = TOUCHPAD_SIZE_X;
		params.height = TOUCHPAD_SIZE_Y;
		mTouchpadView.setLayoutParams(params);
		
		// reload
		realtimeSetting(RELOAD);
		initSound();								// Initialize clicking sounds source
		runtime = Runtime.getRuntime();

	}
	
	/**
	 * Initialize Mouse Sensitivity
	 */
	private void setInitialMouseSensitivity() {
		// 기기 스크린 사이즈에 따른 마우스 감도를 설정한다.
		
		RATIO_SCREEN_BY_PAD_X = (float)(SCREEN_SIZE_X / TOUCHPAD_SIZE_X);
		RATIO_SCREEN_BY_PAD_Y = (float)(SCREEN_SIZE_Y / TOUCHPAD_SIZE_Y);
	}
	
	/**
	 * Get Touchpad Size
	 */
	private void getTouchpadSize() {
		if(SCREEN_SIZE_Y > SCREEN_SIZE_X) {
			TOUCHPAD_SIZE_X = (int)(SCREEN_SIZE_Y * 0.25);
			TOUCHPAD_SIZE_Y = (int)(SCREEN_SIZE_Y * 0.25);
		}
		else {
			TOUCHPAD_SIZE_X = (int)(SCREEN_SIZE_X * 0.25);
			TOUCHPAD_SIZE_Y = (int)(SCREEN_SIZE_X * 0.25);
		}
	}

	/**
	 * Get Screen Size
	 */
	private void getScreenSize() {
		Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		SCREEN_SIZE_X = display.getWidth();
		SCREEN_SIZE_Y = display.getHeight();
	}
	
	/**
	 * Get Hidden Button Size
	 */
	private void getHiddenBtnSize() {
		HIDDEN_BTN_SIZE_X = mHiddenView.getWidth();
		HIDDEN_BTN_SIZE_Y = mHiddenView.getHeight();
	}
	
	/**
	 * Initial Mousepointer Position
	 */
	private void setInitialMousepointerPosition() {
		mMouseParams.x = SCREEN_SIZE_X / 2;
		mMouseParams.y = SCREEN_SIZE_Y / 2;
	}
	
	// Initialize Sound
	private void initSound() {
		sound_pool = new SoundPool(20, AudioManager.STREAM_MUSIC, 0);
		sound_beep = sound_pool.load(this, R.raw.beep, 1);
	}
	
	// Play Sound
	public void playSound() {
		sound_pool.play(sound_beep, 1, 1, 1, 0, 1 );
	}
	
	/**
	 * Apply Setting Variable
	 */
	public void realtimeSetting(int settingToBeChnaged) {

		// Load values of preferences
		pointer_handed		= settings.getInt("pointer_handed", RIGHT_HANDED);
		pointer_sensitivity	= settings.getInt("pointer_sensitivity", 50);
		pointer_img			= settings.getInt("pointer_img", R.drawable.pointer_basic);
		
		switch(settingToBeChnaged) {
		case RELOAD:
			// Set touchpad position
			// Set hidden button position
			// Set pointer sensitivity
			// Set pointer image0
			setTouchpadPosition(pointer_handed);
			mWindowManager.updateViewLayout(mTouchpadView, mTouchpadParams);
			setHiddenButtonPosition();
			mMouseView.setImageResource(pointer_img);
			mWindowManager.updateViewLayout(mMouseView, mMouseParams);
			break;
		
		case TOUCHPAD:					// Set touchpad position
			setTouchpadPosition(pointer_handed);
			mWindowManager.updateViewLayout(mTouchpadView, mTouchpadParams);
			break;
		
		case HIDDEN_BUTTON:				// Set hidden button position
			setHiddenButtonPosition();
			break;
		
		case POINTER_SENSITIVITY:		// Set pointer sensitivity
			break;
		
		case POINTER_IMAGE:				// Set pointer image
			mMouseView.setImageResource(pointer_img);
			mWindowManager.updateViewLayout(mMouseView, mMouseParams);
			break;
			
		default:
			break;
		}
		
		mTouchpadView.setVisibility(View.VISIBLE);
		mMouseView.setVisibility(View.VISIBLE);
	}
	
	// Touch Event - Use To Move Mouse Pointer
	private OnTouchListener mViewTouchListener = new OnTouchListener() {
		@Override public boolean onTouch(View v, MotionEvent event) {
			if(mGesDetector.onTouchEvent(event))
				return true;
			else {
				if(event.getAction() == MotionEvent.ACTION_DOWN) {
					touchpadIn();
				}
				else if(event.getAction() == MotionEvent.ACTION_UP) {
					touchpadOut(0);
				}
			}
			return true;
		}
	};
	
	/**
	 * Show Quick Action Menu
	 */
	private void showQuickActionMenu() {
		
		ActionItem screenitem	= new ActionItem(ID_SCREEN, "스크린샷");
		ActionItem statusItem	= new ActionItem(ID_STATUS, "상태바");
		ActionItem settingItem	= new ActionItem(ID_SETTING, "설정창");
		ActionItem hideItem		= new ActionItem(ID_HIDE, "감추기");
		ActionItem closeItem	= new ActionItem(ID_CLOSE,"종료");
		
		// Quick Action Menu Object
		quickAction = new QuickAction(this, QuickAction.VERTICAL);
		
		// Add Items to Quick Action Menu
		quickAction.addActionItem(screenitem);
		quickAction.addActionItem(statusItem);
		quickAction.addActionItem(settingItem);
		quickAction.addActionItem(hideItem);
		quickAction.addActionItem(closeItem);
		
		// Click Listener of Quick Action Menu
		quickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
			
			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				// TODO Auto-generated method stub
				ActionItem actionItem = quickAction.getActionItem(pos);
				
				// Click Status Bar
				if(actionItem.getTitle() == "상태바") {
					try {
						Object sbservice = getApplication().getSystemService("statusbar");
						Class<?> statusbarManager;
						statusbarManager = Class.forName("android.app.StatusBarManager");
						Method showsb;
						if(Build.VERSION.SDK_INT >= 17) {
							showsb = statusbarManager.getMethod("expandNotificationsPanel");
						}
						else {
							showsb = statusbarManager.getMethod("expand");
						}
						showsb.invoke(sbservice);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
				
				// Click Setting Window
				else if(actionItem.getTitle() == "설정창") {
					mTouchpadView.setVisibility(View.GONE);
					mMouseView.setVisibility(View.GONE);
					// Start New Activity for Change Settings
					Intent intent = new Intent(AlwaysOnTopService.this, SettingActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					
				}
				
				// Click Hide Touchpad
				else if(actionItem.getTitle() == "감추기") {
					// Hide Touchpad and Mousepointer, Show Hidden Button
					mWindowManager.updateViewLayout(mHiddenView, mHiddenParams);
					mHiddenView.setVisibility(View.VISIBLE);
					mTouchpadView.setVisibility(View.GONE);
					mMouseView.setVisibility(View.GONE);
				}
				
				// Click Exit
				else if(actionItem.getTitle() == "종료") {
					stopSelf();
				}
				
				else if(actionItem.getTitle() == "스크린샷") {
					try { 						
						//  Programmatical Click Event
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.KOREA);
						Date currentTime = new Date(System.currentTimeMillis());
						String dTime = formatter.format(currentTime);
						
						if(IsProcessKilled == false) {
						
						process = runtime.exec("su -c /system/bin/screencap -p " + "/sdcard/am_screenshot/" + dTime +".png");
						IsProcessKilled = true;
						}
						else {
							process.destroy();
							process = runtime.exec("su -c /system/bin/screencap -p " + "/sdcard/am_screenshot/" + dTime +".png");
						}

						/*try {
							Thread.sleep(2000);
							process.destroy();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}*/
						
						playSound();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	// Touch Event - Open Or Close Hidden Button
	private OnTouchListener mHiddenViewTouchListener = new OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					v.setVisibility(View.GONE);
					mTouchpadView.setVisibility(View.VISIBLE);
					mMouseView.setVisibility(View.VISIBLE);
			}
			return true;
		}
	};
	
	/**
	 * Set Hidden Button Position
	 */
	private void setHiddenButtonPosition() {
		if(USER_SPECIAL_MODE == 1) {					// right-handed && HORIZONTAL
			mHiddenParams.x = (int)(SCREEN_SIZE_X*0.75);
			mHiddenParams.y = SCREEN_SIZE_Y - HIDDEN_BTN_SIZE_Y;
		}
		else if(USER_SPECIAL_MODE == 2) {				// right-handed && VERTICAL
			mHiddenParams.x = SCREEN_SIZE_X - HIDDEN_BTN_SIZE_X;
			mHiddenParams.y = (int)(SCREEN_SIZE_Y*0.65);
			Log.w("TEST",""+mTouchpadParams.y);
		}
		else if(USER_SPECIAL_MODE == 3) {				// left-handed && HORIZONTAL
			mHiddenParams.x = (int)(SCREEN_SIZE_X*0.15);
			mHiddenParams.y = SCREEN_SIZE_Y - HIDDEN_BTN_SIZE_Y;
		}
		else if(USER_SPECIAL_MODE == 4) {				// left-handed && VERTICAL
			mHiddenParams.x = 0;
			mHiddenParams.y = (int)(SCREEN_SIZE_Y*0.7);;
		}
	}
	
	/**
	 * Touchpad Coordinates Setting
	 */
	private void setTouchpadPosition (int handed) {
			
		// Right Handed
		if(pointer_handed == RIGHT_HANDED) {
			
			// Horizontal Mode
			if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mTouchpadParams.gravity = Gravity.CENTER;
				mTouchpadParams.x = (int)(SCREEN_SIZE_X * 0.3);
				mTouchpadParams.y = (int)(SCREEN_SIZE_Y * 0.2);
				mHiddenView.setImageResource(R.drawable.tp_1);
				USER_SPECIAL_MODE = 1;
			}
			
			// Vertical Mode
			else if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
			{
				mTouchpadParams.gravity = Gravity.CENTER;
				mTouchpadParams.x = (int)(SCREEN_SIZE_X * 0.25);
				mTouchpadParams.y = (int)(SCREEN_SIZE_Y * 0.2);
				mHiddenView.setImageResource(R.drawable.t_p);
				USER_SPECIAL_MODE = 2;
			}
		}
		// Left-handed
		else if(pointer_handed == LEFT_HANDED) { 
					
			// Horizontal Mode
			if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mTouchpadParams.gravity = Gravity.CENTER;
				mTouchpadParams.x = (int)(SCREEN_SIZE_X * -0.3);
				mTouchpadParams.y = (int)(SCREEN_SIZE_Y * 0.2);
				mHiddenView.setImageResource(R.drawable.tp_1);
				USER_SPECIAL_MODE = 3;
			}
			
			// Vertical Mode
			else if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
			{
				mTouchpadParams.gravity = Gravity.CENTER;
				mTouchpadParams.x = (int)(SCREEN_SIZE_X * -0.25);
				mTouchpadParams.y = (int)(SCREEN_SIZE_Y * 0.2);
				mHiddenView.setImageResource(R.drawable.tp_2);
				USER_SPECIAL_MODE = 4;
			}
		}
		
		// Set Hidden Button Position
		setHiddenButtonPosition();
	}
	

	/**
	 * In case, Rotate a Device
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		getScreenSize();
	
		// Left Handed
		if( pointer_handed == LEFT_HANDED) { 
			// Horizontal Mode
			if( newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mTouchpadParams.gravity = Gravity.CENTER;
				mTouchpadParams.x = (int)(SCREEN_SIZE_X * -0.3);
				mTouchpadParams.y = (int)(SCREEN_SIZE_Y * 0.2);
				mHiddenView.setImageResource(R.drawable.tp_1);
				USER_SPECIAL_MODE = 3;
			}
			// Vertical Mode
			else if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
				mTouchpadParams.gravity = Gravity.CENTER;
				mTouchpadParams.x = (int)(SCREEN_SIZE_X * -0.25);
				mTouchpadParams.y = (int)(SCREEN_SIZE_Y * 0.2);
				mHiddenView.setImageResource(R.drawable.tp_2);
				USER_SPECIAL_MODE = 4;
			}
		}
		
		// Right Handed
		else if( pointer_handed == RIGHT_HANDED) {
			// Horizontal Mode
			if( newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mTouchpadParams.gravity = Gravity.CENTER;
				mTouchpadParams.x = (int)(SCREEN_SIZE_X * 0.3);
				mTouchpadParams.y = (int)(SCREEN_SIZE_Y * 0.2);
				mHiddenView.setImageResource(R.drawable.tp_1);
				USER_SPECIAL_MODE = 1;
			}
			// Vertical Mode
			else if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
				mTouchpadParams.gravity = Gravity.CENTER;
				mTouchpadParams.x = (int)(SCREEN_SIZE_X * 0.25);
				mTouchpadParams.y = (int)(SCREEN_SIZE_Y * 0.2);
				mHiddenView.setImageResource(R.drawable.t_p);
				USER_SPECIAL_MODE = 2;
			}
		}
		// Set Hidden Button Position
		setHiddenButtonPosition();
		
		// Resetting Mousepointer Position
		mMouseParams.x = SCREEN_SIZE_X / 2;
		mMouseParams.y = SCREEN_SIZE_Y / 2;
		
		// View Update
		mWindowManager.updateViewLayout(mTouchpadView, mTouchpadParams);
		mWindowManager.updateViewLayout(mMouseView, mMouseParams);
	}
	
	@Override
	public void onDestroy() {
		if(mWindowManager != null) {		//서비스 종료시 뷰 제거. *중요 : 뷰를 꼭 제거 해야함.
			if(mTouchpadView != null) mWindowManager.removeView(mTouchpadView);
			if(mMouseView != null) mWindowManager.removeView(mMouseView);
		}
		super.onDestroy();
	}
	
	// Gesture Listener
	SimpleOnGestureListener mGestureListener = new SimpleOnGestureListener() {
				
		// Single Tap
		public boolean onSingleTapConfirmed(MotionEvent ev) {		
			try { 						
				//  Programmatical Click Event
				/*process = Runtime.getRuntime().exec(
						"su -c input tap " + mMouseParams.x + " "
								+ mMouseParams.y);*/
				if(IsProcessKilled == false) {
				
				process = runtime.exec("su -c input tap " + mMouseParams.x + " "
						+ mMouseParams.y);
				IsProcessKilled = true;
				}
				else {
					process.destroy();
					process = runtime.exec("su -c input tap " + mMouseParams.x + " "
							+ mMouseParams.y);
				}

				/*try {
					Thread.sleep(2000);
					process.destroy();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				
				playSound();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			touchpadOut(0);
	        return true;
		}
		
		// Double Tap
		public boolean onDoubleTap(MotionEvent ev) {
			try { 						
				//  Programmatical Click Event
				/*process = Runtime.getRuntime().exec(
						"su -c input tap " + mMouseParams.x + " "
								+ mMouseParams.y);*/
				if(IsProcessKilled == false) {
				
				process = runtime.exec("su -c input touchscreen swipe " + 0 + " "
						+ 400 + " " + 0 + " " + 9000 + " " + 100);
				IsProcessKilled = true;
				}
				else {
					process.destroy();
					process = runtime.exec("su -c input touchscreen swipe " + 0 + " "
							+ 400 + " " + 0 + " " + 9000 + " " + 100);
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
	    }

		// onScroll
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    		
        	if(((mMouseParams.x > SCREEN_SIZE_X) && (distanceX < 0)) || ((mMouseParams.x < 0) && (distanceX > 0))) { // Outside X
        		if(((mMouseParams.y > SCREEN_SIZE_Y) && (distanceY < 0)) || ((mMouseParams.y < 0) && (distanceY > 0))) { // Outside Y
    			}
    			else { // Not Outside Y && Outside X
    				mMouseParams.y -= ((float)pointer_sensitivity / MOUSE_POINTER_SPEED) * RATIO_SCREEN_BY_PAD_Y * distanceY;
    			}
    		}
    		else { // Not Outside X
    			if(((mMouseParams.y > SCREEN_SIZE_Y) && (distanceY < 0)) || ((mMouseParams.y < 0) && (distanceY > 0))) { // Outside Y
    				mMouseParams.x -= ((float)pointer_sensitivity / MOUSE_POINTER_SPEED) * RATIO_SCREEN_BY_PAD_Y * distanceX;
    			}
    			else { // Not Outside X && Not Outside X
    				mMouseParams.x -= ((float)pointer_sensitivity / MOUSE_POINTER_SPEED) * RATIO_SCREEN_BY_PAD_Y * distanceX;
    				mMouseParams.y -= ((float)pointer_sensitivity / MOUSE_POINTER_SPEED) * RATIO_SCREEN_BY_PAD_Y * distanceY;
    			}
    		}
        	
    		mWindowManager.updateViewLayout(mMouseView, mMouseParams);
    		return true;
        }
		
		// Long Press
        public void onLongPress(MotionEvent ev) {
	        showQuickActionMenu();
	        quickAction.show(mTouchpadView);
	        quickAction.setAnimStyle(QuickAction.ANIM_REFLECT);		// Animation
        }
	};
	
	////////////////////// button test
	public void touchpadIn() {
		mTouchpadView.setImageResource(R.drawable.touchpad_fin2);
	}
	public void touchpadOut(int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mTouchpadView.setImageResource(R.drawable.touchpad3);
	}
	
	// Binder
	public class LocalBinder extends Binder {
		AlwaysOnTopService getService() {
			return AlwaysOnTopService.this;
		}
	}
	
	private final IBinder mBinder = new LocalBinder();
	
	// To Communicate between Activity and Service
	@Override
	public IBinder onBind(Intent arg0) { return mBinder; }
}