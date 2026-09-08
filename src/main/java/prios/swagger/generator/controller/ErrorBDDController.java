package prios.swagger.generator.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import prios.swagger.generator.service.ErrorBDDService;
import prios.swagger.generator.shared.Data;

@RestController
@RequestMapping("/api/error")
@CrossOrigin(origins = "*")
class ErrorBDDController {

    private final ErrorBDDService errorBDDService;

    public ErrorBDDController(ErrorBDDService errorBDDService) {
        this.errorBDDService = errorBDDService;
    }
    
    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Data> generateError(@RequestBody Map<String, String> request) {

        String log = request.get("logContent");
        String table = request.get("tableContent");
        
	    return errorBDDService.generate(log, table);
	}
}
