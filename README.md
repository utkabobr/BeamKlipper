# Beam Klipper - Klipper for vanilla Android!

Beam Klipper allows you to run [Klipper](https://github.com/KevinOConnor/klipper) host software (Klippy) on any Android 5.0+ device with OTG support.

Telegram: https://t.me/ytkab0bp_channel

Boosty (Patreon alternative): https://boosty.to/ytkab0bp

K3D Chat for discussion & support (Russian language only): https://t.me/K_3_D

# Quick start

1. Install APK from Releases tab
2. Allow all the permissions required
3. Add printer instance (Click generic-***.cfg if your printer is not available)
4. Click start
5. Go to web server's url `http://IP:8888/`
6. Configure serial port from "Devices" tab in web editor
7. You're awesome!

# Can I use device as regular after I install Beam Klipper to it?

**Yes!** You definetly can!

Beam Klipper does not do **anything** to your Android system, it runs in user-space as a regular Android app

# What's IP:port?

It's displayed on main page when any of the instances are running.

Web server URL is: `http://IP:8888/`

Camera URL's are:
- /webcam/?action=stream => `http://IP:8889/`
- /webcam/?action=snapshot => `http://IP:8889/snapshot`

Recommended camera config is mjpeg-**stream** (Not adaptive mjpeg) for Fluidd and UV4L-MJPEG for Mainsail

# What's inside?

Beam Klipper bundles:
- [Klipper](https://github.com/KevinOConnor/klipper)
- [Moonraker](https://github.com/Arksine/moonraker)
- [Fluidd](https://github.com/fluidd-core/fluidd)
- [Happy Hare](https://github.com/moggieuk/Happy-Hare)
- [Mainsail](https://github.com/mainsail-crew/mainsail)

# Autostart

You can put the app to autostart by setting needed printers to autostart **AND** setting app as default launcher.

You **must** remove lockscreen pincode if your device is encrypted (Enabled by default on most devices)

# Background activity notice

Some manufacturers may restrict app's performance or background process.
You can circumvent this by setting app as default launcher and allowing all the background tasks

# Android TV support?

It *should* be supported, but it's not tested yet. There might be interface bugs

# What USB hub to use?

I'm using UGREEN Type-c hub (Not affiliated, but I'm waiting for your request UGREEN :D), but any should be fine if it works with your device and provides charging at the same time

# Restrictions

- Web server can't run on default port because Android/linux doesn't allow user-space apps to bind to ports less than 1024 and we want 80 for default `http://IP`
- Only up to 4 instances can be running at the same time because Android requires developer to declare each service with different process individually. Idk if someone will use more than that anyway ¯\\\_(ツ)\_/¯
- Some devices may reset device path on firmware restart, you should use VID/PID naming in that case
- No SSH (You won't be able to build firmware or run additional autorun services anyway)
- Some devices doesn't support OTG and charging at the same time, you must solder directly to the battery pins in that case (Or use different device, it's up to you)
- Only 250000 baud rate is supported (I don't want to forward this setting into Android USB driver, almost all configurations use 250000 anyway)

# Building

- Fetch all of the submodules first! (`git clone --recursive`, do NOT download project as archive)
- Import project into Android Studio & click run

# Contributing

Pull requests are welcome, but I will **NOT** approve Kotlin source code as I don't use it in my projects