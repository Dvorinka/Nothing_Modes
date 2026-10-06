package com.tdvorak.nothingmodes.shizuku;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;

interface IPrivilegedShellService {
    Bundle execute(in String[] command, long timeoutMillis, int maxOutputBytes);
    Bundle executeToFile(in String[] command, in ParcelFileDescriptor stdoutDestination, long timeoutMillis, int maxOutputBytes);
    // Runs in the shell (uid 2000) process; toggles tethered hotspot via WifiManager
    // using the device's persisted SoftApConfiguration.
    Bundle setWifiTethered(boolean enabled);
    // Master auto-sync toggle. The `auto_sync` settings key is dead — the real flag
    // lives in SyncStorageEngine behind WRITE_SYNC_SETTINGS, which shell holds.
    Bundle setMasterSyncAutomatically(boolean enabled);
    // Pages a bonded device's classic profiles. Hidden BluetoothDevice.connect()
    // needs BLUETOOTH_PRIVILEGED, which shell holds and the app does not.
    Bundle connectBluetoothDevice(String address);
    int uid();
    void destroy();
}
