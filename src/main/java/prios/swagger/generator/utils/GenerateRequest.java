package prios.swagger.generator.utils;
public class GenerateRequest {
    private String mainClassContent;
    private String secondaryClassContent; // peut être null ou vide

    // Getters & Setters
    public String getMainClassContent() {
        return mainClassContent;
    }

    public void setMainClassContent(String mainClassContent) {
        this.mainClassContent = mainClassContent;
    }

    public String getSecondaryClassContent() {
        return secondaryClassContent;
    }

    public void setSecondaryClassContent(String secondaryClassContent) {
        this.secondaryClassContent = secondaryClassContent;
    }
}
