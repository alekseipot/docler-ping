package com.docler.ping;

import java.io.InputStream;
import java.util.Properties;

public class ConfigUtils {

    static Integer readIntegerFromConfig(String propName) {
        try (InputStream input = ConfigUtils.class.getClassLoader().getResourceAsStream("app.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return Integer.valueOf(prop.getProperty(propName));
        } catch (Exception ex) {
            throw new RuntimeException("Can not read property with name " + propName);
        }
    }

    static String readStringFromConfig(String propName) {
        try (InputStream input = ConfigUtils.class.getClassLoader().getResourceAsStream("app.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty(propName);
        } catch (Exception ex) {
            throw new RuntimeException("Can not read property with name " + propName);
        }
    }

}
