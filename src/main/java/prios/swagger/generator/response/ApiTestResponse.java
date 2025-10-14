package prios.swagger.generator.response;

public class ApiTestResponse {
    private String entity;
    private String dto;
    private String mapper;
    private String mapperView;
    private String service;
    private String controller;

    // Constructeurs, getters et setters
    public ApiTestResponse(String entity, String dto, String mapper, String mapperView, String service, String controller) {
        this.entity = entity;
        this.dto = dto;
        this.mapper = mapper;
        this.mapperView = mapperView;
        this.service = service;
        this.controller = controller;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getDto() {
        return dto;
    }

    public void setDto(String dto) {
        this.dto = dto;
    }

	public String getMapperView() {
		return mapperView;
	}

	public void setMapperView(String mapperView) {
		this.mapperView = mapperView;
	}

	public String getMapper() {
		return mapper;
	}

	public void setMapper(String mapper) {
		this.mapper = mapper;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getController() {
		return controller;
	}

	public void setController(String controller) {
		this.controller = controller;
	}
}