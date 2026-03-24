package prios.swagger.generator.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import prios.swagger.generator.service.JdbiGeneratorService;

@RestController
@RequestMapping("/api/jdbi")
@CrossOrigin(origins = "*")
class JdbiController {

    private final JdbiGeneratorService jdbiGeneratorService;

    public JdbiController(JdbiGeneratorService jdbiGeneratorService) {
        this.jdbiGeneratorService = jdbiGeneratorService;
    }
    
    @PostMapping(value = "/generate", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String generateJdbi(@RequestBody String javaClassContent) {
	    return jdbiGeneratorService.generateJdbi(javaClassContent);
	}
}
