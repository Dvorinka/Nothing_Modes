package com.tdvorak.nothingmodes.shizuku;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;

interface IPrivilegedShellService {
    Bundle execute(in String[] command, long timeoutMillis, int maxOutputBytes);
    Bundle executeToFile(in String[] command, in ParcelFileDescriptor stdoutDestination, long timeoutMillis, int maxOutputBytes);
    int uid();
    void destroy();
}
