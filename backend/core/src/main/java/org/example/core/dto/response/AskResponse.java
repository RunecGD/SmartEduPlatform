package org.example.core.dto.response;

import java.util.List;

public record AskResponse(String answer, List<String> sources) {}