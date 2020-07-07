package com.docler.ping;

import org.apache.commons.lang3.SystemUtils;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Application {

    private static Map<String, OperationResult> icmpPingLastResults = new HashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Looks like it works");
        doIcmpPing(List.of("jasmin.com", "www.oranum.com"));
    }

    public static void doIcmpPing(List<String> hosts) {
        ScheduledExecutorService scheduler = Executors
                .newScheduledThreadPool(hosts.size());
        Integer delay = readIntegerFromConfig("ICPM_DELAY");
        hosts.parallelStream().forEach(
                host -> {
                    scheduler.scheduleAtFixedRate(() -> {
                        LocalDateTime time = LocalDateTime.now();
                        List<String> result = new ArrayList<>();
                        List<String> command = buildPingCommand(host);
                        ProcessBuilder processBuilder = new ProcessBuilder(command);
                        try (BufferedReader standardOutput = new BufferedReader(new InputStreamReader(processBuilder.start().getInputStream()))) {
                            String outputLine;
                            while ((outputLine = standardOutput.readLine()) != null) {
                                result.add(outputLine);
                                if (outputLine.toLowerCase().contains("destination host unreachable")) {
                                    //todo: call report
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            //todo: call report
                        }
                        icmpPingLastResults.put(host, new OperationResult(result, time));
                    }, 0, delay, TimeUnit.SECONDS);
                }

        );
        while (true) {
            System.out.println("icmpPingLastResults: " + icmpPingLastResults);
        }
    }

    private static List<String> buildPingCommand(String ipAddress) {
        List<String> command = new ArrayList<>();
        command.add("ping");

        if (SystemUtils.IS_OS_WINDOWS) {
            command.add("-n");
        } else if (SystemUtils.IS_OS_UNIX) {
            command.add("-c");
        } else {
            throw new UnsupportedOperationException("Unsupported operating system");
        }

        command.add("1");
        command.add(ipAddress);
        return command;
    }

    private static Integer readIntegerFromConfig(String propName) {
        try (InputStream input = Application.class.getClassLoader().getResourceAsStream("app.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return Integer.valueOf(prop.getProperty(propName));
        } catch (Exception ex) {
            throw new RuntimeException("Can not read property with name " + propName);
        }
    }

    private static String readStringFromConfig(String propName) {
        try (InputStream input = new FileInputStream("app.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty(propName);
        } catch (Exception ex) {
            throw new RuntimeException("Can not read property with name " + propName);
        }
    }
}
