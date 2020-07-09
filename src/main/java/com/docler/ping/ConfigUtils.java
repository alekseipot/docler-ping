package com.docler.ping;

import java.io.InputStream;
import java.util.Properties;

public class ConfigUtils {

    private static final String CONFIG_FILE = "app.properties";
    private final static Properties prop = new Properties();

    static {
        try (InputStream input = ConfigUtils.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            prop.load(input);
        } catch (Exception ex) {
            throw new RuntimeException("Can not read properties from " + CONFIG_FILE);
        }
    }

    static Integer getIcpmDelay(){
        return readIntegerFromConfig("ICPM_DELAY");
    }

    static Integer getTcpDelay(){
        return readIntegerFromConfig("TCP_DELAY");
    }

    static Integer getTcpSocketTimeOut(){
        return readIntegerFromConfig("TCP_SOCKET_TIME_OUT");
    }

    static Integer getTcpConnectionTimeOut(){
        return readIntegerFromConfig("TCP_CONNECTION_TIME_OUT");
    }

    static Integer getTcpConnectionRequestTimeOut(){
        return readIntegerFromConfig("TCP_CONNECTION_REQUET_TIME_OUT");
    }

    static Integer getTraceDelay(){
        return readIntegerFromConfig("TRACE_DELAY");
    }

    static String getIcpmPingCommand(){
        return readStringFromConfig("ICPM_PING_COMMAND");
    }

    static String getTraceCommand(){
        return readStringFromConfig("TRACE_COMMAND");
    }

    private static Integer readIntegerFromConfig(String propName) {
        return Integer.valueOf(prop.getProperty(propName));
    }

    private static String readStringFromConfig(String propName) {
        return prop.getProperty(propName);
    }

}
