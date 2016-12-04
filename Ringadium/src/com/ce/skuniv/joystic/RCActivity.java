package com.ce.skuniv.joystic;


import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ce.skuniv.R;
import com.ce.skuniv.app.App;
import com.ce.skuniv.mw.MultiWii220;

import communication.BT;

public class RCActivity extends Activity implements SensorEventListener {

	private boolean killme = false;
	
	App app;
	RCActivity rcAct;
	Timer timer;
	Handler mHandler = new Handler();
	SensorManager mSensorManager;
	SensorEventListener accL;
	SensorEventListener magL;
	Sensor mSensor; 
	Sensor mSensorpress;
	Sensor preSensor;
	TextView outputX,outputY,outputZ,thrval;


    private static TextView mTitle;
    private static TextView mTextProgress;
    private static Button mButton;
    JoystickView joystick;
    ThrstickView joystick2;
 	
	public static final boolean DEBUG = true;
	public static final String LOG_TAG = "QuadOgada";
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;	
    
	private boolean mEnablingBT;
    private boolean mLocalEcho = false;
    private int mFontSize = 9;
    private int mColorId = 2;
    private int mControlKeyId = 0;
	
    private static final String LOCALECHO_KEY = "localecho";
    private static final String FONTSIZE_KEY = "fontsize";
    private static final String COLOR_KEY = "color";
    private static final String CONTROLKEY_KEY = "controlkey";

    public static final int WHITE = 0xffffffff;
    public static final int BLACK = 0xff000000;
    public static final int BLUE = 0xff344ebd;

    private static int speedCnt;
    private static int progressBackup;
    private static byte sendData;
    private boolean _isBtnDown;
    private boolean _isLeft;
    private boolean _isGyro;
    private boolean _isSensor;
    
    private static final int[][] COLOR_SCHEMES = {
        {BLACK, WHITE}, {WHITE, BLACK}, {WHITE, BLUE}};

    private static String[] CONTROL_KEY_NAME;

    private int mControlKeyCode;
    
//    private BluetoothAdapter mBluetoothAdapter = null;
//	private String mConnectedDeviceName = null;
	
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";	
    private final int GOOGLE_STT = 1000;
    private ArrayList<String> mResult;									//음성인식 결과 저장할 list
	
//	private MenuItem mMenuItemConnect;
	private static BT mSerialService = null;
	private static MultiWii220 mw220 = null;
	TextView txtX, txtY, txtState;
	Button bnOn,bnOff,bnvoice;
	ToggleButton bntoggle;
	TextView textv1,textv2;
	
	int firsttime=0;
	int TState=0;
	int voiceword_num=0;
	boolean diff;
	boolean voiceok=false;
	int[] CH8 = { 1500,1500,1500,1500,0,0,0,0 };
	int backup_exch8[] = { 0,0,0,0,0,0,0,0 };
	String wingardium = "윙가르디움 레비오사";
	
	
	
	private Runnable update = new Runnable() {
		@Override
		public void run() {

			app.mw.ProcessSerialData(app.loggingON);
			app.Frequentjobs();

			// app.mw.SendRequest();
			app.mw.SendRequestMSP_SET_RAW_RC(CH8);

			
			
			
			if (!killme)
				mHandler.postDelayed(update, app.RefreshRate);
			
		}
	};

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		app = (App) getApplication();

//		app.commMW.SetHandler(mHandler1);
//		app.commFrsky.SetHandler(mHandler1);

		super.onCreate(savedInstanceState);
		
//	    requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.remote_control);
//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);	// 화면 안 꺼지도록
        
//        mw220.ProcessSerialData(app.loggingON);
		
//	    mTitle = (TextView) findViewById(R.id.title_left_text);
//	    mTitle.setText(R.string.app_name);
//	    mTitle = (TextView) findViewById(R.id.title_right_text); 
//	    mButton = (Button) findViewById(R.id.title_button);
	    
        joystick = (JoystickView)findViewById(R.id.joystickView);	        
	    joystick.setOnJoystickMovedListener(_listener);
        joystick2 = (ThrstickView)findViewById(R.id.joystickView2);	        
	    joystick2.setOnThrstickMovedListener(_listener2);
	    
		bnOn = (Button)findViewById(R.id.On);
		bnOff = (Button)findViewById(R.id.Off);
		bnvoice = (Button)findViewById(R.id.voice);
		bntoggle = (ToggleButton)findViewById(R.id.isSensor);
		_isSensor = true;
		
//		textv1 = (TextView)findViewById(R.id.tv1);
//		textv2 = (TextView)findViewById(R.id.tv2);
        outputX = (TextView) findViewById(R.id.tv1);
        outputY = (TextView) findViewById(R.id.tv2);
        outputZ = (TextView) findViewById(R.id.tv3);
        thrval = (TextView) findViewById(R.id.tv4);
        
