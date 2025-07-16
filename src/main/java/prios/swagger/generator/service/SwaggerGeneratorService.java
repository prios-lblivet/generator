package prios.swagger.generator.service;

import org.springframework.stereotype.Service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.javadoc.Javadoc;

@Service
public class SwaggerGeneratorService {

	public String generateSwaggerYaml(String javaClassContent) {
        String className = extractClassName(javaClassContent);
        String properties = extractClassProperties(javaClassContent);
        
        final String[] tabInfo = {null, null};  // Tableau pour stocker le titre et la description
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
	    String routes = generateRoutes(className);

        return "openapi: 3.0.1\n" +
                "info:\n" +
                "  title: \"" + title + " API\"\n" +
                "  description: \"" + description + "\"\n" +
                "  version: 1.0.0\n\n" +
                routes +
                "components:\n" +
                "  schemas:\n" +
                "    " + className + ":\n" +
                "      type: object\n" +
                "      properties:\n" +
                properties;
    }

	private String generateRoutes(String className) {
	    return "paths:\n" +
	           "  /v1/" + endPointName(className) + "s:\n" +
	           "    get:\n" +
	           "      summary: Récupère la liste des " + className + "\n" +
	           "      description: Récupère la liste des " + className + "\n" +
	           "      operationId: getAll" + className + "\n" +
	           "      tags:\n" +
	           "        - " + className + "\n" +
	           "      parameters:\n" +
	           "        - name: idCompany\n" +
	           "          in: header\n" +
	           "          required: true\n" +
	           "          schema:\n" +
	           "            type: integer\n" +
	           "        - name: idEstablishment\n" +
	           "          in: header\n" +
	           "          required: true\n" +
	           "          schema:\n" +
	           "            type: integer\n" +
	           "        - name: deleteRecord\n" +
	           "          in: query\n" +
	           "          required: false\n" +
	           "          schema:\n" +
	           "            type: string\n" +
	           "            enum: [all, true, false]\n" +
	           "      responses:\n" +
	           "        \"200\":\n" +
	           "          description: Liste des " + className + "s récupérée avec succès\n" +
	           "          content:\n" +
	           "            application/json:\n" +
	           "              schema:\n" +
	           "                type: array\n" +
	           "                items:\n" +
	           "                  $ref: \"#/components/schemas/" + className + "\"\n\n" +
	           "  /v1/" + toCamelCase(className) + "s/{id}:\n" +
	           "    get:\n" +
	           "      summary: Récupère un " + className + " par son id\n" +
	           "      description: Récupère un " + className + " par son id\n" +
	           "      operationId: get" + className + "ById\n" +
	           "      tags:\n" +
	           "        - " + className + "\n" +
	           "      parameters:\n" +
	           "        - name: id\n" +
	           "          in: path\n" +
	           "          required: true\n" +
	           "          schema:\n" +
	           "            type: integer\n" +
	           "        - name: idCompany\n" +
	           "          in: header\n" +
	           "          required: true\n" +
	           "          schema:\n" +
	           "            type: integer\n" +
	           "        - name: idEstablishment\n" +
	           "          in: header\n" +
	           "          required: true\n" +
	           "          schema:\n" +
	           "            type: integer\n" +
	           "      responses:\n" +
	           "        \"200\":\n" +
	           "          description: " + className + " récupéré avec succès\n" +
	           "          content:\n" +
	           "            application/json:\n" +
	           "              schema:\n" +
	           "                $ref: \"#/components/schemas/" + className + "\"\n" +
	           "        \"404\":\n" +
	           "          description: " + className + " non trouvé\n\n";
	}


	public String extractClassName(String javaClassContent) {
        try {
            // Créer une instance de JavaParser
            JavaParser javaParser = new JavaParser();
            
            // Parsing du code Java pour obtenir la CompilationUnit
            CompilationUnit compilationUnit = javaParser.parse(javaClassContent).getResult()
                    .orElseThrow(() -> new ParseException("Invalid Java code"));
            
            // Vérifier si la CompilationUnit contient des types
            if (compilationUnit.getTypes().isEmpty()) {
                throw new IllegalArgumentException("Aucune classe trouvée dans le code Java");
            }

            // Récupérer la première classe dans la CompilationUnit et extraire son nom
            return compilationUnit.getClassByName(compilationUnit.getTypes().get(0).getNameAsString())
                    .map(clazz -> clazz.getNameAsString())
                    .orElse("toto"); // Si aucune classe trouvée, on retourne "toto"
        } catch (ParseException | IllegalArgumentException e) {
            return "errorJava"; // Nom par défaut en cas d'erreur
        }
    }

