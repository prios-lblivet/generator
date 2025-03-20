package prios.swagger.generator;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.Size;
import java.lang.reflect.Field;

import java.io.IOException;

@SpringBootApplication
public class GeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeneratorApplication.class, args);
	}

}

@RestController
@RequestMapping("/api/swagger")
@CrossOrigin(origins = "*")
class SwaggerController {

	@PostMapping(value = "/generate", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> generateSwagger(@RequestBody String javaClassContent) {
	    String originalClassName = extractClassName(javaClassContent);
	    
	    // Vérifier si le contenu est une classe Java valide
	    if (originalClassName == null || originalClassName.equals("toto")) {
	        Map<String, String> response = new HashMap<>();
	        response.put("error", "Vous devez envoyer une classe Java");
	        return ResponseEntity.badRequest().body(response);
	    }
	    
	    String swaggerName = toSnakeCase(originalClassName); // Convertir en snake_case
	    String fileName = swaggerName + ".yaml"; // Nom dynamique
	    String swaggerYaml = generateSwaggerYaml(javaClassContent);

	    try {
	    	
	        // Construire la réponse JSON avec le nom de fichier et le contenu YAML
	        Map<String, String> response = new HashMap<>();
	        response.put("fileName", fileName);
	        response.put("swaggerYaml", swaggerYaml);

	        return ResponseEntity.ok(response);
	    } catch (Exception  e) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(Collections.singletonMap("error", "Erreur lors de la génération du Swagger : " + e.getMessage()));
	    }
	}

    private String generateSwaggerYaml(String javaClassContent) {
        String className = extractClassName(javaClassContent);
        String properties = extractClassProperties(javaClassContent);
        
        final String[] tabInfo = {null, null};  // Tableau pour stocker le titre et la description
        String tilte;        
        try {
            // Créer une instance de JavaParser
            JavaParser javaParser = new JavaParser();
            // Parsing du code Java pour récupérer les annotations
            CompilationUnit compilationUnit = javaParser.parse(javaClassContent).getResult()
                    .orElseThrow(() -> new ParseException("Erreur de parsing du code Java : code invalide"));

            // Parcours des classes dans le fichier
            compilationUnit.getTypes().forEach(type -> {
                if (type instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) type;

                    // Parcours des annotations sur la classe
                    for (AnnotationExpr annotation : classDecl.getAnnotations()) {
                        if (annotation instanceof NormalAnnotationExpr) {
                            NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;

                            // Vérifier l'annotation @ApiObject pour récupérer le nom et la description
                            if ("ApiObject".equals(normalAnnotation.getNameAsString())) {
                                for (MemberValuePair pair : normalAnnotation.getPairs()) {
                                    if ("name".equals(pair.getNameAsString())) {
                                    	tabInfo[0] = pair.getValue().toString().replace("\"", "");  // Stocker dans le tableau
                                    }
                                    if ("description".equals(pair.getNameAsString())) {
                                    	tabInfo[1] = pair.getValue().toString().replace("\"", "").replace(":", "");  // Stocker dans le tableau
                                    }
                                }
                            }
                        }
                    }
                }
            });
        } catch (ParseException e) {
            System.err.println("Erreur lors du parsing du fichier Java : " + e.getMessage());
            e.printStackTrace();
        }

        // Si aucune annotation @ApiObject n'est trouvée, utiliser des valeurs par défaut
        String title = tabInfo != null && tabInfo[0] != null ? tabInfo[0] : className;  // Utiliser le nom de la classe par défaut
        String description = tabInfo != null && tabInfo[1] != null ? tabInfo[1] : "Automatically generated API documentation";  // Utiliser le nom de la classe par défaut


