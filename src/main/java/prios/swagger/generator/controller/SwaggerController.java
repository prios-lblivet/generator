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

import prios.swagger.generator.response.ApiSwaggerResponse;
import prios.swagger.generator.response.ApiTestResponse;
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
	public ApiSwaggerResponse generateSwagger(@RequestBody String javaClassContent) {
	    String originalClassName = swaggerGeneratorService.extractClassName(javaClassContent);
	        
	    String swaggerName = swaggerGeneratorService.toSnakeCase(originalClassName); // Convertir en snake_case
	    String fileName = swaggerName + ".yaml"; // Nom dynamique
	    String swaggerYaml = swaggerGeneratorService.generateSwaggerYaml(javaClassContent);
	    String entity = swaggerGeneratorService.replaceApiObjectFieldWithComment(javaClassContent);
	    
	    
      
	    return new ApiSwaggerResponse(swaggerYaml, entity , fileName );
	}
}
