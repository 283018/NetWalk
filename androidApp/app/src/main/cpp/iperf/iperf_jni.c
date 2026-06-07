#include <android/log.h>
#include <jni.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "iperf.h"
#include "iperf_api.h"

// ─────────────────────────────────────────────────────────────────────────────
// Logging macros
// ─────────────────────────────────────────────────────────────────────────────
#define TAG "iperfJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ─────────────────────────────────────────────────────────────────────────────
// Global state
// ─────────────────────────────────────────────────────────────────────────────
static struct iperf_test *global_test = NULL;
static pthread_t reader_thread;
static volatile bool stop_requested = false;

// ─────────────────────────────────────────────────────────────────────────────
// Structs
// ─────────────────────────────────────────────────────────────────────────────
/**
 * CallbackArgs - Structure to pass data into the output-reading thread.
 */
struct CallbackArgs {
    JavaVM *jvm;
    jobject callback_global;
    int pipe_fd;
};

// ─────────────────────────────────────────────────────────────────────────────
// Output reader thread
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Reads output from iperf pipe and forwards each line to the Java callback.
 */
void *readerThreadFunc(void *args_ptr) {
    struct CallbackArgs *args = (struct CallbackArgs *)args_ptr;
    JNIEnv *env;
    LOGI("[JNI_TRACK] readerThreadFunc spawned. Attaching current thread to "
         "JVM.");
    (*args->jvm)->AttachCurrentThread(args->jvm, &env, NULL);
    LOGI("[JNI_TRACK] Thread successfully attached to JVM.");

    jclass callbackClass = (*env)->GetObjectClass(env, args->callback_global);
    jmethodID onOutput = (*env)->GetMethodID(env, callbackClass, "onOutput",
                                             "(Ljava/lang/String;)V");

    char buffer[1024];
    FILE *fp = fdopen(args->pipe_fd, "r");

    LOGI("[JNI_TRACK] Starting pipe reading loop...");
    while (fgets(buffer, sizeof(buffer), fp)) {
        jstring line = (*env)->NewStringUTF(env, buffer);
        (*env)->CallVoidMethod(env, args->callback_global, onOutput, line);
        (*env)->DeleteLocalRef(env, line);
    }
    LOGI("[JNI_TRACK] Pipe reading loop exited (EOF reached or pipe broken).");

    fclose(fp);
    (*env)->DeleteGlobalRef(env, args->callback_global);
    (*args->jvm)->DetachCurrentThread(args->jvm);
    free(args);
    LOGI("[JNI_TRACK] Reader thread detached and fully closed.");
    return NULL;
}

// ─────────────────────────────────────────────────────────────────────────────
// Graceful stop method (JNI call from Java)
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Gracefully requests iperf to stop, used by the stop button in the UI.
 */
