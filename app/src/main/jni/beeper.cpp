#include <jni.h>
#include <math.h>
#include <string.h>

#define min(a, b) (a < b ? a : b)

extern "C" {
    JNIEXPORT jfloatArray JNICALL Java_ru_ytkab0bp_beamklipper_service_WebService_generateTone(JNIEnv *env, jclass, jint numSamples, jfloat invFreq) {
        jfloatArray arr = env->NewFloatArray(numSamples);
        jfloat* sample = env->GetFloatArrayElements(arr, JNI_FALSE);

        int period = M_PI * invFreq;
        int min = min(numSamples, period);
        for (int i = 0; i < min; i++) {
            sample[i] = sin(2 * M_PI * i / invFreq);
        }
        min = min(numSamples, period);
        for (int i = period; i < numSamples; i += period) {
            memcpy(sample + i, sample, sizeof(double) * min(numSamples - i, period));
        }
        env->ReleaseFloatArrayElements(arr, sample, JNI_ABORT);

        return arr;
    }
}