//
// Created by homer on 04/10/2018.
//
#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
using namespace cv;
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_alvin_camerasource_MainActivity_stringFromJNI(JNIEnv *env,jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_alvin_camerasource_MainActivity_HumanDetection(JNIEnv *env, jclass type,jlong addrRgba) {
    Mat& mRgba = *(Mat*)addrRgba;

}
void detectHuman(Mat& frame){
    String human_cascade_name = "/storage/emulated/0/data/haarcascade_fullbody.xml";
    CascadeClassifier human_cascade;
    if(!human_cascade.load(human_cascade_name)){
        printf("--(!)Error loading");
    }
    std::vector<Rect> humans;
    Mat frameGray;
    cvtColor(frame,frameGray,CV_BGR2GRAY);
    equalizeHist(frameGray,frameGray);
    human_cascade.detectMultiScale(frameGray,humans,1.1,2,0|CV_HAAR_SCALE_IMAGE,Size(30,30));
    for(int i=0;i<humans.size();i++ ){
        rectangle(frame,Point(humans[i].x,humans[i].y),Point(humans[i].x+humans[i].width,humans[i].y+humans[i].height),Scalar(0,255,0));
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_alvin_camerasource_MainActivity_HumanDetection(JNIEnv *env, jclass type,
                                                                jlong addrRgba) {

    // TODO

}