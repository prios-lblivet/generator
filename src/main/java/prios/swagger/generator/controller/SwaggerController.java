package prios.swagger.generator.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import prios.swagger.generator.service.SwaggerGeneratorService;

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
	    String originalClassName = swaggerGeneratorService.extractClassName(javaClassContent);
	    
	    // Vérifier si le contenu est une classe Java valide
	    if (originalClassName == null || originalClassName.equals("errorJava")) {
	    	 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		                .body(Collections.singletonMap("error", "Vous devez envoyer une classe Java"));
	    }
	    
	    String swaggerName = swaggerGeneratorService.toSnakeCase(originalClassName); // Convertir en snake_case
	    String fileName = swaggerName + ".yaml"; // Nom dynamique
	    String swaggerYaml = swaggerGeneratorService.generateSwaggerYaml(javaClassContent);

	    try {
	    	
	        // Construire la réponse JSON avec le nom de fichier et le contenu YAML
	        Map<String, String> response = new HashMap<>();
	        response.put("fileName", fileName);
	        response.put("swaggerYaml", swaggerYaml);

	        return ResponseEntity.ok(response);
	    } catch (Exception  e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Collections.singletonMap("error", "Erreur lors de la génération du Swagger : " + e.getMessage()));
	    }
	}
}
