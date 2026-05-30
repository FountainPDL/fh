package com.fountainhome.streaming;
import android.content.Context;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class CrashLogger {
    public static void init(Context ctx) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                File f = new File(ctx.getExternalFilesDir(null), "crash_log.txt");
                PrintWriter pw = new PrintWriter(new FileWriter(f, false));
                pw.println("=== CRASH " + new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()) + " ===");
                pw.println(throwable.toString());
                for (StackTraceElement e : throwable.getStackTrace()) pw.println("  at " + e);
                if (throwable.getCause() != null) {
                    pw.println("CAUSED BY: " + throwable.getCause());
                    for (StackTraceElement e : throwable.getCause().getStackTrace()) pw.println("  at " + e);
                }
                pw.flush(); pw.close();
            } catch (Exception ignored) {}
            // Let system handle it so app closes normally
            android.os.Process.killProcess(android.os.Process.myPid());
        });
    }
    public static String read(Context ctx) {
        try {
            File f = new File(ctx.getExternalFilesDir(null), "crash_log.txt");
            if (!f.exists()) return "No crash log yet.";
            BufferedReader br = new BufferedReader(new FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line; while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close(); return sb.toString();
        } catch (Exception e) { return "Error reading log: " + e.getMessage(); }
    }
}