	public String extractClassProperties(String javaClassContent) {
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
        int maxLength = 255;  // Valeur par défaut pour le maxLength
        String description = null;  // Initialiser la description à null
        
        for (AnnotationExpr annotation : field.getAnnotations()) {
            if (annotation instanceof NormalAnnotationExpr) {
                NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;
                
                // Vérifier l'annotation @Size pour maxLength
                if ("Size".equals(normalAnnotation.getNameAsString())) {
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
                
                if (description == null) {
                	if (field.hasJavaDocComment()) {
                	    Javadoc javadoc = field.getJavadoc().get();
                	    description = javadoc.getDescription().toText().trim();
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
                swaggerProperty += "          type: string\n          maxLength: " + maxLength + "\n          description: " + description + "\n          example: \"" + generateStringExample(fieldName, maxLength) + "\"\n";
                break;
            default:
                // Gestion des types complexes (par exemple, en ajoutant le type directement sans $ref pour les types spéciaux)
                if (isSpecialComplexType(fieldType)) {
                    swaggerProperty += "          type: " + toCamelCase(fieldType) + "Type\n";
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

    private String generateStringExample(String name, int maxLength) {
        StringBuilder sb = new StringBuilder(maxLength);
        
        // Vérifier des mots-clés dans le nom pour déterminer l'exemple
        String baseString = name.toLowerCase().replaceAll("[^a-zA-Z0-9]", ""); // Retirer les caractères spéciaux du nom

        if (baseString.contains("phone")) {
            // Exemple de numéro de téléphone
            sb.append("06 64 67 85 32");
        } else if (baseString.contains("address")) {
            // Exemple d'adresse
            sb.append("123 Rue de l'Exemple 75001 Paris");
        } else if (baseString.contains("mail")) {
            // Exemple d'email
            sb.append("exemple@email.com");} 
        else if (baseString.contains("ean128")) {
            // Exemple de code ean 128
            sb.append("(01)01234567890128");
        } else if (baseString.contains("ean113")) {
            // Exemple de code ean 13
            sb.append("4006381333931");
        } else if (baseString.startsWith("sign")) {
            sb.append("+");
        } else {
            // Si aucun mot-clé n'est trouvé, on tronque simplement la baseString
            int baseLength = baseString.length();
            if (baseLength >= maxLength) {
                sb.append(baseString.substring(0, maxLength)); // Tronquer à maxLength
            } else {
                sb.append(baseString); // Garder tel quel si c'est plus court
            }
        }

        int endIndex = Math.min(sb.toString().length(), maxLength);  // On cherche la valeur minimal pour ne pas couper une chaine plus petite que le maxLength

        return sb.toString().substring(0, endIndex);  // Couper pour s'assurer que la longueur est respectée
    }
    
    private String endPointName(String className) {
    	String endPointName = toCamelCase(className);
    	if (endPointName.endsWith("y")) {
    	    endPointName = endPointName.substring(0, endPointName.length() - 1) + "ie";
    	}
    	return endPointName;
    }
    
    public String replaceApiObjectFieldWithComment(String javaClassContent) {

    	JavaParser javaParser = new JavaParser();
        try {
            CompilationUnit compilationUnit = javaParser.parse(javaClassContent)
                .getResult().orElseThrow(() -> new ParseException("Invalid Java code"));

            compilationUnit.accept(new ModifierVisitor<Void>() {
                @Override
                public Visitable visit(FieldDeclaration field, Void arg) {
                    field.getAnnotationByName("ApiObjectField").ifPresent(annotation -> {
                        String description = "";

                        if (annotation instanceof SingleMemberAnnotationExpr) {
                            description = ((SingleMemberAnnotationExpr) annotation).getMemberValue().toString().replaceAll("^\"|\"$", "");
                        } else if (annotation instanceof NormalAnnotationExpr) {
                            for (MemberValuePair pair : ((NormalAnnotationExpr) annotation).getPairs()) {
                                if (pair.getNameAsString().equals("description")) {
                                    description = pair.getValue().toString().replaceAll("^\"|\"$", "");
                                    break;
                                }
                            }
                        }

                        field.getAnnotations().remove(annotation);

                        if (!description.isBlank()) {
                            field.setJavadocComment(new JavadocComment(description));
                        }
                    });

                    return super.visit(field, arg);
                }
            }, null);

            return compilationUnit.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return javaClassContent;
        }
    }
 
    private String toCamelCase(String className) {
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    public static String toSnakeCase(String className) {
        return className.replaceAll("([a-z])([A-Z])", "$1_$2") // Ajoute un "_" entre les lettres minuscules et majuscules
                        .toLowerCase(); // Convertit en minuscules
    }
}
