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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.util.Log;
import android.util.TypedValue;
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
	public boolean bIfcamera = false;
	private boolean flashLight = false;
	private static Integer pictureSize=0;
	private static Integer pictureSizeW=0;
	private static Integer pictureSizeH=0;


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
//		RelativeLayout.LayoutParams tvLp = (LayoutParams) tv.getLayoutParams();
//		tvLp.width = screenWidth/2;
//		tv.setLayoutParams(tvLp);
		
		
		
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
				if(!bIfcamera)
				{
			        //initCamera
					try {
						Log.i(TAG, "initCamera");
		            	closeCamera();
						initCamera();
						bIfcamera = true;
						btnProc.setText("����ʶ��");
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
	            	takeCamera();
					bIfcamera = false;
					btnProc.setText("�����");
				}

	  		}
        });
        
        btnPreview = (Button) findViewById(R.id.btn_preview);
        btnPreview.setOnClickListener(new Button.OnClickListener()
        {
			@Override
			public void onClick(View arg0) {
				doStartApplicationWithPackageName("so.ofo.labofo");
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
							Log.i(TAG, "flashLight Torch");
						} else {
			            	Camera.Parameters mParameters = Camera1.getParameters();
			            	mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			            	Camera1.setParameters(mParameters);							
							Log.i(TAG, "flashLight Off");
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
		
		deleteFile("/sdcard/ofo_ocr/qr.jpg");
		
		//bmp --> rgb_pixels_data
		int[] pixels = new int[w*h];
		bmp.getPixels(pixels, 0, w, 0, 0, w, h);
		
		//rgb_pixels_data --> gray_pixels_data
		int feature2 = ofo.grayProc(pixels, w, h,0);
		
		String resultFeature2="";
		
		if(feature2==1) {
			resultFeature2="δ�ҵ���ά��";
		} else if(feature2==2) {
			resultFeature2="δ�ҵ�����λ��";
		} else if(feature2==3) {
			resultFeature2="δ�ָ�7������";
		} else if(feature2==0) {
			resultFeature2="JNI���ش���";
		} else {
			resultFeature2 = Integer.toString(feature2);
			Log.i(TAG, "return: "+ resultFeature2);
		}
		
		String resultQr = qrZxing();
		String [] resultQrSplitSlash =  null;
		String [] resultQrSplitEqual =  null;
		String resultQrString = null;
		int resultQrInt = 0;
		if (resultQr != null)
		{
			Log.i(TAG, "QR: " + resultQr);
			try{
				resultQrSplitSlash = resultQr.split("/");
				resultQrSplitEqual = resultQrSplitSlash[resultQrSplitSlash.length-1].split("=");
				resultQrString = resultQrSplitEqual[resultQrSplitEqual.length-1];
				Log.i(TAG, "QR: " + resultQrString);
				resultQrInt = Integer.parseInt(resultQrString);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if(feature2>10)
		{
			//���˺ܾ���Դ���Ҳ���Matתjintarray��ֻ�����ļ�������
			String filepatha = "/sdcard/ofo_ocr/temp.jpg";
			File file = new File(filepatha);
			if (file.exists()) {
	            Bitmap bm = BitmapFactory.decodeFile(filepatha);
				imageview.setImageBitmap(bm);
			}
			tv.setText(resultFeature2);
			tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 40);
			ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setText(resultFeature2);
			Toast.makeText(this, resultFeature2+"ʶ��ɹ�1��������ofo", Toast.LENGTH_LONG).show();
		}
		else if((feature2<10) && (resultQrInt>10)) {
			tv.setText(resultQrString);
			tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 40);
			ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setText(resultQrString);
			Toast.makeText(this, resultQrString+"ʶ��ɹ�2��������ofo", Toast.LENGTH_LONG).show();
		} else{
			tv.setText(resultFeature2+"\nʶ��ʧ��2\n�������ճ���");
			tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
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
    				debugTemp = "set square:"+Integer.toString(pictureSize) + " " + debugTemp;
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
        		while (pictureItor.hasNext()){
        			Camera.Size cur = pictureItor.next();
        			if (((cur.width/4) == (cur.height/3))&&(cur.height>=1200))
        			{
        				pictureSizeW = cur.width;
        				pictureSizeH = cur.height;
        				debugTemp = "set 4:3:"+Integer.toString(pictureSize) + " " + debugTemp;
        				break;
        			}
        		}

    			Toast.makeText(this, "�����ֻ���֧������������ͼƬ������ʱ�뽫ofo���ƾ�������", Toast.LENGTH_LONG).show();
        		parameters.setPreviewSize(640, 480);
        		parameters.setPictureSize(pictureSizeW, pictureSizeH);
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
        		pictureSize = pictureSizeH;
        		bmp = Bitmap.createBitmap(bmp, 0, 0, pictureSizeH, pictureSizeH, null,false); 
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
    			sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(strCaptureFilePath)))); //�㲥����ͼ��
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
    
    
    public String qrZxing() {
		String filepathQR = "/sdcard/ofo_ocr/qr.jpg";
		File fileQR = new File(filepathQR);
		if (fileQR.exists()) {
	        Bitmap bmpQR = BitmapFactory.decodeFile(filepathQR);
//	        imageview.setImageBitmap(bmpQR);
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bmpQR.compress(Bitmap.CompressFormat.PNG, 100, stream);
			Log.i(TAG, Integer.toString(bmpQR.getWidth())+"*"+Integer.toString(bmpQR.getHeight()));
			byte[] dataQR = getYUV420sp(bmpQR.getWidth(), bmpQR.getHeight(), bmpQR);   //Ҫ��ת��YUV420��ת�ɶ�ֵͼ����ʶ��
			PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(  
					dataQR, bmpQR.getWidth(), bmpQR.getHeight(), 0, 0, bmpQR.getWidth(), bmpQR.getHeight(), false); 
//	        tv.setText("text");
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
			Reader reader = new QRCodeReader();
	        try {
	            Result result = reader.decode(bitmap);
	            String textQR = result.getText();
//	            textQR=textQR.substring(textQR.length()-7);
//	            int intQR = Integer.parseInt(textQR); 
	            //tv.setText(textQR);
	            return textQR;
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
		}
		return null;
    }
    
	/**
	 * YUV420sp
	 * 
	 * @param inputWidth
	 * @param inputHeight
	 * @param scaled
	 * @return
	 */
	public static byte[] getYUV420sp(int inputWidth, int inputHeight,
			Bitmap scaled) {
		int[] argb = new int[inputWidth * inputHeight];

		scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

		byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];
		
		encodeYUV420SP(yuv, argb, inputWidth, inputHeight);

//		scaled.recycle();

		return yuv;
	}
	
	/**
	 * RGBתYUV420sp
	 * 
	 * @param yuv420sp
	 *            inputWidth * inputHeight * 3 / 2
	 * @param argb
	 *            inputWidth * inputHeight
	 * @param width
	 * @param height
	 */
	private static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width,
			int height) {
		// ֡ͼƬ�����ش�С
		final int frameSize = width * height;
		// ---YUV����---
		int Y, U, V;
		// Y��index��0��ʼ
		int yIndex = 0;
		// UV��index��frameSize��ʼ
		int uvIndex = frameSize;

		// ---��ɫ����---
//		int a, R, G, B;
		int R, G, B;
		//
		int argbIndex = 0;
		//

		// ---ѭ���������ص㣬RGBתYUV---
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {

				// a is not used obviously
//				a = (argb[argbIndex] & 0xff000000) >> 24;
				R = (argb[argbIndex] & 0xff0000) >> 16;
				G = (argb[argbIndex] & 0xff00) >> 8;
				B = (argb[argbIndex] & 0xff);
				//
				argbIndex++;

				// well known RGB to YUV algorithm
				Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
				U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
				V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

				//
				Y = Math.max(0, Math.min(Y, 255));
				U = Math.max(0, Math.min(U, 255));
				V = Math.max(0, Math.min(V, 255));

				// NV21 has a plane of Y and interleaved planes of VU each
				// sampled by a factor of 2
				// meaning for every 4 Y pixels there are 1 V and 1 U. Note the
				// sampling is every other
				// pixel AND every other scanline.
				// ---Y---
				yuv420sp[yIndex++] = (byte) Y;
				// ---UV---
				if ((j % 2 == 0) && (i % 2 == 0)) {
					//
					yuv420sp[uvIndex++] = (byte) V;
					//
					yuv420sp[uvIndex++] = (byte) U;
				}
			}
		}
	}

	
	private void doStartApplicationWithPackageName(String packagename) {

		// ͨ��������ȡ��APP��ϸ��Ϣ������Activities��services��versioncode��name�ȵ�
		PackageInfo packageinfo = null;
		try {
			packageinfo = getPackageManager().getPackageInfo(packagename, 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		if (packageinfo == null) {
			return;
		}

		// ����һ�����ΪCATEGORY_LAUNCHER�ĸð�����Intent
		Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
		resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		resolveIntent.setPackage(packageinfo.packageName);

		// ͨ��getPackageManager()��queryIntentActivities��������
		List<ResolveInfo> resolveinfoList = getPackageManager()
				.queryIntentActivities(resolveIntent, 0);

		ResolveInfo resolveinfo = resolveinfoList.iterator().next();
		if (resolveinfo != null) {
			// packagename = ����packname
			String packageName = resolveinfo.activityInfo.packageName;
			// �����������Ҫ�ҵĸ�APP��LAUNCHER��Activity[��֯��ʽ��packagename.mainActivityname]
			String className = resolveinfo.activityInfo.name;
			// LAUNCHER Intent
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);

			// ����ComponentName����1:packagename����2:MainActivity·��
			ComponentName cn = new ComponentName(packageName, className);

			intent.setComponent(cn);
			startActivity(intent);
		}
	}
    /**
     * ɾ�������ļ�
     * @param   filePath    ��ɾ���ļ����ļ���
     * @return �ļ�ɾ���ɹ�����true�����򷵻�false
     */
    public boolean deleteFile(String filePath) {
    File file = new File(filePath);
        if (file.isFile() && file.exists()) {
        return file.delete();
        }
        return false;
    }
}
