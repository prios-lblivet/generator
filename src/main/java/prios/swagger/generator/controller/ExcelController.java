package prios.swagger.generator.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import prios.swagger.generator.response.ApiExcelResponse;
import prios.swagger.generator.service.ExcelGeneratorService;

@RestController
@RequestMapping("/api/excel")
@CrossOrigin(origins = "*")
class ExcelController {

    private final ExcelGeneratorService excelGeneratorService;

    public ExcelController(ExcelGeneratorService excelGeneratorService) {
        this.excelGeneratorService = excelGeneratorService;
    }
    
    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ApiExcelResponse> generateSwagger(@RequestBody Map<String, String> request) {
    	
        String javaClassContent = request.get("classContent");
        String javaTableContent = request.get("tableContent");
	    
	    return excelGeneratorService.generate(javaClassContent, javaTableContent);
	}
}
