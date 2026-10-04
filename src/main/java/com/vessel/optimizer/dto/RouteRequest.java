package com.vessel.optimizer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RouteRequest(
    @NotNull @Size(min = 2, max = 20, message = "Must provide 2-20 port codes")
    List<String> portCodes
) {}