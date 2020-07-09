package com.docler.ping.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class OperationResult {

    private List<String> results;
    private LocalDateTime dateTime;
}
