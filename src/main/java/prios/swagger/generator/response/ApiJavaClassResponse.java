package prios.swagger.generator.response;

public class ApiJavaClassResponse {
    
	private String mapper;
	private String mapperTest;
    private String mapperView;
    private String mapperViewTest;
    private String repository;
    private String service;
    private String serviceImpl;
    private String serviceImplTest;
    private String controller;
    private String controllerTest;
    private String swagger;

	public ApiJavaClassResponse(String mapper, String mapperTest, String mapperView, String mapperViewTest, String repository, String service,
			String serviceImpl, String serviceImplTest, String controller, String controllerTest, String swagger) {
		this.mapper = mapper;
		this.mapperTest = mapperTest;
		this.mapperView = mapperView;
		this.mapperViewTest = mapperViewTest;
		this.setRepository(repository);
		this.service = service;
		this.serviceImpl = serviceImpl;
		this.serviceImplTest = serviceImplTest;
		this.controller = controller;
		this.controllerTest = controllerTest;
		this.swagger = swagger;
	}
    
	public String getMapper() {
		return mapper;
	}
	
	public void setMapper(String mapper) {
		this.mapper = mapper;
	}
	
	public String getMapperView() {
		return mapperView;
	}
	
	public void setMapperView(String mapperView) {
		this.mapperView = mapperView;
	}
	
	public String getService() {
		return service;
	}
	
	public void setService(String service) {
		this.service = service;
	}
	
	public String getServiceImpl() {
		return serviceImpl;
	}
	
	public void setServiceImpl(String serviceImpl) {
		this.serviceImpl = serviceImpl;
	}
	
	public String getController() {
		return controller;
	}
	
	public void setController(String controller) {
		this.controller = controller;
	}
	
	public String getSwagger() {
		return swagger;
	}
	
	public void setSwagger(String swagger) {
		this.swagger = swagger;
	}  

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

	public String getMapperTest() {
		return mapperTest;
	}

	public void setMapperTest(String mapperTest) {
		this.mapperTest = mapperTest;
	}

	public String getMapperViewTest() {
		return mapperViewTest;
	}

	public void setMapperViewTest(String mapperViewTest) {
		this.mapperViewTest = mapperViewTest;
	}

	public String getServiceImplTest() {
		return serviceImplTest;
	}

	public void setServiceImplTest(String serviceImplTest) {
		this.serviceImplTest = serviceImplTest;
	}

	public String getControllerTest() {
		return controllerTest;
	}

	public void setControllerTest(String controllerTest) {
		this.controllerTest = controllerTest;
	}
}