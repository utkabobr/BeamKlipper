#include <jni.h>
#include <math.h>
#include <string.h>

#define min(a, b) (a < b ? a : b)

extern "C" {
    JNIEXPORT jfloatArray JNICALL Java_ru_ytkab0bp_beamklipper_service_WebService_generateTone(JNIEnv *env, jclass, jint numSamples, jfloat freq) {
        jfloatArray arr = env->NewFloatArray(numSamples);
        jfloat* sample = env->GetFloatArrayElements(arr, JNI_FALSE);

        int period = (int)(M_PI * (1 / freq) + 0.5);
        int min = min(numSamples, period);
        sample[0] = 0;
        sample[1] = sin(2 * M_PI * freq);
        float k = 2 * cos(2 * M_PI * freq);
        for (int i = 2; i < min; i++) {
            sample[i] = k * sample[i - 1] - sample[i - 2];
        }

        min = min(numSamples, period);
        for (int i = period; i < numSamples; i += period) {
            memcpy(sample + i, sample, sizeof(double) * min(numSamples - i, period));
        }
        env->ReleaseFloatArrayElements(arr, sample, JNI_ABORT);

        return arr;
    }
}