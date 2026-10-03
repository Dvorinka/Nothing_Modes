@file:Suppress(
    "ktlint:standard:class-signature",
    "ktlint:standard:multiline-expression-wrapping",
    "ktlint:standard:if-else-wrapping",
    "ktlint:standard:multiline-if-else",
    "ktlint:standard:parameter-list-wrapping",
    "ktlint:standard:condition-wrapping",
    "ktlint:standard:function-signature",
)

package com.tdvorak.nothingmodes.shizuku

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.annotation.Keep
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import org.lsposed.hiddenapibypass.HiddenApiBypass

@Keep
class PrivilegedShellUserService() : IPrivilegedShellService.Stub() {
    @Volatile private var serviceContext: Context? = null
    private val hiddenApiBypassed = AtomicBoolean(false)

    @Keep
    constructor(context: Context) : this() {
        serviceContext = context
    }

    override fun execute(
        command: Array<out String>?,
        timeoutMillis: Long,
        maxOutputBytes: Int,
    ): Bundle {
        val stdout = ByteArrayOutputStream()
        return runCommand(command, timeoutMillis, maxOutputBytes, stdout, includeStdout = true)
    }

    override fun executeToFile(
        command: Array<out String>?,
        stdoutDestination: ParcelFileDescriptor?,
        timeoutMillis: Long,
        maxOutputBytes: Int,
    ): Bundle {
        if (stdoutDestination == null) return errorBundle("destination_missing")
        return ParcelFileDescriptor.AutoCloseOutputStream(stdoutDestination).use { output ->
            runCommand(command, timeoutMillis, maxOutputBytes, output, includeStdout = false)
        }
    }

    @Synchronized
    private fun runCommand(
        rawCommand: Array<out String>?,
        timeoutMillis: Long,
        maxOutputBytes: Int,
        stdoutDestination: OutputStream,
        includeStdout: Boolean,
    ): Bundle {
        val command = rawCommand?.toList().orEmpty()
        if (command.isEmpty() || command.size > MAX_ARGUMENTS ||
            command.sumOf { it.length } > MAX_COMMAND_CHARS
        ) return errorBundle("command_invalid")
        val outputLimit = if (includeStdout) {
            PrivilegedShell.DEFAULT_TEXT_OUTPUT_BYTES
        } else {
            PrivilegedShell.DEFAULT_FILE_OUTPUT_BYTES
        }
        if (timeoutMillis !in 1..PrivilegedShell.MAX_TIMEOUT_MILLIS ||
            maxOutputBytes !in 1..outputLimit
        ) return errorBundle("limits_invalid")

        val stderr = ByteArrayOutputStream()
        val stdoutTruncated = AtomicBoolean(false)
        val stderrTruncated = AtomicBoolean(false)
        val process = try {
            ProcessBuilder(command)
                .directory(java.io.File("/"))
                .start()
        } catch (_: Exception) {
            return errorBundle("start_failed")
        }
        runCatching { process.outputStream.close() }

        val stdoutThread = drain(
            process.inputStream,
            stdoutDestination,
            maxOutputBytes,
            stdoutTruncated,
            "argus-shell-stdout",
        )
        val stderrThread = drain(
            process.errorStream,
            stderr,
            minOf(maxOutputBytes, MAX_STDERR_BYTES),
            stderrTruncated,
            "argus-shell-stderr",
        )

        val finished = try {
            process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!finished) {
            process.destroyForcibly()
            runCatching { process.waitFor(2, TimeUnit.SECONDS) }
        }
        stdoutThread.join(JOIN_MILLIS)
        stderrThread.join(JOIN_MILLIS)
        if (stdoutThread.isAlive) runCatching { process.inputStream.close() }
        if (stderrThread.isAlive) runCatching { process.errorStream.close() }
        if (stdoutThread.isAlive) stdoutThread.join(JOIN_MILLIS)
        if (stderrThread.isAlive) stderrThread.join(JOIN_MILLIS)

        return Bundle().apply {
            putInt(KEY_EXIT_CODE, if (finished) process.exitValue() else EXIT_TIMEOUT)
            if (includeStdout && stdoutDestination is ByteArrayOutputStream) {
                putByteArray(KEY_STDOUT, stdoutDestination.toByteArray())
            }
            putByteArray(KEY_STDERR, stderr.toByteArray())
            putBoolean(KEY_TIMED_OUT, !finished)
            putBoolean(KEY_TRUNCATED, stdoutTruncated.get() || stderrTruncated.get())
        }
    }

