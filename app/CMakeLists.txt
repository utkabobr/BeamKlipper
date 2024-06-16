# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

project(BeamKlipper)

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

add_library(beam_host_mcu
        SHARED
        src/main/jni/pty/openpty.c

        src/main/jni/host_mcu/host_mcu.cpp
        src/main/jni/host_mcu/console.c
        src/main/jni/host_mcu/sched.c
        src/main/jni/host_mcu/gpio.c

        src/main/jni/host_mcu/compile_time_request.c
        src/main/jni/host_mcu/klipper_src/command.c
        src/main/jni/host_mcu/klipper_src/basecmd.c
        src/main/jni/host_mcu/klipper_src/debugcmds.c
        src/main/jni/host_mcu/klipper_src/initial_pins.c
        src/main/jni/host_mcu/klipper_src/gpiocmds.c
        src/main/jni/host_mcu/klipper_src/stepper.c
        src/main/jni/host_mcu/klipper_src/endstop.c
        src/main/jni/host_mcu/klipper_src/trsync.c
        src/main/jni/host_mcu/klipper_src/adccmds.c
        src/main/jni/host_mcu/klipper_src/spicmds.c
        src/main/jni/host_mcu/klipper_src/i2ccmds.c
        src/main/jni/host_mcu/klipper_src/pwmcmds.c
        src/main/jni/host_mcu/klipper_src/buttons.c
        src/main/jni/host_mcu/klipper_src/tmcuart.c
        src/main/jni/host_mcu/klipper_src/neopixel.c
        src/main/jni/host_mcu/klipper_src/pulse_counter.c
        src/main/jni/host_mcu/klipper_src/lcd_st7920.c
        src/main/jni/host_mcu/klipper_src/lcd_hd44780.c
        src/main/jni/host_mcu/klipper_src/spi_software.c
        src/main/jni/host_mcu/klipper_src/i2c_software.c
        src/main/jni/host_mcu/klipper_src/thermocouple.c
        src/main/jni/host_mcu/klipper_src/sensor_adxl345.c
        src/main/jni/host_mcu/klipper_src/sensor_angle.c
        src/main/jni/host_mcu/klipper_src/sensor_mpu9250.c
        src/main/jni/host_mcu/klipper_src/sensor_lis2dw.c
        src/main/jni/host_mcu/klipper_src/sensor_ldc1612.c
        src/main/jni/host_mcu/klipper_src/sensor_bulk.c
        src/main/jni/host_mcu/board/timer.c
        src/main/jni/host_mcu/board/watchdog.c
        src/main/jni/host_mcu/board/pca9685.c
#        src/main/jni/host_mcu/board/spidev.c
#        src/main/jni/host_mcu/board/analog.c
#        src/main/jni/host_mcu/board/hard_pwm.c
        src/main/jni/host_mcu/board/i2c.c
        src/main/jni/host_mcu/board/crc16_ccitt.c
        src/main/jni/host_mcu/board/alloc.c
        src/main/jni/host_mcu/board/sensor_ds18b20.c
)
target_include_directories(beam_host_mcu
        PRIVATE
        src/main/jni/host_mcu
        src/main/jni/host_mcu/klipper_src
        src/main/jni/host_mcu/board
)
target_link_libraries(
        beam_host_mcu
        ${log-lib})

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