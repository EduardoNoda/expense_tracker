#include <jni.h>

extern "C"
JNIEXPORT jint JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_ping(
        JNIEnv*,
        jobject
) {
    return 42;
}


extern "C"
JNIEXPORT jlongArray JNICALL
Java_br_com_expensetracker_bridge_CoreBridge_getMonthSummary(JNIEnv *env, jobject thiz, jint month, jint year) {
    // TODO: implement getMonthSummary()
}