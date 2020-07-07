package com.docler.ping;

import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.docler.ping.ConfigUtils.readIntegerFromConfig;
import static com.docler.ping.ConfigUtils.readStringFromConfig;

public class Application {

    private static Map<String, OperationResult> icmpPingLastResults = new ConcurrentHashMap<>();
    private static Map<String, OperationResult> tcpPingLastResults = new ConcurrentHashMap<>();
    private static Map<String, OperationResult> traceLastResults = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Looks like it works");
//        doIcmpPing(List.of("jasmin.com", "www.oranum.com"));
        doTrace(List.of("jasmin.com", "www.oranum.com"));
    }

    public static void doIcmpPing(List<String> hosts) {
        ScheduledExecutorService scheduler = Executors
                .newScheduledThreadPool(hosts.size());
        Integer delay = readIntegerFromConfig("ICPM_DELAY");
        hosts.parallelStream().forEach(
                host -> scheduler.scheduleAtFixedRate(() -> {
                    LocalDateTime time = LocalDateTime.now();
                    List<String> result = new ArrayList<>();
                    List<String> command = buildPingCommand(host);
                    ProcessBuilder processBuilder = new ProcessBuilder(command);
                    try (BufferedReader standardOutput = new BufferedReader(new InputStreamReader(processBuilder.start().getInputStream()))) {
                        String outputLine;
                        while ((outputLine = standardOutput.readLine()) != null) {
                            result.add(outputLine);
                            if (outputLine.toLowerCase().contains("destination host unreachable")) {
                                sendReport(host);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendReport(host);
                    }
                    icmpPingLastResults.put(host, new OperationResult(result, time));
                }, 0, delay, TimeUnit.SECONDS)

        );
    }

    public static void doTrace(List<String> hosts) {
        ScheduledExecutorService scheduler = Executors
                .newScheduledThreadPool(hosts.size());
        Integer delay = readIntegerFromConfig("TRACE_DELAY");
        hosts.parallelStream().forEach(
                host -> scheduler.scheduleAtFixedRate(() -> {
                    LocalDateTime time = LocalDateTime.now();
                    List<String> result = new ArrayList<>();
                    List<String> command = buildTraceCommand(host);
                    ProcessBuilder processBuilder = new ProcessBuilder(command);
                    try (BufferedReader standardOutput = new BufferedReader(new InputStreamReader(processBuilder.start().getInputStream()))) {
                        String outputLine;
                        while ((outputLine = standardOutput.readLine()) != null) {
                            result.add(outputLine);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    traceLastResults.put(host, new OperationResult(result, time));
                }, 0, delay, TimeUnit.SECONDS)

        );
    }

    public static void sendReport(String host) {
        try {
            ReportRequest reportRequest = new ReportRequest();
            reportRequest.setHost(host);
            OperationResult icmpPing = icmpPingLastResults.get(host);
            if (icmpPing != null) {
                reportRequest.setIcmpPing(String.join(" ", icmpPing.getResults()));
            }
            OperationResult tcpPing = tcpPingLastResults.get(host);
            if (tcpPing != null) {
                reportRequest.setTcpPing(String.join(" ", tcpPing.getResults()));
            }
            OperationResult trace = traceLastResults.get(host);
            if (trace != null) {
                reportRequest.setTrace(String.join(" ", trace.getResults()));
            }
            ReportSender.sendReport(reportRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> buildPingCommand(String ipAddress) {
        List<String> command = new ArrayList<>();
        command.add(readStringFromConfig("ICPM_PING_COMMAND"));

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

    private static List<String> buildTraceCommand(String ipAddress) {
        List<String> command = new ArrayList<>();
        command.add(readStringFromConfig("TRACE_COMMAND"));
        command.add(ipAddress);
        return command;
    }

}
