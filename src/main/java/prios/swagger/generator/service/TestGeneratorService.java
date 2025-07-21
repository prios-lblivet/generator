package prios.swagger.generator.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

@Service
public class TestGeneratorService {
	
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
	
	public Map<String, String> generate(String javaClassContent, String className) {
        StringBuilder entity = new StringBuilder();
        StringBuilder dto = new StringBuilder();
        
    	String lowerClassName = className.substring(0, 1).toLowerCase() + className.substring(1);
        entity.append(lowerClassName + " = new " + className + "();\n");
        dto.append(lowerClassName + "Dto = new " + className + "Dto();\n");
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
                        // Ignorer "serialVersionUID"
                        if (!field.getVariables().get(0).getNameAsString().equals("serialVersionUID")) {
                        	Map<String, String> setter = generateSetter(field, lowerClassName);
                        	entity.append(setter.get("entity"));
                            dto.append(setter.get("dto"));
                        }
                    });
                }
            });
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Map<String, String> response = new HashMap<>();
        response.put("entity", entity.toString());
        response.put("dto", dto.toString());
        return response;
    } 
	

	private Map<String, String> generateSetter(FieldDeclaration field, String className) {
    	
    	String fieldName = field.getVariables().get(0).getNameAsString();
    	String capitalizedFieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        String fieldType = field.getElementType().asString();
        String entity = "";
        String dto = "";
        
        // Vérifier les annotations sur le champ
        int maxLength = 255;  // Valeur par défaut pour le maxLength
        int maxDigits = 1;
        int maxFractionDigits = 1;
        
        for (AnnotationExpr annotation : field.getAnnotations()) {
            if (annotation instanceof NormalAnnotationExpr) {
                NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;
                String annotationName = normalAnnotation.getNameAsString();
                // Vérifier l'annotation @Size pour maxLength
                if ("Size".equals(annotationName)) {
                    for (MemberValuePair pair : normalAnnotation.getPairs()) {
                        if ("max".equals(pair.getNameAsString())) {
                            maxLength = Integer.parseInt(pair.getValue().toString());
                        }
                    }
                }
                if ("Digits".equals(annotationName)) {
                    for (MemberValuePair pair : normalAnnotation.getPairs()) {
                        switch (pair.getNameAsString()) {
                            case "integer":
                            	maxDigits = Integer.parseInt(pair.getValue().toString());
                                break;
                            case "fraction":
                                maxFractionDigits = Integer.parseInt(pair.getValue().toString());
                                break;
                        }
                    }
                }
            }
        }
        
        switch (fieldType) {
            case "Long":
            	Long randomLong = ThreadLocalRandom.current().nextLong(1, maxDigits);
            	entity += className + ".set" + capitalizedFieldName + "(" + randomLong + "L);\n";
            	dto += className + "Dto.set" + capitalizedFieldName + "(" + randomLong + "L);\n";
                break;
            case "Integer":
            	Integer randomInt = ThreadLocalRandom.current().nextInt(1, maxDigits);
            	entity += className + ".set" + capitalizedFieldName + "(" + randomInt + ");\n";
            	dto += className + "Dto.set" + capitalizedFieldName + "(" + randomInt + ");\n";
                break;  
            case "Double":
            	Double randomDouble = ThreadLocalRandom.current().nextDouble(1.0, maxDigits);
            	entity += className + ".set" + capitalizedFieldName + "(" + randomDouble + ");\n";
            	dto += className + "Dto.set" + capitalizedFieldName + "(" + randomDouble + ");\n";
                break;
            case "Float":
            	Float randomFloat = ThreadLocalRandom.current().nextFloat();
            	entity += className + ".set" + capitalizedFieldName + "(" + randomFloat + ");\n";
            	dto += className + "Dto.set" + capitalizedFieldName + "(" + randomFloat + ");\n";
                break; 
            case "Date":
            	entity += className + ".set" + capitalizedFieldName + "(date);\n";
            	dto += className + "Dto.set" + capitalizedFieldName + "(localDateTime);\n";
                break;
            case "boolean":
            	entity += className + ".set" + capitalizedFieldName + "(true);\n";
            	dto += className + "Dto.set" + capitalizedFieldName + "(true);\n";
                break;
            case "String":
            	String exemple = generateStringExample(fieldName, maxLength);
            	entity += className + ".set" + capitalizedFieldName + "(\"" + exemple + "\");\n";
            	dto += className + "Dto.set" + capitalizedFieldName + "(\"" + exemple + "\");\n";
                break;
            default:
            	entity += className + ".set" + capitalizedFieldName + "(new " + fieldType + "());\n";
            	dto += className + "Dto.set" + capitalizedFieldName + "(new " + fieldType + "Dto());\n";
                break;
        }
        Map<String, String> result = new HashMap<>();
        result.put("entity", entity);
        result.put("dto", dto);
        return result;
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
}
