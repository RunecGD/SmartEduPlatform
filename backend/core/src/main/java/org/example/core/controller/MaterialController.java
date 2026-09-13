package org.example.core.controller;

import lombok.RequiredArgsConstructor;
import org.example.core.dto.response.MaterialResponse;
import org.example.core.service.MaterialService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping("/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse upload(@PathVariable Long lessonId,
                                   @RequestParam("file") MultipartFile file) throws Exception {
        return materialService.upload(lessonId, file);
    }

    @GetMapping("/materials/{materialId}/download")
    public Map<String, String> download(@PathVariable Long materialId) throws Exception {
        return Map.of("url", materialService.downloadUrl(materialId));
    }
}