    	// start motion detection
		mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
		
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorpress = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		mSensorManager.registerListener(this, mSensor,SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mSensorpress,SensorManager.SENSOR_DELAY_NORMAL);		
		
		bnOn.setOnClickListener(new OnClickListener(){//시동걸기
			public void onClick(View v){	
				
				for (int i = 0; i < app.mw.BoxNames.length; i++) {
					for (int j = 0; j < 12; j++) {
						app.mw.Checkbox[i][j] = false;						
					}
				}
					app.mw.Checkbox[0][2] = true;
					app.mw.Checkbox[6][0] = true; //시동 AUX1
					app.mw.Checkbox[6][2] = false;
					app.mw.SendRequestMSP_SET_BOX();
					app.mw.SendRequestMSP_EEPROM_WRITE();
					CH8[3] = 1000;
			    	timer = new Timer();
			    	timer.scheduleAtFixedRate(timerTask, 0, 7);
			    	

				}
		});

		bnOff.setOnClickListener(new OnClickListener(){//시동끄기
			public void onClick(View v){	 	        	 	
				
				for (int i = 0; i < app.mw.BoxNames.length; i++) {
					for (int j = 0; j < 12; j++) {
						app.mw.Checkbox[i][j] = false;						
					}
				}	
					app.mw.Checkbox[0][2] = true;
					app.mw.Checkbox[6][0] = false;					
					app.mw.Checkbox[6][2] = true;					
					app.mw.SendRequestMSP_SET_BOX();
					app.mw.SendRequestMSP_EEPROM_WRITE();

					
				}
		});

		bnvoice.setOnClickListener(new OnClickListener(){//음성인식
			public void onClick(View v){	
				Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);			//intent 생성
				i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());	//음성인식을 호출한 패키지
				i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");							//음성인식 언어 설정
				i.putExtra(RecognizerIntent.EXTRA_PROMPT, "말을 하세요.");						//사용자에게 보여 줄 글자
				
				startActivityForResult(i, GOOGLE_STT);												//구글 음성인식 실행
				

				}			
		});
		
		bntoggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {     
			@Override
			public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
				if (isChecked) {
					_isSensor=false;
                }else {                	
                	_isSensor=true;
                }
            }
		});



	}
	
	TimerTask timerTask = new TimerTask() {
		public void run() {
			app.mw.SendRequestMSP_SET_RAW_RC(CH8);
			
		}
	};   
    
	   private JoystickMovedListener _listener = new JoystickMovedListener() {

			@Override
			public void OnMoved(int pan, int tilt) {
				String mode="";
				int xval=0,yval=0;
				double r;
//				txtX.setText(Integer.toString(pan));
//				txtY.setText(Integer.toString(tilt));
				xval=tilt;
				yval=pan;
				//thrval.setText("ele: "+xval);
				if(_isSensor==false){
					if((tilt-pan)<=0 && (tilt+pan)<=0){//elef
						xval+=95;
						xval=1000+(xval*5);
						xval=2950-xval;
						eleforward(xval);
						mode="ele forward";
		//				outputZ.setText("ele: "+xval);
					}
					if((tilt-pan)<=0 && (tilt+pan)>=0){//rudr
						yval+=95;
						yval=1000+(yval*5);
						
						rudright(yval);
						mode="rud right";
					}
					if((tilt-pan)>0 && (tilt+pan)>0){//eleb
						xval+=95;
						xval=1000+(xval*5);
						xval=2950-xval;
						elebackward(xval);
						mode="ele backward";
		//				outputZ.setText("ele: "+xval);
					}
					if((tilt-pan)>0 && (tilt+pan)<0){//rudl
						yval+=95;
						yval=1000+(yval*5);
						rudleft(yval);
						mode="rud left";
					}
					/*
					if(pan==0 && tilt==0){
						speedNeutral();
					}
					txtState.setText(mode);
					 */			
					}
			}




			@Override
			public void OnReleased() {
//				txtX.setText("released");
//				txtY.setText("released");
			}
			
			public void OnReturnedToCenter() {
//				txtX.setText("stopped");
//				txtY.setText("stopped");
			};
		};
		
		   private ThrstickMovedListener _listener2 = new ThrstickMovedListener() {

				@Override
				public void OnMoved(int pan, int tilt) {
					String mode="";
					int xval=0,yval=0;
					double r;
//					txtX.setText(Integer.toString(pan));
//					txtY.setText(Integer.toString(tilt));
//					Log.d("x", Integer.toString(pan));			
//					Log.d("y", Integer.toString(tilt));			
							
					xval=tilt;
					yval=pan;
					
					if(tilt<10 && (pan<=10 && pan>=-10)){ //throttle
						xval+=130;	
						if(xval%4==0 && xval!=0){
							Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
							vibe.vibrate(50);
						}
						thrval.setText("THR: "+(xval*10));
						thrforward(xval);
					}
					
					if(pan<=-8){ // roll
						yval+=95;
						//thrval.setText("AIL: "+yval);
						ailleft(yval);
					}
					if(pan>=8){
						yval+=95;
						//thrval.setText("AIL: "+yval);
						ailright(yval);
					}
					
					/*
					if(pan==0 && tilt==0){
						speedNeutral();
					}
					txtState.setText(mode);
					 */			
				}

				@Override
				public void OnReleased() {
//					txtX.setText("released");
//					txtY.setText("released");
				}
				
				public void OnReturnedToCenter() {
//					txtX.setText("stopped");
//					txtY.setText("stopped");
				};
			};
		
