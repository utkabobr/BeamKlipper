import importlib.util
import sys
import os
import asyncio
from concurrent.futures import ProcessPoolExecutor

def main():
    sys.path.append("moonraker")
    import moonraker.server
    moonraker.server.main()