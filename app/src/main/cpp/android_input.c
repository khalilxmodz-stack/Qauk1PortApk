/* Touch -> QuakeSpasm bridge. JNI calls arrive on the Android UI thread; the engine
 * drains everything on its own thread from IN_Move()/IN_Commands() (patched hooks). */
#include <jni.h>
#include <pthread.h>
#include <string.h>
#include <time.h>
#include "quakedef.h"

#define QMAX 128
typedef struct { int is_key; int key; int down; char cmd[96]; } qitem_t;

static qitem_t q[QMAX];
static int qhead, qtail;
static pthread_mutex_t mtx = PTHREAD_MUTEX_INITIALIZER;

static float g_fwd, g_side, g_dx, g_dy;
static volatile int g_ingame;
static int g_hud[3];

#define NFR 240
static float ft[NFR];
static int fn;
static double last_ms;

static double now_ms(void) {
    struct timespec ts; clock_gettime(CLOCK_MONOTONIC, &ts);
    return ts.tv_sec * 1000.0 + ts.tv_nsec / 1e6;
}

static void push_cmd(const char *s) {
    pthread_mutex_lock(&mtx);
    int next = (qhead + 1) % QMAX;
    if (next != qtail) {
        qitem_t *it = &q[qhead]; memset(it, 0, sizeof(*it));
        strncpy(it->cmd, s, sizeof(it->cmd) - 1);
        for (char *c = it->cmd; *c; c++) if (*c == '\n' || *c == '\r') *c = ' ';
        qhead = next;
    }
    pthread_mutex_unlock(&mtx);
}

static void push_key(int key, int down) {
    pthread_mutex_lock(&mtx);
    int next = (qhead + 1) % QMAX;
    if (next != qtail) {
        qitem_t *it = &q[qhead]; memset(it, 0, sizeof(*it));
        it->is_key = 1; it->key = key; it->down = down; qhead = next;
    }
    pthread_mutex_unlock(&mtx);
}

/* ---- engine-thread hooks ---- */
void IN_AndroidMove(usercmd_t *cmd) {
    float f, s, dx, dy;
    pthread_mutex_lock(&mtx);
    f = g_fwd; s = g_side; dx = g_dx; dy = g_dy; g_dx = g_dy = 0;
    pthread_mutex_unlock(&mtx);

    cmd->forwardmove += f * cl_forwardspeed.value * cl_movespeedkey.value;
    cmd->sidemove    += s * cl_sidespeed.value * cl_movespeedkey.value;
    if (dx != 0 || dy != 0) {
        cl.viewangles[YAW]   -= dx;
        cl.viewangles[PITCH] += dy;
        if (dy != 0) V_StopPitchDrift();
        if (cl.viewangles[PITCH] > 80)  cl.viewangles[PITCH] = 80;
        if (cl.viewangles[PITCH] < -70) cl.viewangles[PITCH] = -70;
    }
}

void IN_AndroidCommands(void) {
    double t = now_ms();
    if (last_ms > 0) {
        pthread_mutex_lock(&mtx);
        ft[fn % NFR] = (float)(t - last_ms); fn++;
        pthread_mutex_unlock(&mtx);
    }
    last_ms = t;

    g_ingame = (key_dest == key_game && cls.state == ca_connected);
    if (cls.state == ca_connected) {
        g_hud[0] = cl.stats[STAT_HEALTH];
        g_hud[1] = cl.stats[STAT_ARMOR];
        g_hud[2] = cl.stats[STAT_AMMO];
    }

    for (;;) {
        qitem_t it;
        pthread_mutex_lock(&mtx);
        if (qtail == qhead) { pthread_mutex_unlock(&mtx); break; }
        it = q[qtail]; qtail = (qtail + 1) % QMAX;
        pthread_mutex_unlock(&mtx);
        if (it.is_key) Key_Event(it.key, it.down ? true : false);
        else { Cbuf_AddText(it.cmd); Cbuf_AddText("\n"); }
    }
}

/* ---- JNI (com.khalilxmodz.port.NativeBridge) ---- */
#define J(name) Java_com_khalilxmodz_port_NativeBridge_##name

JNIEXPORT void JNICALL J(nativeSetMove)(JNIEnv *e, jclass c, jfloat f, jfloat s) {
    pthread_mutex_lock(&mtx); g_fwd = f; g_side = s; pthread_mutex_unlock(&mtx);
}
JNIEXPORT void JNICALL J(nativeAddLook)(JNIEnv *e, jclass c, jfloat dx, jfloat dy) {
    pthread_mutex_lock(&mtx); g_dx += dx; g_dy += dy; pthread_mutex_unlock(&mtx);
}
JNIEXPORT void JNICALL J(nativeCommand)(JNIEnv *e, jclass c, jstring js) {
    const char *s = (*e)->GetStringUTFChars(e, js, NULL);
    if (s) { push_cmd(s); (*e)->ReleaseStringUTFChars(e, js, s); }
}
JNIEXPORT void JNICALL J(nativeKey)(JNIEnv *e, jclass c, jint k, jboolean down) {
    static const int map[6] = { K_UPARROW, K_DOWNARROW, K_LEFTARROW, K_RIGHTARROW, K_ENTER, K_ESCAPE };
    if (k >= 0 && k < 6) push_key(map[k], down ? 1 : 0);
}
JNIEXPORT void JNICALL J(nativeReset)(JNIEnv *e, jclass c) {
    pthread_mutex_lock(&mtx); g_fwd = g_side = g_dx = g_dy = 0; pthread_mutex_unlock(&mtx);
    push_cmd("-attack"); push_cmd("-jump"); push_cmd("-movedown");
    push_cmd("-moveup"); push_cmd("-showscores"); push_cmd("-speed");
}
JNIEXPORT jint JNICALL J(nativeInGame)(JNIEnv *e, jclass c) { return g_ingame; }

/* {instant fps, avg fps over ~1s, last frame ms, max frame ms in window, spike count} */
JNIEXPORT jfloatArray JNICALL J(nativeStats)(JNIEnv *e, jclass c) {
    float out[5] = {0, 0, 0, 0, 0};
    pthread_mutex_lock(&mtx);
    int n = fn < NFR ? fn : NFR;
    if (n > 0) {
        float sum = 0, mx = 0; int cnt = 0, i;
        float last = ft[(fn - 1) % NFR];
        for (i = 0; i < n && sum < 1000.0f; i++) {
            float d = ft[(fn - 1 - i + NFR * 2) % NFR];
            sum += d; cnt++; if (d > mx) mx = d;
        }
        float avg = sum / cnt; int spikes = 0;
        for (i = 0; i < cnt; i++) if (ft[(fn - 1 - i + NFR * 2) % NFR] > 2.0f * avg) spikes++;
        out[0] = last > 0 ? 1000.0f / last : 0; out[1] = 1000.0f * cnt / sum;
        out[2] = last; out[3] = mx; out[4] = (float)spikes;
    }
    pthread_mutex_unlock(&mtx);
    jfloatArray a = (*e)->NewFloatArray(e, 5);
    (*e)->SetFloatArrayRegion(e, a, 0, 5, out);
    return a;
}
JNIEXPORT jintArray JNICALL J(nativeHud)(JNIEnv *e, jclass c) {
    jintArray a = (*e)->NewIntArray(e, 3);
    (*e)->SetIntArrayRegion(e, a, 0, 3, (const jint *)g_hud);
    return a;
}
