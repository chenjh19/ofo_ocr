package com.scut.ofo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SurfaceHolder.Callback{
	
	private static String TAG = "ofoocr";
	private Camera Camera1;
	private Button btnPreview;
	private Button btnProc;
	private Button btnPic;
	private Button btnFlashLight;
	public ImageView imageview;
	public Bitmap bmp;
	public TextView tv;
	private SurfaceView SurfaceView1;
	private SurfaceHolder SurfaceHolder1;
	public boolean bIfPreview = false;
	private boolean flashLight = false;
	private static Integer pictureSize=0;

	String filePath = "/sdcard/ofo_ocr/";
	String cameraPath = "";

	//OpenCV�����ز���ʼ���ɹ���Ļص��������ڴ����ǲ������κβ���
	static {
		Log.i(TAG, "TryloadLibrary");
		System.loadLibrary("ofo");
		Log.i(TAG, "loadLibrary");
	}                	

	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"Start");
        makeRootDirectory(filePath);
        //start ȫ��
        //���ر�����
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //����״̬��
        //����ȫ������
        int flag=WindowManager.LayoutParams.FLAG_FULLSCREEN;
        //��õ�ǰ�������
        Window window=MainActivity.this.getWindow();
        //���õ�ǰ����Ϊȫ����ʾ
        window.setFlags(flag, flag);
        //end ȫ��
        
        //start ����svmtrain.txt��sdcard
        InputStream inStream = getResources().openRawResource(R.raw.svmtrain);
		try {
        FileOutputStream fileOutputStream = new FileOutputStream("/sdcard/ofo_ocr/svmtrain.txt");
        byte[] buffer = new byte[10];
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        int len = 0;
        while((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] bs = outStream.toByteArray();
        fileOutputStream.write(bs);
        outStream.close();
        inStream.close();
        fileOutputStream.flush();
        fileOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        //end ����svmtrain.txt��sdcard
		
		//start camera·��
		cameraPath = Environment.getExternalStorageDirectory().toString();
		cameraPath += "/DCIM/Camera/" ;
		makeRootDirectory(cameraPath);
		Log.i(TAG, cameraPath);
		//end camera·��
        
		
        setContentView(R.layout.activity_main);
        
        imageview = (ImageView) findViewById(R.id.image_view);
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ofo);
        imageview.setImageBitmap(bmp);
        
        tv = (TextView) findViewById(R.id.text_view);
        
        
        //use SurfaceView for camera preview
        SurfaceView1 = (SurfaceView) findViewById(R.id.mSurfaceView1);
        
        //bind SurfaceView to get SurfaceHolder
        SurfaceHolder1 = SurfaceView1.getHolder();
        
        //SurfaceHolder.callback
        SurfaceHolder1.addCallback(MainActivity.this);
        SurfaceHolder1.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		//start���ô�С
		WindowManager wm = this.getWindowManager();
		int screenWidth = wm.getDefaultDisplay().getWidth();
		Log.i(TAG, Integer.toString(screenWidth));
		

		//surfaceview
		RelativeLayout.LayoutParams sfLp = (LayoutParams) SurfaceView1.getLayoutParams();
		sfLp.height = screenWidth;
		sfLp.width = screenWidth;
		SurfaceView1.setLayoutParams(sfLp);
		
		//imageview
		RelativeLayout.LayoutParams ivLp = (LayoutParams) imageview.getLayoutParams();
		ivLp.width = screenWidth/6*2;
		ivLp.height = screenWidth/6*2;
		imageview.setLayoutParams(ivLp);
		
		//textview
		RelativeLayout.LayoutParams tvLp = (LayoutParams) tv.getLayoutParams();
		tvLp.width = screenWidth/2;
		tv.setLayoutParams(tvLp);
		
		
		
		//end����view��С
		
        btnPic = (Button) findViewById(R.id.btn_pic);
        btnPic.setOnClickListener(new Button.OnClickListener()
        {
        	public void onClick(View arg0) {
        		Intent intent = new Intent();
        		intent.setType("image/*");
        		intent.setAction(Intent.ACTION_GET_CONTENT);
        		startActivityForResult(intent, 1);
        		
        	}
        	
        });

        btnProc = (Button) findViewById(R.id.btn_camera);
        btnProc.setOnClickListener(new Button.OnClickListener()
        {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
            	takeCamera();
//            	closeCamera();
				try {
					initCamera();
				} catch (IOException e) {
					e.printStackTrace();
				}

	  			}
        });
        
        btnPreview = (Button) findViewById(R.id.btn_preview);
        btnPreview.setOnClickListener(new Button.OnClickListener()
        {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
		        //initCamera
				try {
					Log.i(TAG, "initCamera");
	            	closeCamera();
					initCamera();
				} catch (IOException e) {
					e.printStackTrace();
				}
	  			}
        });
        
        btnFlashLight = (Button) findViewById(R.id.btn_flashLight);
        btnFlashLight.setOnClickListener(new Button.OnClickListener()
        {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
		        //flashLight
				try {
					if(Camera1 != null ){
						if(flashLight == false)
						{
			            	Camera.Parameters mParameters = Camera1.getParameters();
			            	mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			            	Camera1.setParameters(mParameters);
			            	flashLight = true;
//							Log.i(TAG, "flashLight Torch");
						} else {
			            	Camera.Parameters mParameters = Camera1.getParameters();
			            	mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			            	Camera1.setParameters(mParameters);							
//							Log.i(TAG, "flashLight Off");
							flashLight = false;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
	  			}
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();  // Always call the superclass method first
        closeCamera();
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeCamera();
    }
    
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        closeCamera();
    }
    
	protected void processOfo() {
		int w = bmp.getWidth();
		int h = bmp.getHeight();
		
		//bmp --> rgb_pixels_data
		int[] pixels = new int[w*h];
		bmp.getPixels(pixels, 0, w, 0, 0, w, h);
		
		//rgb_pixels_data --> gray_pixels_data
		int feature2 = ofo.grayProc(pixels, w, h,0);
		
		if(feature2==1) {
			tv.setText("δ���ҵ���ά��");
			Toast.makeText(this, "δ���ҵ���ά��", Toast.LENGTH_LONG).show();
		} else if(feature2==2) {
			tv.setText("δ�ܶ�λ����λ��");
			Toast.makeText(this, "δ�ܶ�λ����λ��", Toast.LENGTH_LONG).show();
		} else if(feature2==3) {
			tv.setText("δ�ָܷ�7������");
			Toast.makeText(this, "δ�ָܷ�7������", Toast.LENGTH_LONG).show();
		} else if(feature2==0) {
			tv.setText("JNI�ӿڷ��ش���");
			Toast.makeText(this, "JNI�ӿڷ��ش���", Toast.LENGTH_LONG).show();
		} else {
			String resultOfoOcr = "";
			resultOfoOcr = Integer.toString(feature2);
			Log.i(TAG, "return: "+ resultOfoOcr);
			tv.setText(resultOfoOcr);
						
			//���˺ܾ���Դ���Ҳ���Matתjintarray��ֻ�����ļ�������
			String filepatha = "/sdcard/ofo_ocr/temp.jpg";
			File file = new File(filepatha);
			if (file.exists()) {
	            Bitmap bm = BitmapFactory.decodeFile(filepatha);
				imageview.setImageBitmap(bm);
			}
			
			ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setText(resultOfoOcr);
			Toast.makeText(this, resultOfoOcr+"ʶ��ɹ�����ֱ������ofo��", Toast.LENGTH_LONG).show();
			
		}

    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) { //���ڴ�ͼ���ж�ȡ
		if (resultCode == RESULT_OK) {
			Uri uri = data.getData();
			Log.e("uri", uri.toString());
			ContentResolver cr = this.getContentResolver();
			try {
				bmp = BitmapFactory.decodeStream(cr.openInputStream(uri));
				ImageView imageView = (ImageView) findViewById(R.id.image_view);
				/* ��Bitmap�趨��ImageView */
				imageView.setImageBitmap(bmp);
        		processOfo();
        		bmp = null;       	
			} catch (FileNotFoundException e) {
				Log.e("Exception", e.getMessage(),e);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}        
        
	
	
	
    //��ʼ�����
    private int initCamera() throws IOException
    {
    	if(!bIfPreview)
    	{
    		//�ж�����Ƿ�ΪԤ��ģʽ
    		Camera1 = Camera.open();
    	}
    	
    	if(Camera1 != null && !bIfPreview)
    	{
    		Log.i(TAG,"inside the camera");
    		Camera.Parameters parameters = Camera1.getParameters();

    		
    		//start getSupportedPreviewSizes
    		String debugTemp = "";
    		List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
    		Iterator<Camera.Size> itor = sizeList.iterator();
    		
    		Integer previewSize=0;
    		while (itor.hasNext()){
    			Camera.Size cur = itor.next();
    			Log.i(TAG,Integer.toString(cur.width)+"*"+Integer.toString(cur.height));
    			if ((cur.width == cur.height)&&(cur.width>=500))
    			{
    				previewSize = cur.width;
    				debugTemp = Integer.toString(previewSize);
    				break;
    			}
    		}
    		//end getSupportedPreviewSizes			

    		//start getSupportedPictureSizes
    		pictureSize=0;
    		List<Camera.Size> pictureSizeList = parameters.getSupportedPictureSizes();
    		Iterator<Camera.Size> pictureItor = pictureSizeList.iterator();
    		while (pictureItor.hasNext()){
    			Camera.Size cur = pictureItor.next();
    			Log.i(TAG,Integer.toString(cur.width)+"*"+Integer.toString(cur.height));
    			if ((cur.width == cur.height)&&(cur.width>=500))
    			{
    				pictureSize = cur.width;
    				debugTemp = Integer.toString(pictureSize) + " " + debugTemp;
    				break;
    			}
    		}
    		
    		//error
//    		if ((previewSize==0)||(pictureSize==0))
//    		{
//    			Toast.makeText(this, "�����ֻ���֧������������ͼƬ���볢��ʹ��ͼ��ʶ��", Toast.LENGTH_LONG).show();
//    		}
    		
    		//end getSupportedPictureSizes
    		
    		//start autoFocus
    		List<String> focusModes = parameters.getSupportedFocusModes();
    		if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
    			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    		}

    		//end autoFocus
   		
    		//start rotation
//    		android.hardware.Camera.CameraInfo info =
//    	             new android.hardware.Camera.CameraInfo();
// 			Log.i(TAG,"ori"+Integer.toString(info.orientation));
    		//end rotation
    		
    		
    		parameters.setPictureFormat(PixelFormat.JPEG);
    		
    		if ((previewSize==0)||(pictureSize==0))
    		{
    			Toast.makeText(this, "�����ֻ���֧������������ͼƬ������ʱ�뽫ofo���ƾ�������", Toast.LENGTH_LONG).show();
        		parameters.setPreviewSize(640, 480);
        		parameters.setPictureSize(1600, 1200);
    		} else {
    		parameters.setPreviewSize(previewSize, previewSize);
    		parameters.setPictureSize(pictureSize, pictureSize);
    		}
    		Camera1.setParameters(parameters);
    		
    		Camera1.setPreviewDisplay(SurfaceHolder1);
    		
    		Camera1.setDisplayOrientation(90);
    		
    		closeCamera();
    		
    		Camera1.startPreview();
    		bIfPreview = true;
    		
    	}
		return 0;
    	
    	
    }
    
    private void closeCamera()
    {
		if(Camera1 != null && bIfPreview)
		{
			Camera1.stopPreview();
			Camera1.lock();
			Camera1.release(); //android 4.4Ҫrelease�������ٿ���crash
			Camera1 = null;
			bIfPreview = false;
		}
    }

    private void takeCamera()
    {
    	if (Camera1 != null && bIfPreview)
    	{
    		Camera1.takePicture(shutterCallback, rawCallback, jpegCallback);
    	}
    }
    
    private ShutterCallback shutterCallback = new ShutterCallback()
    {
    	public void onShutter()
    	{
    		
    	}
    };
    
    private PictureCallback rawCallback = new PictureCallback()
    {
    	public void onPictureTaken(byte[] _data,Camera _camera)
    	{
    		
    	}
    };
    
    private PictureCallback jpegCallback = new PictureCallback()
    {
    	public void onPictureTaken(byte[] _data,Camera _camera)
    	{
    		//onPictureTaken����ĵ�һ��������Ϊ��Ƭ��byte
    		bmp = BitmapFactory.decodeByteArray(_data, 0, _data.length);
    		
    		//start �����ֻ���֧��������
    		if (pictureSize==0)
    		{
        		pictureSize=1200;
        		bmp = Bitmap.createBitmap(bmp, 0, 0, 1200, 1200, null,false); 
    		}
    		
    		//end �����ֻ���֧��������

    		//start resize &rotate
    		float scaleWidth = ((float) 800) / pictureSize;
    	    float scaleHeight = ((float) 800) / pictureSize;
    	    // CREATE A MATRIX FOR THE MANIPULATION
    	    Matrix matrix = new Matrix();
    	    // RESIZE THE BIT MAP
    	    matrix.postScale(scaleWidth, scaleHeight);
    	    // RESIZE THE BIT MAP
    	    matrix.postRotate(90);
    	    
    	    // "RECREATE" THE NEW BITMAP
    	    Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, pictureSize, pictureSize, matrix, false);
    	    bmp = resizedBitmap;
    	    
    	    //end resize & rotate

    	    

    		//��ʱ�����ļ���
    		long time=System.currentTimeMillis();
    		SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss",Locale.getDefault());
    		Date d1=new Date(time);
    		String t1=format.format(d1);
    		String strCaptureFilePath = cameraPath+"ofoocr_"+t1+".jpg";
    		File myCaptureFile = new File(strCaptureFilePath);
    		
    		try
    		{
    			BufferedOutputStream bos = new BufferedOutputStream (new FileOutputStream(myCaptureFile));
    			//ѹ��
    			bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
    			//����
    			bos.close();
    			
    			//��ʾ
    			imageview.setImageBitmap(bmp);
    			
    			processOfo();
    		}
    		catch (Exception e)
    		{
    			Log.e(TAG,e.getMessage());
    		}
    	}
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		
	}
	
    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e+"");
        }
    }    
    
}
