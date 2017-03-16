#include <jni.h>
#include <com_scut_ofo_MainActivity.h> //��ĳ��Լ���
#include <opencv2/opencv.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/ml/ml.hpp>
#include <string>
#include <vector>
#include <sstream>
#include <android/log.h>

using namespace cv;
using namespace std;
float feature2Extract[7][32/8*72/8];
int resultOfoOcr[8];
int img2feature(Mat lastImg,int ror)
{
    //s1-01:resize
    cv::Mat resizeImg;
    cv::resize(lastImg,resizeImg,Size(800,800));
    resizeImg.copyTo(lastImg);

    //s1-02:rotate
    cv::Mat rotateImg;
    //���������ĵ�
    cv::Mat M = cv::getRotationMatrix2D(Point2f(lastImg.cols/2,lastImg.rows/2),ror,1);
    //����warpAffine��ת
    cv::warpAffine(lastImg,rotateImg,M,rotateImg.size());
    rotateImg.copyTo(lastImg);

    //s1-03 rgb2gray
    Mat grayImg;
    cv::GaussianBlur(lastImg,grayImg,Size(11,11),0,0);  //��˹
    cv::cvtColor(grayImg,grayImg,COLOR_RGB2GRAY);

    grayImg.copyTo(lastImg);

    //s1-04:bw
    Mat bwImg;
    cv::Laplacian(lastImg,bwImg,-1,3); //��Ե��
    cv::threshold(bwImg,bwImg,0,255,CV_THRESH_OTSU+CV_THRESH_BINARY);
    cv::fastNlMeansDenoising(bwImg,bwImg,30,15,31); //ȥ��
    bwImg.copyTo(lastImg);

    //s1-05:��һ�����ͣ�ȥ�������е����
    Mat dilateImg1;
    Mat element1 = cv::getStructuringElement(MORPH_RECT,Size(3,3));
    cv::dilate(lastImg,dilateImg1,element1);
    cv::erode(dilateImg1,dilateImg1,element1);
    dilateImg1.copyTo(lastImg);

    //s1-06:������Ӿ��Σ���Ѱ��QR(7λ)
    Mat rectQRFindImg;
    int QRFinded = 0;
    int QRx,QRy,QRwidth,QRheight;
    cv::Point2f QRFindRotatePoint[4];//������ת
    lastImg.copyTo(rectQRFindImg);
    cv::threshold(rectQRFindImg,rectQRFindImg,0,255,CV_THRESH_OTSU+CV_THRESH_BINARY);

    std::vector< std::vector< cv::Point> > contoursQRFind;
    cv::findContours(rectQRFindImg,contoursQRFind,CV_RETR_EXTERNAL,CV_CHAIN_APPROX_NONE);
    for (int i=0; i<contoursQRFind.size(); i++)
    {
        //������С��Ӿ���
        cv::Rect rectQRFind = cv::boundingRect(Mat(contoursQRFind[i]));
//        __android_log_print(ANDROID_LOG_INFO, "ofoocr", "s1-05: %d * %d", rectQRFind.width,rectQRFind.height);
        //�ж��Ƿ�ΪQR
        if(
                ((float(rectQRFind.width)/rectQRFind.height)>0.7)&&
                ((float(rectQRFind.width)/rectQRFind.height)<1.4)&&
                ((rectQRFind.width)>200)&&
                ((rectQRFind.width)<400)&&
//                ((rectQRFind.x>10))&&
                ((rectQRFind.x<200))&&
                ((rectQRFind.y>50))&&
                ((rectQRFind.y<500))
//                (Mat(contoursQRFind[i]).size())>800)

                )

        {
            cv::rectangle(rectQRFindImg,rectQRFind,Scalar(255,255,255));

            __android_log_print(ANDROID_LOG_INFO, "ofoocr", "s1-05: %d * %d", rectQRFind.width,rectQRFind.height);

            QRx = rectQRFind.x;
            QRy = rectQRFind.y;
            QRwidth = rectQRFind.width;
            QRheight = rectQRFind.height;
            QRFinded++;
            //��ת
            cv::RotatedRect rectQRFindRotate = cv::minAreaRect(Mat(contoursQRFind[i]));
            rectQRFindRotate.points(QRFindRotatePoint);
            for (int j=0; j<=3; j++)
            {
                line(rectQRFindImg,QRFindRotatePoint[j],QRFindRotatePoint[(j+1)%4],Scalar(255,255,255),10);
            }
        }
    }

    rectQRFindImg.copyTo(lastImg);

    if(QRFinded != 1)
    {
    	__android_log_print(ANDROID_LOG_INFO, "ofoocr", "ERROR: Can't Find correct QR");
    	return 1;
    }


    //s1-08����QR��ת
    cv::Mat rotateOfoImg;
    rotateImg.copyTo(rotateOfoImg); //��֪Ϊʲô��lastImg�����rotateImg
    double angelOfo;
    //��Ҫ��һ��Ѱ�����ϽǺ����Ͻǵ㡣�뵽��˼·��x+y��С��Ϊ���Ͻǣ����������������Ͻ�
    int findLeft,findLeftXY = 800*800;
    for (int j=0; j<=3; j++)
    {
        if ((QRFindRotatePoint[j].x + QRFindRotatePoint[j].y) < findLeftXY)
        {
            findLeftXY = QRFindRotatePoint[j].x + QRFindRotatePoint[j].y;
            findLeft = j;
        }
    }

    // ��arctan������ת�Ƕȣ���ʽ(y/x)*180/3.14
    angelOfo = atan ((QRFindRotatePoint[findLeft+1].y - QRFindRotatePoint[findLeft].y) / (QRFindRotatePoint[findLeft+1].x - QRFindRotatePoint[findLeft].x)) * 180 / 3.14;
    //���������ĵ�
    cv::Mat M2 = cv::getRotationMatrix2D(Point2f(rotateOfoImg.cols/2,rotateOfoImg.rows/2),angelOfo,1);
    //����warpAffine��ת
    //���� Դ��Ŀ�꣬�Ƕȣ��ߴ�
    cv::warpAffine(rotateOfoImg,rotateOfoImg,M2,rotateOfoImg.size());
    rotateOfoImg.copyTo(lastImg);

    //s1-09 ���Խ�������QRһ��crop����
    Mat cropOfo7CharImg;
    rotateOfoImg.copyTo(cropOfo7CharImg);

    //�������빫ʽ
    // sqrt(pow((x2-x1),2)+pow((y2-y1),2))
    float distanceQRx = sqrt(pow((QRFindRotatePoint[findLeft+1].x - QRFindRotatePoint[findLeft].x),2) + pow((QRFindRotatePoint[findLeft+1].y - QRFindRotatePoint[findLeft].y),2));
    float distanceQRy = sqrt(pow((QRFindRotatePoint[findLeft].x - QRFindRotatePoint[findLeft-1].x),2) + pow((QRFindRotatePoint[findLeft].y - QRFindRotatePoint[findLeft-1].y),2));
    cout<<"distanceX: "<<distanceQRx<<" Y: "<<distanceQRy<<endl;

    int maskOfo7CharX = distanceQRx*1.4;
    if((maskOfo7CharX + QRx + distanceQRx) > 800)
    {
        maskOfo7CharX = 800-(QRx + distanceQRx);
    }

    cv::Rect maskOfo7Char(QRx + distanceQRx, QRy + (distanceQRy*0.6), maskOfo7CharX, distanceQRy*0.5);
    cv::Mat croppedOfo7CharImg(cropOfo7CharImg, maskOfo7Char);
    lastImg.empty();
    croppedOfo7CharImg.copyTo(lastImg);

    //s2-01 ��С�����ٴν�����ֵ�˲����Ҷȡ���ֵ��������
    Mat ofo7CharReProcessImg;
    int Ofo7CharFind = 0;
    int Ofo7CharX,Ofo7CharY,Ofo7CharWidth,Ofo7CharHeight;
    croppedOfo7CharImg.copyTo(ofo7CharReProcessImg);
    cv::medianBlur(ofo7CharReProcessImg,ofo7CharReProcessImg,11);//��ֵ�˲�
    cv::cvtColor(ofo7CharReProcessImg,ofo7CharReProcessImg,COLOR_BGR2GRAY);
    cv::Canny(ofo7CharReProcessImg,ofo7CharReProcessImg,50,150,3);//ȡ��Ե
    Mat elementOfo7CharReProcess = cv::getStructuringElement(MORPH_RECT,Size(25,1));//�������Ͳ���
    cv::dilate(ofo7CharReProcessImg,ofo7CharReProcessImg,elementOfo7CharReProcess); //����
    cv::threshold(ofo7CharReProcessImg,ofo7CharReProcessImg,0,255,CV_THRESH_OTSU+CV_THRESH_BINARY); //�ٴζ�ֵ����Ϊ���ƾ�����׼��
    std::vector< std::vector< cv::Point> > contoursOfo7CharFind; //���ָ��
    cv::findContours(ofo7CharReProcessImg,contoursOfo7CharFind,CV_RETR_EXTERNAL,CV_CHAIN_APPROX_NONE); //������
    for (int i=0; i<contoursOfo7CharFind.size(); i++)
    {
        //������С��Ӿ���
        cv::Rect rectOfo7CharFind = cv::boundingRect(Mat(contoursOfo7CharFind[i]));
        //�ж��Ƿ�Ϊ7���ַ�
        if(
                (((float(rectOfo7CharFind.width))/rectOfo7CharFind.height)>3.2)&&
                (((float(rectOfo7CharFind.width))/rectOfo7CharFind.height)<4.8)&&
                ((rectOfo7CharFind.width>distanceQRx)||(rectOfo7CharFind.width>distanceQRy))
                )
        {
            cv::rectangle(ofo7CharReProcessImg,rectOfo7CharFind,Scalar(255,255,255));
            cout<<"Draw7Char: "<< rectOfo7CharFind.width <<"*"<<rectOfo7CharFind.height<<endl;
            Ofo7CharX=rectOfo7CharFind.x;
            Ofo7CharY=rectOfo7CharFind.y;
            Ofo7CharWidth=rectOfo7CharFind.width;
            Ofo7CharHeight=rectOfo7CharFind.height;
            Ofo7CharFind++;
        }
    }

    if(Ofo7CharFind!=1)
    {
    	__android_log_print(ANDROID_LOG_INFO, "foo", "ERROR:Can't Find OFO 7 Char position");
    	return 2;
    }

    ofo7CharReProcessImg.copyTo(lastImg);

    //s2-02 crop����
    Mat cropReProcessImg;
    croppedOfo7CharImg.copyTo(cropReProcessImg);
    cv::Rect maskOfo7CharReProcessImg(Ofo7CharX, Ofo7CharY, Ofo7CharWidth,Ofo7CharHeight);
    cv::Mat croppedOfo7CharReProcessImg(cropReProcessImg, maskOfo7CharReProcessImg);

    croppedOfo7CharReProcessImg.copyTo(lastImg);


    //s2-03 ��7�����ҳ���
    Mat char7findImg;
    Mat char7findImgBw;
    //charList�ṹ��1���,x,y,w,h
    int charList[7][5] = {};
    int charListCount = 0;
    croppedOfo7CharReProcessImg.copyTo(char7findImg);
//    cout<<char7findImg.cols<<"*"<<char7findImg.rows<<endl;
    cv::medianBlur(char7findImg,char7findImg,5);//��ֵ�˲�
    cv::cvtColor(char7findImg,char7findImg,COLOR_BGR2GRAY);
    cv::threshold(char7findImg,char7findImg,0,255,CV_THRESH_OTSU+CV_THRESH_BINARY); //�ٴζ�ֵ����Ϊ���ƾ�����׼��
    char7findImg.copyTo(char7findImgBw);

    std::vector< std::vector< cv::Point> > contourschar7findImg; //���ָ��
    cv::findContours(char7findImg,contourschar7findImg,CV_RETR_EXTERNAL,CV_CHAIN_APPROX_NONE); //������
    for (int i=0;i<contourschar7findImg.size();i++)
    {
        //������С��Ӿ���
        cv::Rect rectRotateReProcess=cv::boundingRect(Mat(contourschar7findImg[i]));
//        cout<<"Char:"<<rectRotateReProcess.width<<"*"<<rectRotateReProcess.height<<" x:"<<rectRotateReProcess.x<<" y:"<<rectRotateReProcess.y<<endl;
        if (
                (((float(rectRotateReProcess.height))/rectRotateReProcess.width) > 1.5)&&
                (((float(rectRotateReProcess.height))/rectRotateReProcess.width) < 4)&&
                (rectRotateReProcess.height > ((char7findImg.rows)*0.67))
                )
        {
            cv::rectangle(char7findImg,rectRotateReProcess,Scalar(255,255,255));


            charList[charListCount][1] = rectRotateReProcess.x;
            charList[charListCount][2] = rectRotateReProcess.y;
            charList[charListCount][3] = rectRotateReProcess.width;
            charList[charListCount][4] = rectRotateReProcess.height;
            charListCount++;
        }
    }
    if(charListCount!=7)
    {
    	__android_log_print(ANDROID_LOG_INFO, "ofoocr", "ERROR: Can't find ALL 7 char");
    	return 3;
    }

    char7findImg.copyTo(lastImg);

    //s2-04����
    //ð������
    int charSortTemp[5];
    for (int i=0; i<7; i++)
    {
        for (int j=i+1; j<7; j++)
        {
            if(charList[i][1] > charList[j][1])
            {
                for(int k=0; k<5; k++)
                {
                    charSortTemp[k] = charList[i][k];
                    charList[i][k] = charList[j][k];
                    charList[j][k] = charSortTemp[k];
                }
            }
        }
    }

    //s2-05 crop����������resize 32*72
    Mat croppedCharListImg[7];
    Mat croppedCharTempImg;

    char7findImgBw.copyTo(croppedCharTempImg);
    for (int i=0; i<7; i++)
    {
        cv::Rect maskCharList(charList[i][1],charList[i][2],charList[i][3],charList[i][4]);
        cv::Mat croppedChar(croppedCharTempImg, maskCharList);
        croppedChar.copyTo(croppedCharListImg[i]);
        cv::resize(croppedCharListImg[i],croppedCharListImg[i],Size(32,72),0,0,INTER_CUBIC);

        //��ΪADT�ò���to_string��ֻ����������debug~_~
//        if(i==0)
//        {
//        	cv::imwrite("/sdcard/ofo_ocr/temp0.jpg",croppedCharListImg[i]);
//        } else if(i==6) {
//        	cv::imwrite("/sdcard/ofo_ocr/temp6.jpg",croppedCharListImg[i]);
//        }
    }


    //s2-06 ��Mat����ת���ɶ�ά������ΪMat����ֻ����һάָ����������̫�鷳��
    int imgArray[7][72][32];
    for(int i=0; i<7; i++)
    {
        //�ȶ�ֵ��
        cv::threshold(croppedCharListImg[i],croppedCharListImg[i],0,255,CV_THRESH_OTSU+CV_THRESH_BINARY);
        croppedCharListImg[i].convertTo(croppedCharListImg[i],CV_8U);
        cv::MatIterator_<cv::Vec3b> it = croppedCharListImg[i].begin<cv::Vec3b>();
        int t=0;
        for(; it!=croppedCharListImg[i].end<cv::Vec3b>(); it++)
        {
            if((((*it)[0])==255)&&(((*it)[1])==255)&&(((*it)[2])==255))
            {
                imgArray[i][t/32][t%32] = 1;
            } else {
                imgArray[i][t/32][t%32] = 0;
            }
            t++;
        }
    }

    //s2-07 ����feature2
    for (int i=0; i<7; i++)
    {
        cv::MatIterator_<cv::Vec3b> it = croppedCharListImg[i].begin<cv::Vec3b>();
        int arrayNum = 0;
        for(int j=0; j<72; j=j+8)
        {
            for (int k=0; k<32; k=k+8)
            {
                int countBW = 0;
                for (int lj=0; lj<8; lj++)
                {
                    for(int lk=0; lk<8; lk++)
                    {
                        if ((imgArray[i][j+lj][k+lk]) == 1)
                        {
                            countBW++;
                        }
                    }
                }
                feature2Extract[i][arrayNum] = countBW;
                arrayNum++;
            }
        }
    }
    //�����log����۲� (�����ÿ��������Qt���������1)
    for (int i=0; i<7; i++)
    {
        for (int j=0; j<32/8*72/8; j++)
        {
        	if(feature2Extract[i][j] == 0)
        	{
        		feature2Extract[i][j] = 0.01; //�ؼ�����������Ϊ0�ľ���ֵΪ0.01������Ԥ��Ƶ�ֵ��ȫ����ͬ
        	}

        }
    }
    cv::imwrite("/sdcard/ofo_ocr/temp.jpg",lastImg);
    return 0;
}

