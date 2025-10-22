package prios.swagger.generator.response;

public class ApiJavaClassResponse {
    
	private String mapper;
    private String mapperView;
    private String service;
    private String serviceImpl;
    private String controller;
    private String swagger;
    
    public ApiJavaClassResponse(String mapper, String mapperView, String service, String serviceImpl, String controller,
			String swagger) {
		this.mapper = mapper;
		this.mapperView = mapperView;
		this.service = service;
		this.serviceImpl = serviceImpl;
		this.controller = controller;
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
}