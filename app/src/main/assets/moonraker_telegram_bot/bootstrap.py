import runpy
import sys
import os

def main():
    script_path = os.path.abspath(__file__)
    parent_dir = os.path.dirname(script_path) or os.path.realpath(os.path.join(os.path.split(script_path)[0], ".."))
    dir = os.path.join(parent_dir, "bot")
    os.chdir(dir)
    sys.path.append(dir)
    result = runpy.run_module("main", run_name="__main__")