package com.tdvorak.nothingmodes.shizuku;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;

interface IPrivilegedShellService {
    Bundle execute(in String[] command, long timeoutMillis, int maxOutputBytes);
    Bundle executeToFile(in String[] command, in ParcelFileDescriptor stdoutDestination, long timeoutMillis, int maxOutputBytes);
    // Runs in the shell (uid 2000) process; toggles tethered hotspot via WifiManager
    // using the device's persisted SoftApConfiguration.
    Bundle setWifiTethered(boolean enabled);
    int uid();
    void destroy();
}