        return "openapi: 3.0.1\n" +
                "info:\n" +
                "  title: \"" + title + " API\"\n" +
                "  description: \"" + description + "\"\n" +
                "  version: 1.0.0\n" +
                "components:\n" +
                "  schemas:\n" +
                "    " + className + ":\n" +
                "      type: object\n" +
                "      properties:\n" +
                properties;
    }
    
    private String extractClassName(String javaClassContent) {
        try {
            // Créer une instance de JavaParser
            JavaParser javaParser = new JavaParser();
            
            // Parsing du code Java pour obtenir la CompilationUnit
            CompilationUnit compilationUnit = javaParser.parse(javaClassContent).getResult()
                    .orElseThrow(() -> new ParseException("Invalid Java code"));

            // Récupérer la première classe dans la CompilationUnit et extraire son nom
            return compilationUnit.getClassByName(compilationUnit.getTypes().get(0).getNameAsString())
                    .map(clazz -> clazz.getNameAsString())
                    .orElse("toto"); // Si aucune classe trouvée, on retourne "toto"
        } catch (ParseException e) {
            return "toto"; // Nom par défaut en cas d'erreur
        }
    }
    
    public static String toSnakeCase(String className) {
        return className.replaceAll("([a-z])([A-Z])", "$1_$2") // Ajoute un "_" entre les lettres minuscules et majuscules
                        .toLowerCase(); // Convertit en minuscules
    }

    private String extractClassProperties(String javaClassContent) {
        StringBuilder properties = new StringBuilder();

        try {
            // Créer une instance de JavaParser
            JavaParser javaParser = new JavaParser();
            // Parsing du code Java pour récupérer les propriétés
            CompilationUnit compilationUnit = javaParser.parse(javaClassContent).getResult()
                .orElseThrow(() -> new ParseException("Invalid Java code"));

            // Parcours des classes dans le fichier
            compilationUnit.getTypes().forEach(type -> {
                if (type instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) type;
                    
                    // Parcours des champs de la classe
                    classDecl.getFields().forEach(field -> {
                        // Passage direct du champ (Field) à la fonction generateSwaggerProperty
                        String swaggerProperty = generateSwaggerProperty(field);
                        
                        // Ignorer "serialVersionUID"
                        if (!field.getVariables().get(0).getNameAsString().equals("serialVersionUID")) {
                            properties.append(swaggerProperty);
                        }
                    });
                }
            });
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return properties.toString();
    }  

    private String generateSwaggerProperty(FieldDeclaration field) {
    	String fieldName = field.getVariables().get(0).getNameAsString();
        String fieldType = field.getElementType().asString();
        String swaggerProperty = "        " + fieldName + ":\n";
        
        // Vérifier les annotations sur le champ
        boolean hasSizeAnnotation = false;
        int maxLength = 255;  // Valeur par défaut pour le maxLength
        String description = null;  // Initialiser la description à null
        
        for (AnnotationExpr annotation : field.getAnnotations()) {
            if (annotation instanceof NormalAnnotationExpr) {
                NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;
                
                // Vérifier l'annotation @Size pour maxLength
                if ("Size".equals(normalAnnotation.getNameAsString())) {
                    hasSizeAnnotation = true;
                    for (MemberValuePair pair : normalAnnotation.getPairs()) {
                        if ("max".equals(pair.getNameAsString())) {
                            maxLength = Integer.parseInt(pair.getValue().toString());
                        }
                    }
                }

                // Vérifier l'annotation @ApiObjectField pour la description
                if ("ApiObjectField".equals(normalAnnotation.getNameAsString())) {
                    for (MemberValuePair pair : normalAnnotation.getPairs()) {
                        if ("description".equals(pair.getNameAsString())) {
                            description = pair.getValue().toString().replace("\"", "").replace(":", "");  // Retirer les guillemets
                        }
                    }
                }
            }
        }
        
        switch (fieldType) {
            case "Long":
                swaggerProperty += "          type: integer\n          format: int64\n          description: " + description + "\n          example: 12345\n";
                break;
            case "Integer":
                swaggerProperty += "          type: integer\n          format: int32\n          description: \"" + description + "\"\n          example: 100\n";
                break;  
            case "Double":
                swaggerProperty += "          type: number\n          format: double\n          description: " + description + "\n          example: 99.99\n";
                break;
            case "Float":
                swaggerProperty += "          type: number\n          format: float\n          description: " + description + "\n          example: 99.99\n";
                break; 
            case "Date":
                swaggerProperty += "          type: string\n          example: '2025-03-19T10:00:00Z'\n          description: " + description + "\n          format: date-time\n";
                break;
            case "boolean":
                swaggerProperty += "          type: boolean\n          description: " + description + "\n          example: 'true'\n";
                break;
            case "String":
                swaggerProperty += "          type: string\n          maxLength: " + maxLength + "\n          description: " + description + "\n          example: '" + generateStringExample(fieldName, maxLength) + "'\n";
                break;
            default:
                // Gestion des types complexes (par exemple, en ajoutant le type directement sans $ref pour les types spéciaux)
                if (isSpecialComplexType(fieldType)) {
                    swaggerProperty += "          type: " + toLowerCaseFirstLetter(fieldType) + "Type\n";
                } else {
                    swaggerProperty += "          $ref: '#/components/schemas/" + fieldType + "'\n";
                }
                break;
        }
        return swaggerProperty;
    }

    private boolean isSpecialComplexType(String type) {
        // Liste des types complexes définis directement
        String[] specialTypes = {
            "CommonExchange", "BuildingExchange", "BuildingsDeliveryOrderExchange", "CellsTourLoadingExchange",
            "DeliveredThirdPartyTourLoadingExchange", "DeliveryInformationDeliveryOrderExchange", "DeliveryInformationExchange",
            "GeneralInformationThirdParty", "QuantityTourLoadingExchange", "SiloExchange", "SilosDeliveryOrderExchange",
            "SupplementationListTourExchange", "SupplementationTourExchange", "SupplementationTourLoadingExchange",
            "VehicleBoxesTourLoadingExchange", "VeterinarianDeliveryOrderExchange", "HistoryManagementA"
        };
        for (String specialType : specialTypes) {
            if (specialType.equals(type)) {
                return true;
            }
        }
        return false;
    }
        
    private String toLowerCaseFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
    
    private String generateStringExample(String name, int maxLength) {
        StringBuilder sb = new StringBuilder(maxLength);
        
        // Vérifier des mots-clés dans le nom pour déterminer l'exemple
        String baseString = name.toLowerCase().replaceAll("[^a-zA-Z0-9]", ""); // Retirer les caractères spéciaux du nom

        if (baseString.contains("phone")) {
            // Exemple de numéro de téléphone
            sb.append("06 64 67 85 32");
        } else if (baseString.contains("address")) {
            // Exemple d'adresse
            sb.append("123 Rue de l'Exemple, 75001 Paris");
        } else if (baseString.contains("mail")) {
            // Exemple d'email
            sb.append("exemple@email.com");
        } else if (baseString.startsWith("sign")) {
            sb.append("+");
        } else {
            // Si aucun mot-clé n'est trouvé, on génère une chaîne répétée basée sur le nom du champ
            int baseLength = baseString.length();
            for (int i = 0; i < maxLength; i++) {
                sb.append(baseString.charAt(i % baseLength)); // Répéter les lettres du nom du champ
            }
        }

        int endIndex = Math.min(sb.toString().length(), maxLength);  // On cherche la valeur minimal pour ne pas couper une chaine plus petite que le maxLength

        return sb.toString().substring(0, endIndex);  // Couper pour s'assurer que la longueur est respectée
    }
}