    private fun drain(
        input: java.io.InputStream,
        output: OutputStream,
        limit: Int,
        truncated: AtomicBoolean,
        name: String,
    ) = thread(start = true, isDaemon = true, name = name) {
        val buffer = ByteArray(8 * 1024)
        var written = 0
        try {
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                val accepted = minOf(count, limit - written)
                if (accepted > 0) {
                    output.write(buffer, 0, accepted)
                    written += accepted
                }
                if (accepted < count) truncated.set(true)
            }
            output.flush()
        } catch (_: Exception) {
            // Chiusura stream durante timeout/kill: lo stato timedOut è già nel risultato.
        }
    }

    /**
     * Toggles the tethered hotspot via WifiManager hidden APIs. This process runs
     * as uid 2000, which holds NETWORK_STACK; calling through a "com.android.shell"
     * package context makes AppOps.checkPackage resolve the shell package instead
     * of ours (which doesn't belong to uid 2000 and would throw SecurityException).
     * A null SoftApConfiguration keeps the user's saved SSID/passphrase.
     */
    override fun setWifiTethered(enabled: Boolean): Bundle {
        val ctx = serviceContext ?: return errorBundle("context_missing")
        if (Build.VERSION.SDK_INT < 30) return errorBundle("unsupported_api")
        return try {
            ensureHiddenApiExemptions()
            val shellContext = ctx.createPackageContext(SHELL_PACKAGE_NAME, 0)
            val wifi = shellContext.getSystemService(Context.WIFI_SERVICE)
                ?: return errorBundle("wifi_service_missing")
            val accepted = if (enabled) {
                wifi.javaClass
                    .getMethod(
                        "startTetheredHotspot",
                        Class.forName("android.net.wifi.SoftApConfiguration"),
                    ).invoke(wifi, *arrayOfNulls<Any?>(1))
            } else {
                wifi.javaClass.getMethod("stopSoftAp").invoke(wifi)
            }
            if (accepted as? Boolean == true) {
                Bundle().apply { putInt(KEY_EXIT_CODE, 0) }
            } else {
                errorBundle("rejected", "tethered hotspot toggle rejected by WifiService")
            }
        } catch (e: InvocationTargetException) {
            val cause = e.cause
            errorBundle(
                "rejected",
                "${cause?.javaClass?.simpleName}: ${cause?.message ?: "wifi service call failed"}",
            )
        } catch (e: Exception) {
            errorBundle("failed", "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * Master auto-sync toggle. Public static API — but it is gated by
     * WRITE_SYNC_SETTINGS (signature-level), so only the shell-uid process can
     * reach it. No package-context trick needed: the check is uid-only.
     */
    override fun setMasterSyncAutomatically(enabled: Boolean): Bundle =
        try {
            ContentResolver.setMasterSyncAutomatically(enabled)
            Bundle().apply { putInt(KEY_EXIT_CODE, 0) }
        } catch (e: Exception) {
            errorBundle("failed", "${e.javaClass.simpleName}: ${e.message}")
        }

    /** "L" exposes every hidden API member in this process; needed on API 30+. */
    private fun ensureHiddenApiExemptions() {
        if (hiddenApiBypassed.compareAndSet(false, true)) {
            runCatching { HiddenApiBypass.addHiddenApiExemptions("L") }
        }
    }

    private fun errorBundle(code: String, detail: String? = null) = Bundle().apply {
        putInt(KEY_EXIT_CODE, EXIT_INTERNAL_ERROR)
        putString(KEY_ERROR_CODE, code)
        if (detail != null) {
            putByteArray(KEY_STDERR, detail.take(MAX_STDERR_BYTES).toByteArray())
        }
    }

    override fun uid(): Int = Process.myUid()

    override fun destroy() {
        kotlin.system.exitProcess(0)
    }

    internal companion object {
        const val KEY_EXIT_CODE = "exit_code"
        const val KEY_STDOUT = "stdout"
        const val KEY_STDERR = "stderr"
        const val KEY_TIMED_OUT = "timed_out"
        const val KEY_TRUNCATED = "truncated"
        const val KEY_ERROR_CODE = "error_code"
        private const val MAX_ARGUMENTS = 128
        private const val MAX_COMMAND_CHARS = 64 * 1024
        private const val MAX_STDERR_BYTES = 64 * 1024
        private const val EXIT_TIMEOUT = -1
        private const val EXIT_INTERNAL_ERROR = -127
        private const val JOIN_MILLIS = 2_000L
        private const val SHELL_PACKAGE_NAME = "com.android.shell"
    }
}
