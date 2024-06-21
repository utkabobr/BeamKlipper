import urllib.request
import logging

class BeamCamera:
    def __init__(self, config):
        self.printer = config.get_printer()
        self.gcode = self.printer.lookup_object('gcode')
        self.gcode.register_command(
            'SET_CAMERA_FLASHLIGHT', self.cmd_SET_CAMERA_FLASHLIGHT,
            desc=self.cmd_SET_CAMERA_FLASHLIGHT_help)
        self.gcode.register_command(
            'SET_CAMERA_FOCUS', self.cmd_SET_CAMERA_FOCUS,
            desc=self.cmd_SET_CAMERA_FOCUS_help)

    cmd_SET_CAMERA_FLASHLIGHT_help = 'Sets camera flashlight'
    def cmd_SET_CAMERA_FLASHLIGHT(self, gcmd):
        enabled = gcmd.get('ENABLED')
        try:
            response = urllib.request.urlopen('http://127.0.0.1:8888/beam/set_camera_flashlight?enabled=' + enabled)
            data = response.read()
            response.close()
        except Exception:
            logging.exception('Failed to make a request to set_camera_flashlight')

    cmd_SET_CAMERA_FOCUS_help = 'Sets camera focus'
    def cmd_SET_CAMERA_FOCUS(self, gcmd):
        autofocus = gcmd.get('AUTOFOCUS').lower() == 'true'
        if not autofocus:
            focus_distance = gcmd.get_float('FOCUS_DISTANCE')
        else:
            focus_distance = 0
        try:
            response = urllib.request.urlopen('http://127.0.0.1:8888/beam/set_camera_focus?autofocus=' + str(autofocus).lower() + '&focus=' + str(focus_distance))
            data = response.read()
            response.close()
        except Exception:
            logging.exception('Failed to make a request to set_camera_flashlight')

def load_config(config):
    return BeamCamera(config)