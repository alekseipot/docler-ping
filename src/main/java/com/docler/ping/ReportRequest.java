package com.docler.ping;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReportRequest {
    String host;
    @JsonProperty("icmp_ping")
    String icmpPing;
    @JsonProperty("tcp_ping")
    String tcpPing;
    String trace;
}
