# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log )

add_library(serial
        SHARED
        src/main/jni/pty/openpty.c
        src/main/jni/serial.cpp
)

add_library(klippy_chelper
        SHARED
        src/main/klipper/klippy/chelper/itersolve.c
        src/main/klipper/klippy/chelper/kin_cartesian.c
        src/main/klipper/klippy/chelper/kin_corexy.c
        src/main/klipper/klippy/chelper/kin_corexz.c
        src/main/klipper/klippy/chelper/kin_delta.c
        src/main/klipper/klippy/chelper/kin_deltesian.c
        src/main/klipper/klippy/chelper/kin_extruder.c
        src/main/klipper/klippy/chelper/kin_polar.c
        src/main/klipper/klippy/chelper/kin_rotary_delta.c
        src/main/klipper/klippy/chelper/kin_shaper.c
        src/main/klipper/klippy/chelper/kin_winch.c
        src/main/klipper/klippy/chelper/msgblock.c
        src/main/klipper/klippy/chelper/pollreactor.c
        src/main/klipper/klippy/chelper/pyhelper.c
        src/main/klipper/klippy/chelper/serialqueue.c
        src/main/klipper/klippy/chelper/stepcompress.c
        src/main/klipper/klippy/chelper/trapq.c
        src/main/klipper/klippy/chelper/trdispatch.c
)

target_link_libraries(
        serial
        ${log-lib})