/*		
	    public void speedNeutral() {
	    	byte[] buffer = new byte[4];
	    	
	    	sendData = 0;
	    	
	    	buffer[0] = (byte)0x55;
	    	buffer[1] = (byte)0xAA;
	    	buffer[2] = (byte)0x01;
	    	buffer[3] = (byte)0x50;
	 
	    	mSerialService.Write(buffer);  	
	    }    
*/	    
			
	    public void thrforward(int val) {
	    	CH8[3] = (10*val);
	    	
	    	
	    }     
	    public void ailright(int val) {
			CH8[0] = 1000+val;						
	    } 
	    public void ailleft(int val) {
			CH8[0] = 1000+val;			
	    }
	    public void eleforward(int val) {
			CH8[1] = val;	
//			thrval.setText("ele: "+CH8[1]);
	    }     	    
	    public void elebackward(int val) {
			CH8[1] = val;
//			thrval.setText("ele: "+CH8[1]);
	    } 
	    public void rudright(int val) {
			CH8[2] = val;
//			thrval.setText("rud: "+CH8[2]);
	    } 
	    public void rudleft(int val) {
			CH8[2] = val;
//			thrval.setText("rud: "+CH8[2]);
	    }
		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			float dataX, dataY, dataZ;
			int rollleft,rollright,pitchfor,pitchback;
			
	        synchronized (this) {
				float var0 = event.values[0];
				float var1 = event.values[1];
				float var2 = event.values[2];

	            switch (event.sensor.getType()){
		            case Sensor.TYPE_ACCELEROMETER:
	                    outputX.setText("x:"+var0);
	                    outputY.setText("y:"+var1);
	                    outputZ.setText("z:"+var2);
	//                    outputZ.setText("z:"+Float.toString(event.values[2]));
	                    
	                    dataX = var0;
	                    dataY = var1;
	                    dataZ = var2;
	                    if(_isSensor==true){
		               		if(dataY <= -1.0 && (dataX < 1.0 && dataX > -1.0)) {//roll left
		//            			touchHandler.sendEmptyMessage(8); /* left */
		               			rollleft=(int)((dataY+10)*50+1000);
		               			rudleft(rollleft);
		               			
		            		} if(dataY >= 1.0 && (dataX < 1.0 && dataX > -1.0)){//roll right
		//            			touchHandler.sendEmptyMessage(4); /* right */
		            			rollright=(int)((dataY+10)*50+1000);
		               			rudright(rollright);
		            			
		            		} if(dataX <= -1.0 && (dataY < 1.0 && dataY > -1.0)){//pitch
		//            			touchHandler.sendEmptyMessage(2); /* fwd */
		            			pitchfor=(int)((dataX+10)*50+1000);
		            			pitchfor=2950-pitchfor;
		//            			outputX.setText("x:"+pitchfor);
		               			eleforward(pitchfor);
		            			
		            		} if(dataX > 1.0 && (dataY < 1.0 && dataY > -1.0)){//pitch
		//            			touchHandler.sendEmptyMessage(1); /* rwd */
		            			pitchback=(int)((dataX+10)*50+1000);
		            			pitchback=2950-pitchback;
		//            			outputY.setText("y:"+pitchback);
		               			elebackward(pitchback);	            			
		            		} 
	                    }
	            	/*	
	            		else if(dataZ > 11.0){//throttle
	            			
	            		}
	            	*/	
	 /*
	            		else if((dataX >= 4)&&(dataX <= 5)&&(dataY >=-4.0)&&(dataY<=4.0)){
		            			Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
							vibe.vibrate(50);
	            		}                
	*/

	    			case Sensor.TYPE_PRESSURE:
//	    				outputX.setText("x:"+var0);
//	                    outputY.setText("y:"+var1);
//	    				outputZ.setText("z:"+var0);
	    			break;
	            }
	        }

			
		}
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}	 
		
		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data){
			if( resultCode == RESULT_OK  && (requestCode == GOOGLE_STT) ){		//결과가 있으면
				SaveResultDialog(requestCode, data);				//결과를 다이얼로그로 출력.
				
				for(int i=0;i<voiceword_num;i++){
					if(mResult.get(i).equals(wingardium)){
						voiceok=true;					
//						outputX.setText("r:"+wingardium);
//						outputY.setText("v:"+mResult.get(0));
					}
					else
						;
				}
				
				if(voiceok){

					for (int j = 0; j < app.mw.BoxNames.length; j++) {
						for (int k = 0; k < 12; k++) {
							app.mw.Checkbox[j][k] = false;						
						}
					}
						app.mw.Checkbox[0][2] = true;
						app.mw.Checkbox[6][0] = true; //시동 AUX1
						app.mw.Checkbox[6][2] = false;
						app.mw.SendRequestMSP_SET_BOX();
						app.mw.SendRequestMSP_EEPROM_WRITE();
						CH8[3] = 1000;
				    	timer = new Timer();
				    	timer.scheduleAtFixedRate(timerTask, 0, 7);
				    	
				    	thrval.setText("r:"+voiceok);
					}
				else{
					thrval.setText("r:"+voiceok);
				}
			}
			else{															//결과가 없으면 에러 메시지 출력
				String msg = null;
				
				//내가 만든 activity에서 넘어오는 오류 코드를 분류
				switch(resultCode){
					case SpeechRecognizer.ERROR_AUDIO:
						msg = "오디오 입력 중 오류가 발생했습니다.";
						break;
					case SpeechRecognizer.ERROR_CLIENT:
						msg = "단말에서 오류가 발생했습니다.";
						break;
					case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
						msg = "권한이 없습니다.";
						break;
					case SpeechRecognizer.ERROR_NETWORK:
					case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
						msg = "네트워크 오류가 발생했습니다.";
						break;
					case SpeechRecognizer.ERROR_NO_MATCH:
						msg = "일치하는 항목이 없습니다.";
						break;
					case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
						msg = "음성인식 서비스가 과부하 되었습니다.";
						break;
					case SpeechRecognizer.ERROR_SERVER:
						msg = "서버에서 오류가 발생했습니다.";
						break;
					case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
						msg = "입력이 없습니다.";
						break;
				}
				
				if(msg != null)		//오류 메시지가 null이 아니면 메시지 출력
					Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			}
		}
		
		//결과 list 출력하는 다이얼로그 생성
		private void SaveResultDialog(int requestCode, Intent data){
			String key = "";
			if(requestCode == GOOGLE_STT)					//구글음성인식이면
				key = RecognizerIntent.EXTRA_RESULTS;	//키값 설정
			
			mResult = data.getStringArrayListExtra(key);		//인식된 데이터 list 받아옴.
			String[] result = new String[mResult.size()];			//배열생성. 다이얼로그에서 출력하기 위해
			mResult.toArray(result);									//	list 배열로 변환
			
			voiceword_num=mResult.size();
			
			
			
		}
		
	 }

	


/*
	    class AsyncTaskListener extends AsyncTask<Integer, Integer, Integer> {
	    	 
	    	         protected void onCancelled(Integer... params) {
	    	             super.onCancelled();
	    	         }
	    	         @Override
	    	         protected void onPostExecute(Integer result) {
//	    	             btn.setText("Thread END");
	    	             super.onPostExecute(result);
	    	         }
	    	 
	    	         @Override
	    	         protected void onPreExecute() {
//	    	             btn.setText("Thread START!!!!");
	    	             super.onPreExecute();
	    	         }
	    	         protected Integer doInBackground(Integer... params) {
	    	             int result = 0;
	    	             int i=0;
	    	             int exch8[]={0,0,0,0,0,0,0,0};
			
	    	             
	    	             for( Integer v : params )	exch8[i++] = v ; // int -> Integer 변환
	    	             Log.d("msg","diff: "+diff);
	    	             Log.d("msg","CH8[3]: "+CH8[3]);
	    	             do{	    	            	 
	    	            	 app.mw.SendRequestMSP_SET_RAW_RC(exch8);
							//Thread.sleep(900);
	    	             }while(diff);
	    	             
	    	             return result;
	    	         }
	    	     }
	    	 
*/