JNIEXPORT void JNICALL
Java_edu_pwr_zpi_netwalk_iperf_IperfRunner_forceStopIperfTest(
    JNIEnv *env, jobject thiz, jobject callback) {
    LOGI("[JNI_TRACK] forceStopIperfTest called from Kotlin.");
    stop_requested = true;

    jclass callbackClass = (*env)->GetObjectClass(env, callback);
    jmethodID onOutput = (*env)->GetMethodID(env, callbackClass, "onOutput",
                                             "(Ljava/lang/String;)V");

    jstring statusMsg = (*env)->NewStringUTF(
        env, "[iPerf JNI] Requested graceful stop of iPerf test.");
    (*env)->CallVoidMethod(env, callback, onOutput, statusMsg);
    (*env)->DeleteLocalRef(env, statusMsg);

    if (global_test && !global_test->done) {
        global_test->done = 1;
        iperf_set_send_state(global_test, IPERF_DONE);
        shutdown(global_test->ctrl_sck, SHUT_RDWR); // Unblocks select()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main iperf run method (JNI call from Java)
// ─────────────────────────────────────────────────────────────────────────────
/**
 * Starts and runs an iperf3 client session using given arguments.
 * Sends output and status updates via the provided callback.
 */
JNIEXPORT void JNICALL Java_edu_pwr_zpi_netwalk_iperf_IperfRunner_runIperfLive(
    JNIEnv *env, jobject thiz, jobjectArray arguments, jobject callback) {
    LOGI("[JNI_TRACK] Entered runIperfLive JNI method");

    jclass callbackClass = (*env)->GetObjectClass(env, callback);
    jmethodID onOutput = (*env)->GetMethodID(env, callbackClass, "onOutput",
                                             "(Ljava/lang/String;)V");
    jmethodID onError = (*env)->GetMethodID(env, callbackClass, "onError",
                                            "(Ljava/lang/String;)V");
    jmethodID onComplete =
        (*env)->GetMethodID(env, callbackClass, "onComplete", "()V");

    // ───── Convert Java String[] to native char* argv[] ─────
    int argc = (*env)->GetArrayLength(env, arguments);
    if (argc > 64)
        argc = 64;

    LOGI("[JNI_TRACK] Parsing Kotlin arguments array. Size: %d", argc);
    char *argv[64];
    for (int i = 0; i < argc; i++) {
        jstring arg = (jstring)(*env)->GetObjectArrayElement(env, arguments, i);
        const char *arg_str = (*env)->GetStringUTFChars(env, arg, 0);
        argv[i] = strdup(arg_str);
        (*env)->ReleaseStringUTFChars(env, arg, arg_str);
    }

    // ───── Create and initialize iperf test ─────
    LOGI("[JNI_TRACK] Allocating iperf_new_test...");
    global_test = iperf_new_test();
    if (!global_test) {
        LOGE("[JNI_TRACK] CRITICAL: Failed to create iperf test instance.");
        jstring errMsg =
            (*env)->NewStringUTF(env, "Failed to create iperf test");
        (*env)->CallVoidMethod(env, callback, onError, errMsg);
        (*env)->DeleteLocalRef(env, errMsg);
        return;
    }
    iperf_defaults(global_test);

    // ───── Setup pipe to capture iperf output ─────
    LOGI("[JNI_TRACK] Setting up internal POSIX output pipes...");
    int pipefd[2];
    if (pipe(pipefd) < 0) {
        jstring errMsg =
            (*env)->NewStringUTF(env, "Failed to create output pipe");
        (*env)->CallVoidMethod(env, callback, onError, errMsg);
        (*env)->DeleteLocalRef(env, errMsg);
        iperf_free_test(global_test);
        return;
    }

    FILE *fp = fdopen(pipefd[1], "w");
    setvbuf(fp, NULL, _IOLBF, 0);
    global_test->outfile = fp;

    // ───── Parse iperf arguments ─────
    LOGI("[JNI_TRACK] Passing argv map to iperf_parse_arguments...");
    if (iperf_parse_arguments(global_test, argc, argv) < 0) {
        LOGE("[JNI_TRACK] Parse Error: %s", iperf_strerror(i_errno));
        fflush(fp);
        fclose(fp);

        jstring errMsg = (*env)->NewStringUTF(env, iperf_strerror(i_errno));
        (*env)->CallVoidMethod(env, callback, onError, errMsg);
        (*env)->DeleteLocalRef(env, errMsg);
        iperf_free_test(global_test);
        return;
    }

    // ───── Start reader thread ─────
    LOGI("[JNI_TRACK] Creating background pthread for output handling...");
    struct CallbackArgs *cb_args = malloc(sizeof(struct CallbackArgs));
    (*env)->GetJavaVM(env, &cb_args->jvm);
    cb_args->callback_global = (*env)->NewGlobalRef(env, callback);
    cb_args->pipe_fd = pipefd[0];
    int thread_status =
        pthread_create(&reader_thread, NULL, readerThreadFunc, cb_args);
    LOGI("[JNI_TRACK] pthread_create code returned status: %d", thread_status);

    // ───── Notify start ─────
    // REMOVED MESSAGE
    // jstring initMsg =
    //     (*env)->NewStringUTF(env, "Initiating iPerf3 client request...\n");
    // (*env)->CallVoidMethod(env, callback, onOutput, initMsg);
    // (*env)->DeleteLocalRef(env, initMsg);

    // ───── Run the test ─────
    LOGI("[JNI_TRACK] !!! Launching blocking iperf_run_client loop !!!");
    int result = iperf_run_client(global_test);
    LOGI("[JNI_TRACK] iperf_run_client returned execution to JNI with result "
         "flag: %d",
         result);
    if (result < 0 && global_test) {
        LOGE("[JNI_TRACK] Core execution failure code: %s",
             iperf_strerror(i_errno));
        jstring errMsg = (*env)->NewStringUTF(env, iperf_strerror(i_errno));
        (*env)->CallVoidMethod(env, callback, onError, errMsg);
        (*env)->DeleteLocalRef(env, errMsg);
    }

    // ───── Cleanup ─────
    LOGI("[JNI_TRACK] Commencing resource cleanup...");
    fflush(fp);
    fclose(fp);

    if (global_test) {
        iperf_free_test(global_test);
        global_test = NULL;
    }

    LOGI("[JNI_TRACK] Waiting for reader thread to join...");
    pthread_join(reader_thread, NULL);
    LOGI("[JNI_TRACK] Reader thread joined successfully.");

    for (int i = 0; i < argc; i++) {
        free(argv[i]);
    }

    // REMOVED END STRING
    // jstring finalMsg;
    // if (stop_requested) {
    //     finalMsg =
    //         (*env)->NewStringUTF(env, "[iPerf JNI] Test was stopped by
    //         user.");
    // } else if (result < 0) {
    //     finalMsg = (*env)->NewStringUTF(
    //         env, "[iPerf JNI] Test failed to complete successfully.");
    // } else {
    //     finalMsg = (*env)->NewStringUTF(
    //         env, "[iPerf JNI] Test completed successfully.");
    // }
    // (*env)->CallVoidMethod(env, callback, onOutput, finalMsg);
    // (*env)->DeleteLocalRef(env, finalMsg);

    // ───── Final cleanup ─────
    stop_requested = false;
    reader_thread = 0;

    // ───── Notify completion to Java ─────
    LOGI("[JNI_TRACK] Invoking onComplete Kotlin callback hook. Execution "
         "complete.");
    (*env)->CallVoidMethod(env, callback, onComplete);
}
