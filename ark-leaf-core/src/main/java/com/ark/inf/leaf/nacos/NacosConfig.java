package com.ark.inf.leaf.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.utils.StringUtils;
import com.ark.inf.leaf.common.PropertyFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.StringReader;
import java.util.Properties;

@Slf4j
public class NacosConfig {
    private static String DEFAULT_GROUP = "DEFAULT_GROUP";
    private static ConfigService configService = null;
    private static Properties springProperties = null;
    private static String serverAddr;
    private static String profile;

    public static void init() {
        springProperties = new Properties();
        Properties properties = PropertyFactory.getProperties();
        Properties systemProperties = System.getProperties();
        profile = properties.getProperty("spring.profiles.active");
        String nacosAddr = null;

        if (profile != null && profile.indexOf("pro") != -1) {
            //线上是通过jvm参数 -D指定的地址
            nacosAddr = systemProperties.getProperty("spring.cloud.nacos.config.server-addr");
        } else {
            nacosAddr = properties.getProperty("spring.cloud.nacos.config.server-addr");
        }
        if (StringUtils.isEmpty(nacosAddr)) {
            throw new RuntimeException("nacos 地址不存在,请先配置.");
        }
        Properties nacosProperties = new Properties();
        serverAddr = nacosAddr;
        nacosProperties.put("serverAddr", nacosAddr);
        String appFile = "ark-leaf-" + profile + ".properties";
        try {
            configService = NacosFactory.createConfigService(nacosProperties);
            String nacosSpringConfig = getNacosConfig(appFile);
            springProperties.load(new StringReader(nacosSpringConfig));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }


    public static String getSpringConfig(String key) {
        return springProperties.getProperty(key);
    }

    public static String getSpringConfig(String key, String defaultValue) {
        return springProperties.getProperty(key, defaultValue);
    }

    public static String getNacosConfig(String config) {
        return getNacosConfig(config, DEFAULT_GROUP);
    }

    public static String getNacosConfig(String config, String group) {
        try {
            return configService.getConfig(config, group, 3000);
        } catch (NacosException e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public static String getServerAddr() {
        return serverAddr;
    }

    public static String getActiveProfile() {
        return profile;
    }

    public static Boolean isProductEnv(String profile) {
        if ( StringUtils.isBlank(profile)){
            return false;
        }
        if ( "pro".equals(profile) || "prod".equals(profile) ){
            return true;
        }
        return false;
    }

}



