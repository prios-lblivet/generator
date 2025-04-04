package prios.swagger.generator.response;

public class ApiResponse {
    private String entity;
    private String table;
    private String mapper;
    private String controller;
    private String service;
    private String serviceImpl;
    private String repository;
    private String swagger;

    // Constructeurs, getters et setters
    public ApiResponse(String entity, String table, String mapper, String controller, String service, String serviceImpl, String repository, String swagger) {
        this.entity = entity;
        this.table = table;
        this.mapper = mapper;
        this.controller = controller;
        this.service = service;
        this.serviceImpl = serviceImpl;
        this.repository = repository;
        this.swagger = swagger;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getMapper() {
        return mapper;
    }

    public void setMapper(String mapper) {
        this.mapper = mapper;
    }

    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
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

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getSwagger() {
        return swagger;
    }

    public void setSwagger(String swagger) {
        this.swagger = swagger;
    }
}