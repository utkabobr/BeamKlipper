#include <android/log.h>
#include <jni.h>
#include <pthread.h>
#include <unistd.h>
#include <malloc.h>
#include <asm-generic/fcntl.h>
#include <asm-generic/mman.h>
#include <sys/mman.h>
#include "pty/pty.h"

#define BUFFER_SIZE 4096
#define TAG "beam_serial"

jclass mProxyClass;
jmethodID mProxyOnDataReceivedMethod;

static JavaVM *staticVM = nullptr;

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

struct SerialPort {
    int master, slave;
    jstring file;
    pthread_t worker_thread;
};

struct WorkerArgs {
    char* file;
    struct termios tio;
    int master, slave;
    jweak proxy;
};

[[noreturn]] static void* worker(void* arguments) {
    mlockall(MCL_CURRENT | MCL_FUTURE);
    WorkerArgs* args = (WorkerArgs*) arguments;

    fd_set read_fds, exception_fds;
    int code = TIOCPKT_NOSTOP;

    char* buffer = (char*) malloc(BUFFER_SIZE);
    int count;
    ioctl(args->master, TIOCPKT, &code);

    JavaVMAttachArgs vmArgs;
    vmArgs.name = nullptr;
    vmArgs.group = nullptr;
    vmArgs.version = JNI_VERSION_1_6;

    JNIEnv* env;
    staticVM->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);

    tcgetattr(args->master, &args->tio);
    args->tio.c_lflag = 0;
    args->tio.c_cflag = 176;
    args->tio.c_iflag = 0;
    args->tio.c_oflag = 0;
    tcsetattr(args->master, TCSANOW, &args->tio);

    tcgetattr(args->slave, &args->tio);
    args->tio.c_lflag = 0;
    args->tio.c_cflag = 176;
    args->tio.c_iflag = 0;
    args->tio.c_oflag = 0;
    tcsetattr(args->slave, TCSANOW, &args->tio);

    while (true) {
        FD_ZERO(&read_fds);
        FD_SET(args->master, &read_fds);
        FD_ZERO(&exception_fds);
        FD_SET(args->master, &exception_fds);

        code = select(args->master + 1, &read_fds, nullptr, &exception_fds, nullptr);
        if (code == -1) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to do select(...)");
            pthread_exit(0);
        }

        if ((count = read(args->master, buffer, BUFFER_SIZE - 1)) < 0) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Failed to read pty");
        } else if (count > 1) {
            staticVM->AttachCurrentThreadAsDaemon(&env, &vmArgs);

            jbyteArray arr = env->NewByteArray(count - 1);
            env->SetByteArrayRegion(arr, 0, count - 1, reinterpret_cast<const jbyte *>(buffer + 1));
            env->CallVoidMethod(args->proxy, mProxyOnDataReceivedMethod, arr);

            staticVM->DetachCurrentThread();
        }
    }
}

extern "C" {
    int JNI_OnLoad(JavaVM *vm, void*) {
        JNIEnv *env;
        if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
            return JNI_ERR;
        }

        staticVM = vm;

        mProxyClass = env->FindClass("ru/ytkab0bp/beamklipper/serial/SerialProxy");
        mProxyOnDataReceivedMethod = env->GetMethodID(mProxyClass, "onDataReceived", "([B)V");

        return JNI_VERSION_1_6;
    }

    JNIEXPORT jlong JNICALL Java_ru_ytkab0bp_beamklipper_serial_SerialNative_create(JNIEnv *env, jclass, jstring file, jobject proxy) {
        SerialPort* port = new SerialPort();
        port->file = file;

        const char* chars = env->GetStringUTFChars(file, JNI_FALSE);
        WorkerArgs* args = new WorkerArgs();
        args->file = (char*) chars;

        char name[256];
        unlink(args->file);
        if (openpty(&args->master, &args->slave, name, &args->tio, nullptr) < 0) {
            delete port;
            return 0;
        }

        // set_non_blocking
        int flags = fcntl(args->master, F_GETFL);
        fcntl(args->master, flags | O_NONBLOCK);

        // set_close_on_exec
        fcntl(args->master, F_SETFD, FD_CLOEXEC);
        fcntl(args->slave, F_SETFD, FD_CLOEXEC);

        symlink(name, args->file);

        port->master = args->master;
        port->slave = args->slave;

        args->file = (char*) chars;
        args->proxy = env->NewWeakGlobalRef(proxy);
        pthread_create(&port->worker_thread, nullptr, worker, args);
        env->ReleaseStringUTFChars(file, chars);

        return (jlong) (intptr_t) port;
    }

    JNIEXPORT void JNICALL Java_ru_ytkab0bp_beamklipper_serial_SerialNative_write(JNIEnv *env, jclass, jlong pointer, jbyteArray arr, jint len) {
        SerialPort* port = (SerialPort*) (intptr_t) pointer;
        jbyte* elements = env->GetByteArrayElements(arr, JNI_FALSE);
        pthread_mutex_lock(&mutex);
        tcflush(port->master, TCOFLUSH);
        write(port->master, elements, len);
        pthread_mutex_unlock(&mutex);
        env->ReleaseByteArrayElements(arr, elements, JNI_ABORT);
    }

    JNIEXPORT void JNICALL Java_ru_ytkab0bp_beamklipper_serial_SerialNative_release(JNIEnv *env, jclass, jlong pointer) {
        SerialPort* port = (SerialPort*) (intptr_t) pointer;
        pthread_kill(port->worker_thread, SIGQUIT);
        close(port->master);
        close(port->slave);
        delete port;
    }
}