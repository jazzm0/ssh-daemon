package com.sshdaemon.util;


import static java.util.Objects.isNull;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class ExternalStorage {

    public static void createDirIfNotExists(String path) {
        var file = new File(path);
        if (!file.exists()) file.mkdirs();
    }

    public static String getRootPath() {
        return isNull(Environment.getExternalStorageDirectory()) ? "/" : Environment.getExternalStorageDirectory().getPath() + "/";
    }

    public static boolean hasMultipleStorageLocations(Context context) {
        return Arrays
                .stream(context.getExternalFilesDirs(null))
                .filter(d -> !isNull(d))
                .distinct()
                .count() > 1;
    }

    public static List<String> getAllStorageLocations(Context context) {
        var locations = new LinkedHashSet<String>();

        final var directories = Arrays
                .stream(context.getExternalFilesDirs(null))
                .filter(d -> !isNull(d))
                .map(File::getPath)
                .collect(Collectors.toList());

        if (directories.isEmpty()) {
            return List.of("/");
        }

        final var appSuffixLength = directories.get(0).replace(getRootPath(), " ").length();

        for (var directory : directories) {
            locations.add(directory.substring(0, directory.length() - appSuffixLength + 1));
        }
        return new ArrayList<>(locations);
    }
}
