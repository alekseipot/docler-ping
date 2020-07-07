package com.docler.ping;

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
