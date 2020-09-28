package com.sshdaemon.util;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AndroidLogger {

    private static final Logger logger = LoggerFactory.getLogger(AndroidLogger.class);

    public static Logger getLogger() {
        BasicConfigurator.configure();
        return logger;
    }
}

