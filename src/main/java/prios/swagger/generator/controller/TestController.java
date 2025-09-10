package prios.swagger.generator.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import prios.swagger.generator.response.ApiTestResponse;
import prios.swagger.generator.service.TestGeneratorService;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
class TestController {

    private final TestGeneratorService testGeneratorService;

    public TestController(TestGeneratorService testGeneratorService) {
        this.testGeneratorService = testGeneratorService;
    }
    
    @PostMapping(value = "/generate", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiTestResponse generateSwagger(@RequestBody String javaClassContent) {
	    String originalClassName = testGeneratorService.extractClassName(javaClassContent);
	    
	    Map<String, String> response = testGeneratorService.generate(javaClassContent, originalClassName);
        
	    return new ApiTestResponse(response.get("entity"), response.get("dto"));
	}
}
