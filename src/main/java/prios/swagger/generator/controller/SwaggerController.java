package prios.swagger.generator.controller;

import org.springframework.web.bind.annotation.*;

import prios.swagger.generator.service.SwaggerGeneratorService;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping("/api/swagger")
@CrossOrigin(origins = "*")
class SwaggerController {

    private final SwaggerGeneratorService swaggerGeneratorService;

    public SwaggerController(SwaggerGeneratorService swaggerGeneratorService) {
        this.swaggerGeneratorService = swaggerGeneratorService;
    }

    @PostMapping(value = "/generate", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> generateSwagger(@RequestBody String javaClassContent) {
        try {
            String swaggerYaml = swaggerGeneratorService.generateSwaggerYaml(javaClassContent);
            String fileName = swaggerGeneratorService.toSnakeCase(swaggerGeneratorService.extractClassName(javaClassContent)) + ".yaml"; // Nom dynamique
            Map<String, String> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("swaggerYaml", swaggerYaml);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erreur lors de la génération du Swagger : " + e.getMessage()));
        }
    }
}
