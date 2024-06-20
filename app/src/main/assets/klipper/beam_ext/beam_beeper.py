import urllib.request
import logging

class BeamBeeper:
    def __init__(self, config):
        self.printer = config.get_printer()
        self.gcode = self.printer.lookup_object('gcode')
        self.gcode.register_command(
            'PLAY_TONE', self.cmd_PLAY_TONE,
            desc=self.cmd_PLAY_TONE_help)

    cmd_PLAY_TONE_help = 'Plays tone'
    def cmd_PLAY_TONE(self, gcmd):
        duration = gcmd.get_int('DURATION')
        frequency = gcmd.get_int('FREQUENCY')
        try:
            response = urllib.request.urlopen('http://127.0.0.1:8888/beam/play_tone?duration=' + str(duration) + '&frequency=' + str(frequency))
            data = response.read()
            response.close()
        except Exception:
            logging.exception('Failed to make a request to play_tone')

def load_config(config):
    return BeamBeeper(config)