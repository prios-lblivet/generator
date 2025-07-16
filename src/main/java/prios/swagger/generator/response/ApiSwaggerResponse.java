package prios.swagger.generator.response;

public class ApiSwaggerResponse {
    private String swaggerYaml;
    private String javaEntity;
    private String fileName;

    // Constructeurs, getters et setters
    public ApiSwaggerResponse(String swaggerYaml, String javaEntity, String fileName) {
        this.swaggerYaml = swaggerYaml;
        this.javaEntity = javaEntity;
        this.fileName = fileName;
    }

    public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getSwaggerYaml() {
        return swaggerYaml;
    }

    public void setSwaggerYaml(String swaggerYaml) {
        this.swaggerYaml = swaggerYaml;
    }

    public String getJavaEntity() {
        return javaEntity;
    }

    public void setJavaEntity(String javaEntity) {
        this.javaEntity = javaEntity;
    }
}