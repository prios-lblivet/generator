package prios.swagger.generator.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
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
    	javaGeneratorService.init(csvContent, className, apiName);
        String entity = javaGeneratorService.generateEntity();
        String table = javaGeneratorService.generateTable();
        String mapper = javaGeneratorService.generateMapper();
        String controller = javaGeneratorService.generateController();
        String service = javaGeneratorService.generateService();
        String serviceImpl = javaGeneratorService.generateServiceImpl();
        String repository = javaGeneratorService.generateRepository();
        String swagger = javaGeneratorService.generateSwagger(entity);

        return new ApiResponse(entity, table, mapper, controller, service, serviceImpl, repository, swagger);
    }
    
}
