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


//JNImain
extern "C" {
JNIEXPORT jintArray JNICALL Java_com_scut_ofo_ofo_grayProc  (JNIEnv* env, jclass obj, jintArray buf, jint w, jint h,jint ror) //����Ҫ�������ɳ�����.h�ļ���д
{
    jint *cbuf;
    cbuf = env->GetIntArrayElements(buf,JNI_FALSE);
    if(cbuf == NULL){
        return 0;
    }

    Mat imgData(h, w, CV_8UC4, (unsigned char*)cbuf);
    Mat lastImg;
    imgData.copyTo(lastImg);
    //s1-02:rotate
    cv::Mat rotateImg;
    //���������ĵ�
    cv::Mat M = cv::getRotationMatrix2D(Point2f(lastImg.cols/2,lastImg.rows/2),ror,1);
    //����warpAffine��ת
    cv::warpAffine(lastImg,rotateImg,M,rotateImg.size());
    rotateImg.copyTo(lastImg);
    cv::imwrite("/sdcard/ofo_ocr/temp.jpg",lastImg);
    return 0;

}
}
