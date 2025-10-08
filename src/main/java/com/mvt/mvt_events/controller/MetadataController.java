package com.mvt.mvt_events.controller;

import com.mvt.mvt_events.metadata.EntityMetadata;
import com.mvt.mvt_events.metadata.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/metadata")
@Tag(name = "Metadados", description = "Metadados das entidades do sistema (acesso público)")
public class MetadataController {

    private final MetadataService metadataService;

    public MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping
    @Operation(summary = "Listar todos os metadados", description = "Retorna metadados de todas as entidades")
    public Map<String, EntityMetadata> getAllMetadata() {
        return metadataService.getAllEntitiesMetadata();
    }

    @GetMapping("/{entityName}")
    @Operation(summary = "Buscar metadados de entidade", description = "Retorna metadados de uma entidade específica")
    public EntityMetadata getEntityMetadata(@PathVariable String entityName) {
        return metadataService.getEntityMetadata(entityName);
    }
}
