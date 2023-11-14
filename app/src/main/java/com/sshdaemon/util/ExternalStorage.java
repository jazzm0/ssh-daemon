package com.sshdaemon.util;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ExternalStorage {

    public static final String SD_CARD = "sdCard";
    public static final String EXTERNAL_SD_CARD = "externalSdCard";

    public static void createDirIfNotExists(String path) {
        var file = new File(path);
        if (!file.exists()) file.mkdirs();
    }

    /**
     * @return True if the external storage is available. False otherwise.
     */
    public static boolean isAvailable() {
        var state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getPath() + "/";
    }

    /**
     * @return True if the external storage is writable. False otherwise.
     */
    public static boolean isWritable() {
        var state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);

    }

    /**
     * @return A map of all storage locations available
     */
    public static Map<String, File> getAllStorageLocations() {
        var map = new HashMap<String, File>(10);

        var mMounts = new ArrayList<String>(10);
        var mVold = new ArrayList<>(10);
        mMounts.add("/mnt/sdcard");
        mVold.add("/mnt/sdcard");

        try {
            var mountFile = new File("/proc/mounts");
            if (mountFile.exists()) {
                var scanner = new Scanner(mountFile);
                while (scanner.hasNext()) {
                    var line = scanner.nextLine();
                    if (line.startsWith("/dev/block/vold/")) {
                        var lineElements = line.split(" ");
                        var element = lineElements[1];

                        // don't add the default mount path
                        // it's already in the list.
                        if (!element.equals("/mnt/sdcard"))
                            mMounts.add(element);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            File voldFile = new File("/system/etc/vold.fstab");
            if (voldFile.exists()) {
                var scanner = new Scanner(voldFile);
                while (scanner.hasNext()) {
                    var line = scanner.nextLine();
                    if (line.startsWith("dev_mount")) {
                        var lineElements = line.split(" ");
                        var element = lineElements[2];

                        if (element.contains(":"))
                            element = element.substring(0, element.indexOf(":"));
                        if (!element.equals("/mnt/sdcard"))
                            mVold.add(element);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        for (int i = 0; i < mMounts.size(); i++) {
            var mount = mMounts.get(i);
            if (!mVold.contains(mount))
                mMounts.remove(i--);
        }
        mVold.clear();

        var mountHash = new ArrayList<>(10);

        for (String mount : mMounts) {
            File root = new File(mount);
            if (root.exists() && root.isDirectory() && root.canWrite()) {
                File[] list = root.listFiles();
                StringBuilder hash = new StringBuilder("[");
                if (list != null) {
                    for (File f : list) {
                        hash.append(f.getName().hashCode()).append(":").append(f.length()).append(", ");
                    }
                }
                hash.append("]");
                if (!mountHash.contains(hash.toString())) {
                    String key = SD_CARD + "_" + map.size();
                    if (map.size() == 0) {
                        key = SD_CARD;
                    } else if (map.size() == 1) {
                        key = EXTERNAL_SD_CARD;
                    }
                    mountHash.add(hash.toString());
                    map.put(key, root);
                }
            }
        }

        mMounts.clear();

        if (map.isEmpty()) {
            map.put(SD_CARD, Environment.getExternalStorageDirectory());
        }
        return map;
    }
}
