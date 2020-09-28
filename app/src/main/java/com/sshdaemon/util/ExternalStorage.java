package com.sshdaemon.util;

import java.io.File;

public class ExternalStorage {

    public static void createDirIfNotExists(String path) {
        File file = new File(path);
        if (!file.exists()) file.mkdirs();
    }
}
