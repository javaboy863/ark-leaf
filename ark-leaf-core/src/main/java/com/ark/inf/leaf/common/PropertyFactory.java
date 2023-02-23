package com.ark.inf.leaf.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class PropertyFactory {
    private static final Logger logger = LoggerFactory.getLogger(PropertyFactory.class);
    private static final Properties prop = new Properties();
    static {
        try {
            String appFile = "config/application.properties";
            logger.info("load application file :{}",appFile);
            prop.load(PropertyFactory.class.getClassLoader().getResourceAsStream(appFile));
        } catch (IOException e) {
            logger.warn("Load Properties Ex", e);
        }
    }
    public static Properties getProperties() {
        return prop;
    }
}