//JNImain
extern "C" {
JNIEXPORT jint JNICALL Java_com_scut_ofo_ofo_grayProc  (JNIEnv* env, jclass obj, jintArray buf, jint w, jint h,jint ror) //��ĳ��Լ�
{
    jint *cbuf;
    cbuf = env->GetIntArrayElements(buf,JNI_FALSE);
    if(cbuf == NULL){
        return 0;
    }

    Mat imgData(h, w, CV_8UC4, (unsigned char*)cbuf);
    Mat lastImg;
    imgData.copyTo(lastImg);
    int imgProcResult=img2feature(lastImg,ror);
	__android_log_print(ANDROID_LOG_INFO, "ofoocr", "img2feature: %d ",imgProcResult);

    //ѵ�� 3.1.0
//    using namespace cv::ml;
//    Ptr<SVM> svm = SVM::create();
//    svm->setType(SVM::C_SVC);
//    svm->setKernel(SVM::POLY);
//    svm->setDegree(3);
//    svm->setGamma(0.01);
//    svm->setCoef0(2);
//    svm->setTermCriteria(TermCriteria(TermCriteria::MAX_ITER, 100, 1e-6));
//    svm->train(tm,ROW_SAMPLE, labelsMat);
//    svm->save("/sdcard/ofo_ocr/svmtrain1775687.txt");
//	__android_log_print(ANDROID_LOG_INFO, "foo", "train finish");


    int result=0;


    if(imgProcResult==0)
    {
        //Ԥ��SVM 3.1.0
        for (int i=0; i<7; i++)
        {
            Mat trainningDataMat(1,32/8*72/8,CV_32FC1,feature2Extract[i]);
        	//����ѵ��
    //        int q = svm->predict(trainningDataMat);

            //ʵս
            cv::Ptr<cv::ml::SVM> mSvm2;
            mSvm2 = Algorithm::load<cv::ml::SVM>("/sdcard/ofo_ocr/svmtrain.txt");
            int q2 = mSvm2->predict(trainningDataMat);

        	__android_log_print(ANDROID_LOG_INFO, "ofoocr", "%d ",q2);
        	resultOfoOcr[i] = q2;
        	result += (pow(float(10),(6-i)))*q2;
        }
    	__android_log_print(ANDROID_LOG_INFO, "ofoocr", "result: %d ",result);

    } else {
        result = imgProcResult;
    }

//    jintArray jarr = env->NewIntArray(8);
    jintArray jarr = env->NewIntArray(1);
    jint *arr = env->GetIntArrayElements(jarr,NULL);
//    for (int i=0; i<8; i++)
//    {
//  		arr[i] = resultOfoOcr[i];
//    }
//    arr[0]=result;
	return result;
}
}
