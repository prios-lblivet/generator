package prios.swagger.generator.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import prios.swagger.generator.response.ApiResponse;
import prios.swagger.generator.service.JavaGeneratorService;

@RestController
@RequestMapping("/api/java")
@CrossOrigin(origins = "*")
class JavaController {

    private final JavaGeneratorService javaGeneratorService;

    public JavaController(JavaGeneratorService javaGeneratorService) {
        this.javaGeneratorService = javaGeneratorService;
    }
    
    @PostMapping("/generate")
    public ApiResponse generateJavaClasses(@RequestHeader String className, @RequestHeader String apiName, @RequestBody String csvContent) {
    	List<Map<String, String>> csvContentList = javaGeneratorService.csvToList(csvContent);
        String entity = javaGeneratorService.generateEntity(className, apiName, csvContentList);
        String table = javaGeneratorService.generateTable(className, apiName, csvContentList);
        String mapper = javaGeneratorService.generateMapper(className, apiName);
        String controller = javaGeneratorService.generateController(className, apiName);
        String service = javaGeneratorService.generateService(className, apiName);
        String serviceImpl = javaGeneratorService.generateServiceImpl(className, apiName);
        String repository = javaGeneratorService.generateRepository(className, apiName);
        String swagger = javaGeneratorService.generateSwagger(entity);

        return new ApiResponse(entity, table, mapper, controller, service, serviceImpl, repository, swagger);
    }
    
}
