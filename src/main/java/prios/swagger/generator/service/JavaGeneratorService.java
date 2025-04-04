package prios.swagger.generator.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class JavaGeneratorService {
    
	public String generateEntity(String className, String apiName, List<Map<String, String>> csvContent) {
	    String packageName = "com.prios.api.a." + apiName + ".shared." + classNameToPackage(className);

	    // Initialiser le code de la classe avec la déclaration du package et les imports
	    StringBuilder classCode = new StringBuilder();
	    classCode.append("package ").append(packageName).append(";\n\n");

	    classCode.append("import java.io.Serializable;\n");
	    classCode.append("import javax.persistence.Convert;\n");
	    classCode.append("import javax.persistence.Embedded;\n");
	    classCode.append("import javax.persistence.GeneratedValue;\n");
	    classCode.append("import javax.persistence.Id;\n");
	    classCode.append("import javax.persistence.MappedSuperclass;\n");
	    classCode.append("import javax.persistence.PrePersist;\n");
	    classCode.append("import javax.persistence.PreUpdate;\n");
	    classCode.append("import javax.validation.constraints.Digits;\n");
	    classCode.append("import org.hibernate.annotations.GenericGenerator;\n");
	    classCode.append("import org.hibernate.annotations.Type;\n");
	    classCode.append("import org.hibernate.annotations.TypeDef;\n");
	    classCode.append("import org.jsondoc.core.annotation.ApiObject;\n");
	    classCode.append("import org.jsondoc.core.annotation.ApiObjectField;\n");
	    classCode.append("import com.prios.core.a.common.history.management.HistoryManagementA;\n");
	    classCode.append("import com.prios.tools.config.data.IdConverter;\n");
	    classCode.append("import com.prios.tools.config.data.OuiNonType;\n");
	    classCode.append("import lombok.Getter;\n");
	    classCode.append("import lombok.Setter;\n\n");

	    // Ajouter les annotations de la classe
	    classCode.append("@Getter\n");
	    classCode.append("@Setter\n");
	    classCode.append("@MappedSuperclass\n");
	    classCode.append("@ApiObject(name = \"Pricing Cost\", description = \"Tarif de Vente - Coûts\")\n");
	    classCode.append("@TypeDef(name = \"ouiNonType\", typeClass = OuiNonType.class)\n");

	    classCode.append("public class ").append(className).append(" implements Serializable {\n\n");

        classCode.append("\t// TODO: générer serialVersionUID\n\n");
	    
	    // Stocker les classes Embedded pour éviter les doublons
        Set<String> embeddedClasses = new HashSet<>();

	    // Ajouter les champs
	    for (Map<String, String> rowMap : csvContent) {
	        String nomVariable = rowMap.get("Nom variable");
	        String description = rowMap.get("Libellé Champ");

	        
	        
	        String embedded = rowMap.get("Embedded");
	        String dec = rowMap.get("Dec");
	        String lng = rowMap.get("Lng");
	        String digit = rowMap.get("Digit");
	        
	        if (embedded.equals("HistoryManagementA"))
	        	description = "Gestion Utilisateur";
	   
	        String embeddedName = nomVariable.split("\\.")[0];
	        // verifier si on a pas un embedded et si il est pas déjà dans la liste
	        if (!(embedded != null && !embedded.isEmpty() && embeddedClasses.contains(embeddedName))) {
	        	// Ajouter un champ avec annotation
		        classCode.append("\t/**\n");
		        classCode.append("\t* ").append(description).append("\n");
		        classCode.append("\t*/\n");
	        }

	        // Si "Embedded" est renseigné, le type est celui de la valeur
	        if (embedded != null && !embedded.isEmpty()) {
	        	if (!embeddedClasses.contains(embeddedName)) {
	        		embeddedClasses.add(embeddedName);
	        		classCode.append("\t@Embedded\n");
		            classCode.append("\t@Valid\n");
			        classCode.append("\t@ApiObjectField(description = \"").append(description).append("\")\n");
		            classCode.append("\tprivate ").append(embedded).append(" ").append(embeddedName).append(";\n\n");  
	        	}  
	        }
	        // Si Lng et Digit sont à 1, c'est un boolean
	        else if ("1".equals(lng) && "1".equals(digit)) {
	            classCode.append("\t@Type(type = \"ouiNonType\")\n");
		        classCode.append("\t@ApiObjectField(description = \"").append(description).append("\")\n");
	            classCode.append("\tprivate boolean ").append(nomVariable).append(";\n\n");
	        }
	        // Si "Dec" est vide, c'est un String
	        else if (dec == null || dec.isEmpty()) {
	            classCode.append("\t@Size(max = ").append(lng).append(")\n");
		        classCode.append("\t@ApiObjectField(description = \"").append(description).append("\")\n");
	            classCode.append("\tprivate String ").append(nomVariable).append(";\n\n");
	        }
	        // Si "Dec" est "0"
	        else if (dec.equals("0")) {
	            // Si Lng <= 9, c'est un Integer, sinon c'est un long
	            if (Integer.parseInt(lng) <= 9) {
	            	if (nomVariable.equals("id"))
			            classCode.append("\t@Id\n");
		            classCode.append("\t@Digits(integer = ").append(lng).append(", fraction = 0)\n");
			        classCode.append("\t@ApiObjectField(description = \"").append(description).append("\")\n");
		            classCode.append("\tprivate Integer ").append(nomVariable).append(";\n\n");
	            } else {
		            classCode.append("\t@Digits(integer = ").append(lng).append(", fraction = 0)\n");
			        classCode.append("\t@ApiObjectField(description = \"").append(description).append("\")\n");
		            classCode.append("\tprivate Long ").append(nomVariable).append(";\n\n");
	            }
	        }
	        // Si "Dec" contient un chiffre
	        else if (dec.matches("\\d+")) {
	            // Si Lng <= 9, c'est un Double, sinon c'est un Float
	        	Integer decInt = Integer.parseInt(dec);
		        Integer digitInt = Integer.parseInt(digit);
	            if (Integer.parseInt(lng) <= 9) {
		            classCode.append("\t@Digits(integer = ").append(digitInt - decInt).append(", fraction = ").append(dec).append(")\n");
			        classCode.append("\t@ApiObjectField(description = \"").append(description).append("\")\n");
		            classCode.append("\tprivate Double ").append(nomVariable).append(";\n\n");
	            } else {
		            classCode.append("\t@Digits(integer = ").append(digitInt - decInt).append(", fraction = ").append(dec).append(")\n");
			        classCode.append("\t@ApiObjectField(description = \"").append(description).append("\")\n");
		            classCode.append("\tprivate Float ").append(nomVariable).append(";\n\n");
	            }
	        }        

	    }
	    

        if (embeddedClasses.contains("historyManagement")) {
        	classCode.append("\t/**\n");
    	    classCode.append("\t* Pre Insert\n");
    	    classCode.append("\t*/\n");
    	    classCode.append("\t@PrePersist\n");
    	    classCode.append("\tpublic void prePersist() {\n");
    	    classCode.append("\t\tinitHistoryManagement();\n");
    	    classCode.append("\t\thistoryManagement.prePersist();\n");
    	    classCode.append("\t}\n\n");

    	    classCode.append("\t/**\n");
    	    classCode.append("\t* Pre Update\n");
    	    classCode.append("\t*/\n");
    	    classCode.append("\t@PreUpdate\n");
    	    classCode.append("\tpublic void preUpdate() {\n");
    	    classCode.append("\t\tinitHistoryManagement();\n");
    	    classCode.append("\t\thistoryManagement.preUpdate();\n");
    	    classCode.append("\t}\n\n");

    	    // Ajouter la méthode initHistoryManagement
    	    classCode.append("\tprivate void initHistoryManagement() {\n");
    	    classCode.append("\t\tif (historyManagement == null)\n");
    	    classCode.append("\t\t\thistoryManagement = new HistoryManagementA();\n");
    	    classCode.append("\t}\n\n");
        }   

	    // Fin de la classe
	    classCode.append("}\n");

	    // Retourner le code de la classe générée
	    return classCode.toString();
	}


    public String generateTable(String className, String apiName, List<Map<String, String>> csvContent) {
    	String name = className + "Table";
    	String packageName = "com.prios.api.a." + apiName + ".shared." + classNameToPackage(className);
    	
    	// Initialiser le code de la classe avec la déclaration du package et les imports
        StringBuilder classCode = new StringBuilder();
        classCode.append("package ").append(packageName).append(";\n\n");

        classCode.append("import javax.persistence.AttributeOverride;\n");
        classCode.append("import javax.persistence.Column;\n");
        classCode.append("import javax.persistence.Entity;\n");
        classCode.append("import javax.persistence.Table;\n");
        classCode.append("import org.hibernate.annotations.DynamicUpdate;\n");
        classCode.append("import org.jsondoc.core.annotation.ApiObject;\n");
        classCode.append("import com.fasterxml.jackson.annotation.JsonIgnoreProperties;\n");
        classCode.append("import lombok.EqualsAndHashCode;\n");
        classCode.append("import lombok.Getter;\n");
        classCode.append("import lombok.Setter;\n\n");

        // Ajouter les annotations de la classe
        classCode.append("@Getter\n");
        classCode.append("@Setter\n");
        classCode.append("@EqualsAndHashCode(callSuper = false)\n");
        classCode.append("@Entity\n");
        classCode.append("@Table(name = \"BTARVCP\")\n");
        classCode.append("@ApiObject(name = \"Pricing Cost Table\", description = \"Tarif de Vente - Coûts (Table)\")\n");
        classCode.append("@JsonIgnoreProperties(ignoreUnknown = true)\n");
        
        for (Map<String, String> rowMap : csvContent) {
        	String nomVariable = rowMap.get("Nom variable");
            String nomLong = rowMap.get("Nom");

            // Vérifier si "historyManagement" et "creation" sont présents dans nomVariable
            boolean addUpdatableFalse = nomVariable != null && nomVariable.contains("historyManagement") && nomVariable.toLowerCase().contains("creation");

            // Créer l'annotation avec ou sans updatable = false
            classCode.append("@AttributeOverride(name = \"").append(nomVariable)
                      .append("\", column = @Column(name = \"").append(nomLong);
            
            if (addUpdatableFalse) {
                classCode.append("\", updatable = false");  // Ajouter updatable = false
            }
            classCode.append("))\n");
        }   
       
        classCode.append("@DynamicUpdate\n");
        classCode.append("public class ").append(name).append(" extends PricingCost {\n\n");

        // Ajouter un champ statique serialVersionUID
        classCode.append("\t// TODO: générer serialVersionUID\n");   

        // Terminer la classe
        classCode.append("\n}");

        // Retourner le code de la classe générée
        return classCode.toString();
    }
    
    public String generateMapper(String className, String apiName) {
        String packageName = "com.prios.api.a." + apiName + ".mapper." + classNameToPackage(className);
        String entityName = className + "Table";
        String dtoName = className + "Dto";
        String mapperName = className + "Mapper";
        
        return "package " + packageName + ";\n\n" +
               "import org.mapstruct.Mapper;\n" +
               "import java.util.List;\n" +
               "import java.util.Optional;\n\n" +
               "import com.prios.api.a." + apiName + ".shared." + classNameToPackage(className) + "." + entityName + ";\n" +
               "import com.prios.core.a.shared.dto." + apiName + "." + dtoName + ";\n\n" +
               "@Mapper\n" +
               "public interface " + mapperName + " {\n\n" +
               "\tList<" + entityName + "> " + toCamelCase(entityName) + "DtosTo" + entityName + "s(List<" + dtoName + "> " + toCamelCase(entityName) + "Dtos);\n\n" +
               "\t" + dtoName + " " + toCamelCase(entityName) + "To" + dtoName + "(" + entityName + " " + toCamelCase(entityName) + ");\n\n" +
               "\tList<" + dtoName + "> " + toCamelCase(entityName) + "To" + dtoName + "s(List<" + entityName + "> " + toCamelCase(entityName) + "s);\n\n" +
               "\tdefault Optional<" + dtoName + "> optional" + entityName + "ToOptional" + dtoName + "(Optional<" + entityName + "> optional" + entityName + ") {\n" +
               "\t\treturn optional" + entityName + ".map(this::" + toCamelCase(entityName) + "To" + dtoName + ");\n" +
               "\t}\n" +
               "}";
    }
    
    public String generateController(String className, String apiName) {
        String packageName = "com.prios.api.a." + apiName + ".controller." + classNameToPackage(className);
        String serviceName = className + "Service";
        String serviceVarName = toCamelCase(serviceName);
        String mapperName = className + "Mapper";
        String mapperVarName = toCamelCase(mapperName);
        String entityName = className + "Table";
        String dtoName = className + "Dto";
        String controllerName = className + "ControllerRest";
        
        return "package " + packageName + ";\n\n" +
               "import static com.prios.tools.util.MethodUtils.getMethodName;\n" +
               "import static com.prios.tools.util.criteria.SearchUtils.getRequestParams;\n" +
               "import static org.springframework.http.ResponseEntity.notFound;\n\n" +
               "import java.util.List;\n\n" +
               "import javax.servlet.http.HttpServletRequest;\n" +
               "import javax.validation.Valid;\n" +
               "import javax.validation.constraints.NotNull;\n\n" +
               "import org.springframework.http.HttpStatus;\n" +
               "import org.springframework.http.ResponseEntity;\n" +
               "import org.springframework.web.bind.annotation.GetMapping;\n" +
               "import org.springframework.web.bind.annotation.RequestHeader;\n" +
               "import org.springframework.web.bind.annotation.RequestParam;\n" +
               "import org.springframework.web.bind.annotation.RestController;\n\n" +
               "import com.prios.api.a." + apiName + ".mapper." + classNameToPackage(className) + "." + mapperName + ";\n" +
               "import com.prios.api.a." + apiName + ".service." + classNameToPackage(className) + "." + serviceName + ";\n" +
               "import com.prios.api.a." + apiName + ".shared." + classNameToPackage(className) + "." + entityName + ";\n" +
               "import com.prios.core.a.controller." + apiName + "." + className + "Controller;\n" +
               "import com.prios.core.a.shared.dto." + apiName + "." + dtoName + ";\n\n" +
               "import lombok.RequiredArgsConstructor;\n\n" +
               "@RestController\n" +
               "@RequiredArgsConstructor\n" +
               "public class " + controllerName + " implements " + className + "Controller {\n\n" +
               "\tprivate final " + mapperName + " " + mapperVarName + ";\n" +
               "\tprivate final " + serviceName + " " + serviceVarName + ";\n\n" +
               "\t@GetMapping(value = \"/v1/" + toCamelCase(className) + "s\")\n" +
               "\tpublic ResponseEntity<List<" + dtoName + ">> getAll" + className + "(HttpServletRequest request,\n" +
               "\t\t\t\t\t\t\t\t\t@RequestHeader(value = \"idCompany\") Integer idCompany,\n" +
               "\t\t\t\t\t\t\t\t\t@RequestHeader(value = \"idEstablishment\") Integer idEstablishment,\n" +
               "\t\t\t\t\t\t\t\t\t@RequestParam(value = \"deleteRecord\", required = false) String deleteRecord) {\n\n" +
               "\t\tList<" + entityName + "> " + toCamelCase(entityName) + "s = " + serviceVarName + ".findAll(getRequestParams(this, getMethodName(), request), idCompany, idEstablishment, deleteRecord);\n" +
               "\t\tList<" + dtoName + "> " + toCamelCase(dtoName) + "s = " + mapperVarName + "." + toCamelCase(entityName) + "To" + dtoName + "s(" + toCamelCase(entityName) + "s);\n" +
               "\t\treturn ResponseEntity.ok(" + toCamelCase(dtoName) + "s);\n" +
               "\t}\n\n" +
               "\t@Override\n" +
               "\tpublic ResponseEntity<List<" + dtoName + ">> getAll" + className + "(@NotNull Integer idCompany, @NotNull Integer idEstablishment,\n" +
               "\t\t\t@Valid String deleteRecord) {\n" +
               "\t\t return ResponseEntity.status(HttpStatus.FORBIDDEN).build();\n" +
               "\t}\n\n" +
               "\t@Override\n" +
               "\tpublic ResponseEntity<" + dtoName + "> get" + className + "ById(Integer id, @NotNull Integer idCompany, @NotNull Integer idEstablishment) {\n" +
               "\t\treturn " + mapperVarName + ".optional" + className + "ToOptional" + dtoName + "(" + serviceVarName + ".findById(id))\n" +
               "\t\t\t.map(ResponseEntity::ok)\n" +
               "\t\t\t.orElse(notFound().build());\n" +
               "\t}\n" +
               "}";
    }

    public String generateService(String className, String apiName) {
        String packageName = "com.prios.api.a." + apiName + ".service." + classNameToPackage(className);
        String entityName = className + "Table";
        String serviceName = className + "Service";

        return "package " + packageName + ";\n\n" +
               "import java.util.List;\n" +
               "import java.util.Optional;\n\n" +
               "import com.prios.api.a." + apiName + ".shared." + classNameToPackage(className) + "." + entityName + ";\n" +
               "import com.prios.tools.util.criteria.SearchCriteria;\n\n" +
               "public interface " + serviceName + " {\n\n" +
               "\tOptional<" + entityName + "> findById(final int id);\n\n" +
               "\tList<" + entityName + "> findAll(final List<SearchCriteria> requestParam, final int idCompany, final int idEstablishment, final String deleteRecord);\n\n" +
               "}";
    }
    
    public String generateServiceImpl(String className, String apiName) {
        String packageName = "com.prios.api.a." + apiName + ".service." + classNameToPackage(className);
        String repositoryName = className + "Repository";
        String entityName = className + "Table";
        String serviceName = className + "Service";
        String serviceImplName = className + "ServiceImpl";

        return "package " + packageName + ";\n\n" +
               "import java.util.List;\n" +
               "import java.util.Optional;\n\n" +
               "import org.springframework.stereotype.Service;\n\n" +
               "import com.prios.api.a." + apiName + ".repository." + classNameToPackage(className) + "." + repositoryName + ";\n" +
               "import com.prios.api.a." + apiName + ".shared." + classNameToPackage(className) + "." + entityName + ";\n" +
               "import com.prios.core.a.util.criteria.specification.table.TableManagementLevelDeleteRecordSpecification;\n" +
               "import com.prios.tools.util.criteria.SearchCriteria;\n" +
               "import com.prios.tools.util.criteria.specification.PriosParams;\n\n" +
               "import lombok.RequiredArgsConstructor;\n\n" +
               "@RequiredArgsConstructor\n" +
               "@Service\n" +
               "public class " + serviceImplName + " extends TableManagementLevelDeleteRecordSpecification<" + entityName + "> implements " + serviceName + " {\n\n" +
               "\tprivate final " + repositoryName + " " + toCamelCase(repositoryName) + ";\n\n" +
               "\t@Override\n" +
               "\tpublic List<" + entityName + "> findAll(final List<SearchCriteria> requestParams, final int idCompany, final int idEstablishment, final String deleteRecord) {\n" +
               "\t\tPriosParams params = new PriosParams(idCompany, idEstablishment, requestParams, deleteRecord);\n" +
               "\t\treturn " + toCamelCase(repositoryName) + ".findAll(table(params));\n" +
               "\t}\n\n" +
               "\t@Override\n" +
               "\tpublic Optional<" + entityName + "> findById(final int id) {\n" +
               "\t\treturn " + toCamelCase(repositoryName) + ".findById(id);\n" +
               "\t}\n\n" +
               "}";
    }

    public String generateRepository(String className, String apiName) {
        String packageName = "com.prios.api.a." + apiName + ".repository." + classNameToPackage(className);
        String entityName = className + "Table";
        String repositoryName = className + "Repository";

        return "package " + packageName + ";\n\n" +
               "import org.springframework.data.jpa.repository.JpaRepository;\n" +
               "import org.springframework.data.jpa.repository.JpaSpecificationExecutor;\n\n" +
               "import com.prios.api.a." + apiName + ".shared." + classNameToPackage(className) + "." + entityName + ";\n\n" +
               "public interface " + repositoryName + 
               " extends JpaRepository<" + entityName + ", Integer>, JpaSpecificationExecutor<" + entityName + "> {\n\n" +
               "}";
    }

    
    public String generateSwagger(String javaClassContent) {
    	SwaggerGeneratorService swaggerGeneratorService = new SwaggerGeneratorService();
        return swaggerGeneratorService.generateSwaggerYaml(javaClassContent);
    }
    
    private String classNameToPackage(String className) {
    	return className.replaceAll("([a-z])([A-Z])", "$1.$2").toLowerCase();
    }
    
    private String toCamelCase(String className) {
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
    
    public List<Map<String, String>> csvToList(String csvContent) {
        // Initialiser une liste pour stocker toutes les lignes sous forme de liste
        List<List<String>> allRows = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();

        // Séparer le CSV en lignes
        String[] lines = csvContent.split("\n");

        // Récupérer les noms des colonnes depuis la première ligne
        columnNames = Arrays.asList(lines[0].split(";"));

        // Parcourir chaque ligne du CSV, en commençant à partir de la deuxième ligne
        for (int i = 1; i < lines.length; i++) {
            // Enlever les espaces blancs supplémentaires autour de chaque ligne
            String line = lines[i].trim();

            // Si la ligne est vide, créer une ligne remplie de chaînes vides
            if (line.isEmpty()) {
                continue;
            }

            // Séparer la ligne en colonnes
            String[] columnsInLine = line.split(";");

            // Si la ligne a moins de colonnes que la première ligne, ajouter des colonnes vides
            if (columnsInLine.length < columnNames.size()) {
                String[] newColumns = new String[columnNames.size()];
                System.arraycopy(columnsInLine, 0, newColumns, 0, columnsInLine.length);
                Arrays.fill(newColumns, columnsInLine.length, newColumns.length, ""); // Remplir les colonnes vides avec ""
                columnsInLine = newColumns;
            }

            // Ajouter la ligne traitée à la liste
            List<String> rowList = new ArrayList<>();
            for (String column : columnsInLine) {
                rowList.add(column.isEmpty() ? "" : column); // S'assurer que les colonnes vides sont ajoutées comme chaînes vides
            }
            allRows.add(rowList);
        }

        // Création d'une map pour chaque ligne, où les clés sont les noms des colonnes et les valeurs sont les valeurs des colonnes
        List<Map<String, String>> mappedRows = new ArrayList<>();

        for (List<String> row : allRows) {
            Map<String, String> rowMap = new HashMap<>();
            for (int i = 0; i < columnNames.size(); i++) {
                rowMap.put(columnNames.get(i), row.get(i)); // Map les noms des colonnes aux valeurs
            }
            mappedRows.add(rowMap);
        }

        return mappedRows;
    }



}
