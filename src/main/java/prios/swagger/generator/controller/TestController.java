package prios.swagger.generator.controller;

import java.io.IOException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import prios.swagger.generator.response.ApiResponse;
import prios.swagger.generator.service.TestGeneratorService;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
class TestController {

    private final TestGeneratorService testGeneratorService;

    public TestController(TestGeneratorService testGeneratorService) {
        this.testGeneratorService = testGeneratorService;
    }
    
    @PostMapping(value = "/generate", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> generateSwagger(@RequestBody String javaClassContent) {
	    String originalClassName = testGeneratorService.extractClassName(javaClassContent);
	    
	    // Vérifier si le contenu est une classe Java valide
	    if (originalClassName == null || originalClassName.equals("errorJava")) {
	    	 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		                .body("error : Vous devez envoyer une classe Java");
	    }
	    
	    try {	    	
	        return ResponseEntity.ok(testGeneratorService.generateEntity(javaClassContent, originalClassName));
	    } catch (Exception  e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("Erreur lors de la génération du Swagger : " + e.getMessage());
	    }
	}
}
