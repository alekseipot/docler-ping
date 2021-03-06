package com.docler.ping;

import com.docler.ping.model.OperationResult;
import com.docler.ping.model.ReportRequest;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.docler.ping.ConfigUtils.*;

public class Application {

    private static final LogManager logManager = LogManager.getLogManager();
    private static final Logger LOGGER = Logger.getLogger("confLogger");

    static {
        try {
            logManager.readConfiguration(ConfigUtils.class.getClassLoader().getResourceAsStream("logger.properties"));
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Error in loading configuration", exception);
        }
    }

    private static final Map<String, OperationResult> icmpPingLastResults = new ConcurrentHashMap<>();
    private static final Map<String, OperationResult> tcpPingLastResults = new ConcurrentHashMap<>();
    private static final Map<String, OperationResult> traceLastResults = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        LOGGER.info("Executing Simple ping app");
        List<String> hosts;
        if (args == null || args.length == 0) {
            hosts = List.of("jasmin.com", "www.oranum.com");
        } else {
            hosts = Arrays.asList(args);
        }
        LOGGER.info("hosts: " + hosts);
        doIcmpPing(hosts);
        doTcpPing(hosts);
        doTrace(hosts);
    }

    public static void doIcmpPing(List<String> hosts) {
        ScheduledExecutorService scheduler = Executors
                .newScheduledThreadPool(hosts.size());
        hosts.parallelStream().forEach(
                host -> scheduler.scheduleAtFixedRate(() -> {
                    LOGGER.info("Starting icmp ping task for host: " + host);
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
                        OperationResult operationResult = new OperationResult(result, time);
                        LOGGER.info("Finished icmp ping task with result: " + host + " with result: " + operationResult);
                        icmpPingLastResults.put(host, operationResult);
                    } catch (Exception e) {
                        LOGGER.warning("An error has occurred in icpm ping task for host: " + host +", report will be created");
                        sendReport(host);
                    }
                }, 0, getIcpmDelay(), TimeUnit.SECONDS)
        );
    }

    public static void doTcpPing(List<String> hosts) {
        ScheduledExecutorService scheduler = Executors
                .newScheduledThreadPool(hosts.size());
        hosts = hosts.stream().filter(h -> !h.startsWith("https")).map(h -> "https://" + h).collect(Collectors.toList());
        hosts.parallelStream().forEach(
                host -> scheduler.scheduleAtFixedRate(() -> {
                    LOGGER.info("Starting tcp ping task for host: " + host);
                    LocalDateTime time = LocalDateTime.now();
                    long beforeCall = System.currentTimeMillis();
                    List<String> result = new ArrayList<>();
                    try {
                        CloseableHttpClient httpclient = HttpClients.createDefault();
                        HttpGet getMethod = new HttpGet(host);
                        RequestConfig requestConfig = RequestConfig.custom()
                                .setSocketTimeout(getTcpSocketTimeOut())
                                .setConnectTimeout(getTcpConnectionTimeOut())
                                .setConnectionRequestTimeout(getTcpConnectionRequestTimeOut())
                                .build();
                        getMethod.setConfig(requestConfig);
                        CloseableHttpResponse response = httpclient.execute(getMethod);
                        result.add("host: " + host);
                        result.add("request time mls: " + (System.currentTimeMillis() - beforeCall));
                        result.add("httpCode: " + response.getStatusLine().getStatusCode());
                        OperationResult operationResult = new OperationResult(result, time);
                        LOGGER.info("Finished tcp ping task with result: " + host + " with result: " + operationResult);
                        tcpPingLastResults.put(host, operationResult);
                    } catch (Exception e) {
                        LOGGER.warning("An error has occurred in tcp ip ping task for host: " + host +", report will be created");
                        sendReport(host);
                    }
                }, 0, getTcpDelay(), TimeUnit.SECONDS)
        );
    }

    public static void doTrace(List<String> hosts) {
        ScheduledExecutorService scheduler = Executors
                .newScheduledThreadPool(hosts.size());
        hosts.parallelStream().forEach(
                host -> scheduler.scheduleAtFixedRate(() -> {
                    LOGGER.info("Starting trace task for host: " + host);
                    LocalDateTime time = LocalDateTime.now();
                    List<String> result = new ArrayList<>();
                    List<String> command = buildTraceCommand(host);
                    ProcessBuilder processBuilder = new ProcessBuilder(command);
                    try (BufferedReader standardOutput = new BufferedReader(new InputStreamReader(processBuilder.start().getInputStream()))) {
                        String outputLine;
                        while ((outputLine = standardOutput.readLine()) != null) {
                            result.add(outputLine);
                        }
                        OperationResult operationResult = new OperationResult(result, time);
                        LOGGER.info("Finished trace task with result: " + host + " with result: " + operationResult);
                        traceLastResults.put(host, operationResult);
                    } catch (Exception e) {
                        LOGGER.warning("An error has occurred in trace task for host: " + host +", report will be created");
                        sendReport(host);
                    }
                }, 0, getTraceDelay(), TimeUnit.SECONDS)

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
            LOGGER.warning("An error has occurred for host: " + host + ", report request: " + reportRequest);
            ReportSender.sendReport(reportRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> buildPingCommand(String ipAddress) {
        List<String> command = new ArrayList<>();
        command.add(getIcpmPingCommand());

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
        command.add(getTraceCommand());
        command.add(ipAddress);
        return command;
    }

}
