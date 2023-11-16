package com.sshdaemon.util;


import static java.util.Objects.isNull;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public class ExternalStorage {

    public static void createDirIfNotExists(String path) {
        var file = new File(path);
        if (!file.exists()) file.mkdirs();
    }

    public static String getRootPath() {
        return isNull(Environment.getExternalStorageDirectory()) ? "/" : Environment.getExternalStorageDirectory().getPath() + "/";
    }

    public static boolean hasMultipleStorageLocations(Context context) {
        var directories = Arrays.stream(context.getExternalFilesDirs(null)).filter(d -> !isNull(d)).toArray();
        return !isNull(directories) && directories.length > 1;
    }

    public static List<String> getAllStorageLocations(Context context) {
        var locations = new LinkedHashSet<String>();
        final var directories = context.getExternalFilesDirs(null);
        if (directories.length == 0) {
            return List.of("/");
        }
        final var appSuffixLength = directories[0].getPath().replace(getRootPath(), " ").length();
        for (var directory : directories) {
            if (!isNull(directory)) {
                var path = directory.getPath();
                locations.add(path.substring(0, path.length() - appSuffixLength + 1));
            }
        }
        return new ArrayList<>(locations);
    }
}
