#include <jni.h>
#include <android/log.h>
#include <stdlib.h>

/**
 * Based on Linux & Simulator branches of Klipper
 */

#define TAG "beam_host_mcu"

extern "C" {
    #include <sched.h> // sched_setscheduler sched_get_priority_max
    #include <stdio.h> // fprintf
    #include <string.h> // memset
    #include <unistd.h> // getopt
    #include <sys/mman.h> // mlockall MCL_CURRENT MCL_FUTURE
    #include "board/misc.h" // console_sendf
    #include "command.h" // DECL_CONSTANT
    #include "internal.h" // console_setup
    #include "sched.h" // sched_main

    void
    command_config_reset(uint32_t *args)
    {
        if (!sched_is_shutdown())
            shutdown("config_reset only available when shutdown");
    }
    DECL_COMMAND_FLAGS(command_config_reset, HF_IN_SHUTDOWN, "config_reset");

    JNIEXPORT int JNICALL Java_ru_ytkab0bp_beamklipper_serial_HostSerial_runNative(JNIEnv *env, jclass, jstring file) {
        const char* chars = env->GetStringUTFChars(file, JNI_FALSE);

        int ret = console_setup((char*) chars);
        env->ReleaseStringUTFChars(file, chars);
        if (ret)
            return -1;

        sched_main();
        console_shutdown();
        return 0;
    }
}