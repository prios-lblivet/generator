package prios.swagger.generator.response;

public class ApiTestResponse {
    private String entity;
    private String dto;

    // Constructeurs, getters et setters
    public ApiTestResponse(String entity, String dto) {
        this.entity = entity;
        this.dto = dto;
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
}