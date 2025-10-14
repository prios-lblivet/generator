package prios.swagger.generator.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import prios.swagger.generator.response.ApiTestResponse;
import prios.swagger.generator.service.TestGeneratorService;
import prios.swagger.generator.utils.GenerateRequest;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
class TestController {

	private final TestGeneratorService testGeneratorService;

	public TestController(TestGeneratorService testGeneratorService) {
		this.testGeneratorService = testGeneratorService;
	}

	@PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiTestResponse generateSwagger(@RequestBody GenerateRequest request,
			@RequestHeader(required = false, defaultValue = "false") boolean deleteRecord,
			@RequestHeader(required = false) boolean idCompany,
			@RequestHeader(required = false) boolean idEstablishment) {
		
		Map<String, String> response = testGeneratorService.generate(request.getMainClassContent(),
				request.getSecondaryClassContent(), deleteRecord, idCompany, idEstablishment);

		return new ApiTestResponse(response.get("entity"), response.get("dto"), response.get("mapper"),
				response.get("mapperView"), response.get("service"), response.get("controller"));
	}

}
