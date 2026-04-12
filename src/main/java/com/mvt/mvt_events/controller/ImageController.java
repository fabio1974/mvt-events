package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Endpoint de upload de imagens para o módulo Zapi-Food.
 * Imagens são armazenadas no Cloudinary e a URL é retornada.
 */
@RestController
@RequestMapping("/api/images")
@Tag(name = "Images", description = "Upload de imagens (Zapi-Food)")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload de imagem", description = "Faz upload para Cloudinary. Folders: products, categories, stores")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "products") String folder) throws IOException {
        String url = imageService.upload(file, folder);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @DeleteMapping
    @Operation(summary = "Remove imagem", description = "Remove imagem do Cloudinary pela URL")
    public ResponseEntity<Map<String, String>> delete(@RequestParam("url") String url) {
        imageService.delete(url);
        return ResponseEntity.ok(Map.of("message", "Imagem removida"));
    }
}
