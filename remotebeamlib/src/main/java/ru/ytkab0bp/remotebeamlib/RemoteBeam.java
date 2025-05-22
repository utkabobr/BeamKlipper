package ru.ytkab0bp.remotebeamlib;

public class RemoteBeam {
    private static IPlatform platform;

    public static void init(IPlatform platform) {
        RemoteBeam.platform = platform;
    }

    /* package */ static IPlatform getPlatform() {
        if (platform == null) {
            throw new IllegalStateException("You must call RemoteBeam.init(...) first!");
        }
        return platform;
    }
}
