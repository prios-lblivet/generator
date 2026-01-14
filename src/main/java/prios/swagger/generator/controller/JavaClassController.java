package prios.swagger.generator.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import prios.swagger.generator.response.ApiJavaClassResponse;
import prios.swagger.generator.service.JavaClassGeneratorService;
import prios.swagger.generator.utils.GenerateRequest;

@RestController
@RequestMapping("/api/javaClass")
@CrossOrigin(origins = "*")
class JavaClassController {

	private final JavaClassGeneratorService javaClassGeneratorService;

	public JavaClassController(JavaClassGeneratorService javaClassGeneratorService) {
		this.javaClassGeneratorService = javaClassGeneratorService;
	}

	@PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiJavaClassResponse generateJavaClass(@RequestBody GenerateRequest request,
			@RequestHeader(required = false, defaultValue = "false") boolean deleteRecord,
			@RequestHeader(required = false) boolean idCompany, @RequestHeader(required = false) boolean postAll,
			@RequestHeader(required = false) boolean putAll, @RequestHeader(required = false) boolean patchById,
			@RequestHeader(required = false) boolean deleteAll) {

		Map<String, String> response = javaClassGeneratorService.generate(request.getMainClassContent(),
				request.getSecondaryClassContent(), deleteRecord, idCompany, postAll, putAll, patchById, deleteAll);

		return new ApiJavaClassResponse(response.get("mapper"), response.get("mapperTest"), response.get("mapperView"),
				response.get("mapperViewTest"), response.get("repository"), response.get("service"),
				response.get("serviceImpl"), response.get("serviceImplTest"), response.get("controller"),
				response.get("controllerTest"), response.get("swagger"), response.get("feign"));
	}

}
