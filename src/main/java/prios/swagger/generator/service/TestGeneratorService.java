package prios.swagger.generator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
	
	public Map<String, String> generate(String javaClassContent, String javaViewClassContent, boolean deleteRecord, boolean idCompany) {
        Map<String, String> response = new HashMap<>();
		boolean hasView = javaViewClassContent != null && !javaViewClassContent.isEmpty();
        StringBuilder entity = new StringBuilder();
        StringBuilder dto = new StringBuilder();
        StringBuilder entityView = new StringBuilder();
        StringBuilder dtoView = new StringBuilder();
                
        String className = extractClassName(javaClassContent);

		String entityName = className + "Table";
		
        String packageName = extractPackageName(javaClassContent);
    	String lowerClassName = className.substring(0, 1).toLowerCase() + className.substring(1);

        generateEntityAndDto(javaClassContent, "", entity, dto, className, entityName, lowerClassName, "");
        entity.append(System.lineSeparator());
        dto.append(System.lineSeparator());
        generateEntityAndDto(javaClassContent, "", entity, dto, className, entityName, lowerClassName, "2");
        if (hasView) {
    		entityName = className + "View";
    		lowerClassName =  lowerClassName + "View";
    		entityView.append(System.lineSeparator());
    		dtoView.append(System.lineSeparator());
            generateEntityAndDto(javaViewClassContent, javaClassContent, entityView, dtoView, entityName, entityName, lowerClassName, "");
            entityView.append(System.lineSeparator());
            dtoView.append(System.lineSeparator());
            generateEntityAndDto(javaViewClassContent, javaClassContent, entityView, dtoView, entityName, entityName, lowerClassName, "2");
        }

        response.put("entity", entity.toString());
        response.put("dto", dto.toString());
                
        response.put("mapper", generateMapper(entity.toString(), dto.toString(), className, packageName));
        if (hasView) {
            response.put("mapperView", generateMapper(entityView.toString(), dtoView.toString(), className + "View", packageName));
        }

        response.put("service", generateServiceTest(entity, dto, hasView, entityView, dtoView, className, packageName));
        response.put("controller", generateControllerTest(entity, dto, hasView, entityView, dtoView, className, packageName));
        
        return response;
    }

	private void generateEntityAndDto(String javaClassContent, String javaClassContent2, StringBuilder entity, StringBuilder dto,	String className, String entityName, String lowerClassName, String number) {
		entity.append( "		" + lowerClassName + number + " = new " + entityName + "();\n");
        dto.append( "		" + lowerClassName + "Dto" + number + " = new " + className + "Dto();\n");
        
        // pour remettre les champs de la table dans la vue
        if (javaClassContent2 != null && !javaClassContent2.isEmpty()) {
        	try {
                // Créer une instance de JavaParser
                JavaParser javaParser = new JavaParser();
                // Parsing du code Java pour récupérer les propriétés
                CompilationUnit compilationUnit = javaParser.parse(javaClassContent2).getResult()
                    .orElseThrow(() -> new ParseException("Invalid Java code"));

                // Parcours des classes dans le fichier
                compilationUnit.getTypes().forEach(type -> {
                    if (type instanceof ClassOrInterfaceDeclaration) {
                        ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) type;
                        
                        // Parcours des champs de la classe
                        classDecl.getFields().forEach(field -> {       	
                            // Ignorer "serialVersionUID"
                            if (!field.getVariables().get(0).getNameAsString().equals("serialVersionUID")) {
                            	Map<String, String> setter = generateSetter(field, lowerClassName, number);
                            	entity.append(setter.get("entity"));
                                dto.append(setter.get("dto"));
                            }
                        });
                    }
                });
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        
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
                        	Map<String, String> setter = generateSetter(field, lowerClassName, number);
                        	entity.append(setter.get("entity"));
                            dto.append(setter.get("dto"));
                        }
                    });
                }
            });
        } catch (ParseException e) {
            e.printStackTrace();
        }
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
	
	public String extractPackageName(String javaClassContent) {
	    try {
	        // Créer une instance de JavaParser
	        JavaParser javaParser = new JavaParser();
	        
	        // Parsing du code Java pour obtenir la CompilationUnit
	        CompilationUnit compilationUnit = javaParser.parse(javaClassContent).getResult()
	                .orElseThrow(() -> new ParseException("Invalid Java code"));
	        
	        // Vérifier si un package est défini dans la CompilationUnit
	        return compilationUnit.getPackageDeclaration()
	                .map(pkg -> pkg.getNameAsString())
	                .orElse("defaultPackage"); // Nom par défaut si aucun package trouvé

	    } catch (ParseException | IllegalArgumentException e) {
	        return "errorPackage"; // Nom par défaut en cas d'erreur
	    }
	}

	private Map<String, String> generateSetter(FieldDeclaration field, String className, String number) {
    	
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
            	Long randomLong = ThreadLocalRandom.current().nextLong(0, maxDigits);
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(" + randomLong + "L);\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(" + randomLong + "L);\n";
                break;
            case "Integer":
            	Integer randomInt = 8;
            	if (!fieldName.equals("id")) {
            		randomInt = ThreadLocalRandom.current().nextInt(0, maxDigits);
            	}
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(" + randomInt + ");\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(" + randomInt + ");\n";
                break;  
            case "BigDecimal":
            	BigDecimal randomBigDecimal = randomBigDecimal(maxDigits, maxFractionDigits);
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(new BigDecimal(\"" + randomBigDecimal + "\"));\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(" + randomBigDecimal + ");\n";
                break;
            case "Double":
            	Double randomDouble = ThreadLocalRandom.current().nextDouble(1.0, maxDigits);
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(" + randomDouble + ");\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(" + randomDouble + ");\n";
                break;
            case "Float":
            	Float randomFloat = ThreadLocalRandom.current().nextFloat();
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(" + randomFloat + ");\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(" + randomFloat + ");\n";
                break; 
            case "Date":
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(date);\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(localDateTime);\n";
                break;
            case "LocalDateTime":
            	int year = 2025;
            	if (fieldName.toLowerCase().contains("end")) {
            		year = 2026;
            	}
            	int month = ThreadLocalRandom.current().nextInt(1, 13);
            	int day = ThreadLocalRandom.current().nextInt(1, 28);
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(LocalDateTime.of(" + year + ", " + month + ", " + day + ", 0, 0, 0, 0));\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(LocalDateTime.of(" + year + ", " + month + ", " + day + ", 0, 0, 0, 0));\n";
                break;
            case "boolean":
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(true);\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(true);\n";
                break;
            case "String":
            	String exemple = generateStringExample(fieldName, maxLength);
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(\"" + exemple + "\");\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(\"" + exemple + "\");\n";
                break;
            case "HistoryManagementA":
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(historyManagementA);\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(historyManagementADto);\n";
                break;
            default:
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(new " + fieldType + "());\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(new " + fieldType + "Dto());\n";
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
	
	public String generateMapper(String entity, String dto, String className, String packageName) {

		String classNamePlural = toPlural(className);
		String entityName = className + "Table";
		if (className.contains("View")) {
			entityName = className;		
		}
		String packageMapper = packageName.replace("shared", "mapper");
		String domain = extractDomainFromPackage(packageName);
		String lowerClassName = className.substring(0, 1).toLowerCase() + className.substring(1);
		String lowerClassNamePlural = toPlural(lowerClassName);

		return "package " + packageMapper + ";\r\n"
				+ "\r\n"
				+ "import static org.assertj.core.api.Assertions.assertThat;\r\n"
				+ "\r\n"
				+ "import java.time.LocalDateTime;\r\n"
				+ "import java.time.ZoneId;\r\n"
				+ "import java.time.ZonedDateTime;\r\n"
				+ "import java.time.temporal.ChronoUnit;\r\n"
				+ "import java.util.Comparator;\r\n"
				+ "import java.util.Date;\r\n"
				+ "import java.util.List;\r\n"
				+ "import java.util.Optional;\r\n"
				+ "\r\n"
				+ "import org.junit.jupiter.api.BeforeEach;\r\n"
				+ "import org.junit.jupiter.api.Test;\r\n"
				+ "\r\n"
				+ "import com.prios.core.a.common.history.management.HistoryManagementA;\r\n"
				+ "import com.prios.core.a.shared.dto.common.HistoryManagementADto;\r\n"
				+ "import com.prios.core.a.shared.dto." + domain + "." + className + "Dto;\r\n"
				+ "import " + packageName + "." + entityName + ";\r\n"
				+ "\r\n"
				+ "class " + className + "MapperTest {\r\n"
				+ "\r\n"
				+ "	private " + className + "Mapper " + lowerClassName + "Mapper;\r\n"
				+ "\r\n"
				+ "	" + className + "Dto " + lowerClassName + "Dto;\r\n"
				+ "	" + className + "Dto " + lowerClassName + "Dto2;\r\n"
				+ "\r\n"
				+ "	" + entityName + " " + lowerClassName + ";\r\n"
				+ "	" + entityName + " " + lowerClassName + "2;\r\n"
				+ "\r\n"
				+ "	@BeforeEach\r\n"
				+ "	void setUp() {\r\n"
				+ "		" + lowerClassName + "Mapper = new " + className + "MapperImpl();\r\n"
				+ "\r\n"
				+ getHistoryManagement()
				+ "		\r\n"
				+ entity
				+ "\r\n"
				+ dto
				+ "	}\r\n"
				+ "\r\n"
				+ "	@Test\r\n"
				+ "	void test" + className + "DtosTo" + classNamePlural + "() {\r\n"
				+ "		// GIVEN\r\n"
				+ "		List<" + className + "Dto> " + lowerClassName + "Dtos = List.of(" + lowerClassName + "Dto, " + lowerClassName + "Dto2);\r\n"
				+ "		List<" + entityName + "> " + lowerClassNamePlural + "ToCompare = List.of(" + lowerClassName + ", " + lowerClassName + "2);\r\n"
				+ "\r\n"
				+ "		// WHEN\r\n"
				+ "		List<" + entityName + "> " + lowerClassNamePlural + " = " + lowerClassName + "Mapper." + lowerClassName + "DtosTo" + classNamePlural + "(" + lowerClassName + "Dtos);\r\n"
				+ "\r\n"
				+ "		// THEN\r\n"
				+ "		assertThat(" + lowerClassNamePlural + "ToCompare).usingRecursiveComparison()\r\n"
				+ "				.withComparatorForType(Comparator.comparing(d -> d.truncatedTo(ChronoUnit.MILLIS)), LocalDateTime.class)\r\n"
				+ "				.isNotNull().isEqualTo(" + lowerClassNamePlural + ");\r\n"
				+ "	}\r\n"
				+ "\r\n"
				+ "	@Test\r\n"
				+ "	void test" + className + "To" + className + "Dto() {\r\n"
				+ "		// GIVEN\r\n"
				+ "\r\n"
				+ "		// WHEN\r\n"
				+ "		" + className + "Dto " + lowerClassName + "DtoToCompare = " + lowerClassName + "Mapper." + lowerClassName + "To" + className + "Dto(" + lowerClassName + ");\r\n"
				+ "\r\n"
				+ "		// THEN\r\n"
				+ "		assertThat(" + lowerClassName + "DtoToCompare).usingRecursiveComparison()\r\n"
				+ "				.withComparatorForType(Comparator.comparing(d -> d.truncatedTo(ChronoUnit.MILLIS)), LocalDateTime.class)\r\n"
				+ "				.isEqualTo(" + lowerClassName + "Dto);\r\n"
				+ "	}\r\n"
				+ "\r\n"
				+ "	@Test\r\n"
				+ "	void test" + classNamePlural + "To" + className + "Dtos() {\r\n"
				+ "		// GIVEN\r\n"
				+ "		List<" + entityName + "> " + lowerClassNamePlural + " = List.of(" + lowerClassName + ", " + lowerClassName + "2);\r\n"
				+ "		List<" + className + "Dto> " + lowerClassName + "Dtos = List.of(" + lowerClassName + "Dto, " + lowerClassName + "Dto2);\r\n"
				+ "		// WHEN\r\n"
				+ "		List<" + className + "Dto> " + lowerClassName + "DtosToCompare = " + lowerClassName + "Mapper." + lowerClassNamePlural + "To" + className + "Dtos(" + lowerClassNamePlural + ");\r\n"
				+ "\r\n"
				+ "		// THEN\r\n"
				+ "		assertThat(" + lowerClassName + "DtosToCompare).usingRecursiveComparison()\r\n"
				+ "				.withComparatorForType(Comparator.comparing(d -> d.truncatedTo(ChronoUnit.MILLIS)), LocalDateTime.class)\r\n"
				+ "				.isNotNull().isEqualTo(" + lowerClassName + "Dtos);\r\n"
				+ "	}\r\n"
				+ "\r\n"
				+ "	@Test\r\n"
				+ "	void testOptional" + className + "ToOptional" + className + "Dto() {\r\n"
				+ "		// GIVEN\r\n"
				+ "		Optional<" + entityName + "> optional" + className + " = Optional.of(" + lowerClassName + ");\r\n"
				+ "\r\n"
				+ "		// WHEN\r\n"
				+ "		Optional<" + className + "Dto> optional" + className + "Dto = " + lowerClassName + "Mapper\r\n"
				+ "				.optional" + className + "ToOptional" + className + "Dto(optional" + className + ");\r\n"
				+ "\r\n"
				+ "		// THEN\r\n"
				+ "		assertThat(optional" + className + "Dto).usingRecursiveComparison()\r\n"
				+ "				.withComparatorForType(Comparator.comparing(d -> d.truncatedTo(ChronoUnit.MILLIS)), LocalDateTime.class)\r\n"
				+ "				.isNotNull().isEqualTo(Optional.of(" + lowerClassName + "Dto));\r\n"
				+ "	}\r\n"
				+ "\r\n"
				+ "	@Test\r\n"
				+ "	void testOptional" + className + "ToOptional" + className + "DtoEmpty() {\r\n"
				+ "		// GIVEN\r\n"
				+ "		Optional<" + entityName + "> optional" + entityName + " = Optional.empty();\r\n"
				+ "\r\n"
				+ "		// WHEN\r\n"
				+ "		Optional<" + className + "Dto> optional" + className + "Dto = " + lowerClassName + "Mapper\r\n"
				+ "				.optional" + className + "ToOptional" + className + "Dto(optional" + entityName + ");\r\n"
				+ "\r\n"
				+ "		// THEN\r\n"
				+ "     assertThat(optional" + className + "Dto).isEmpty();\r\n"
				+ "	}\r\n"
				+ "\r\n"
				+ "	@Test\r\n"
				+ "	void test" + className + "DtosTo" + classNamePlural + "_emptyList() {\r\n"
				+ "		// GIVEN\r\n"
				+ "		List<" + className + "Dto> " + lowerClassName + "Dtos = List.of();\r\n"
				+ "\r\n"
				+ "		// WHEN\r\n"
				+ "		List<" + entityName + "> " + lowerClassNamePlural + " = " + lowerClassName + "Mapper." + lowerClassName + "DtosTo" + classNamePlural + "(" + lowerClassName + "Dtos);\r\n"
				+ "\r\n"
				+ "		// THEN\r\n"
				+ "     assertThat(" + lowerClassNamePlural + ").isNotNull().isEmpty();\r\n"
				+ "	}\r\n"
				+ "\r\n"
				+ "	@Test\r\n"
				+ "	void test" + className + "To" + className + "Dto_null() {\r\n"
				+ "		// WHEN\r\n"
				+ "		" + className + "Dto " + lowerClassName + "DtoNull = " + lowerClassName + "Mapper." + lowerClassName + "To" + className + "Dto(null);\r\n"
				+ "\r\n"
				+ "		// THEN\r\n"
				+ "     assertThat(" + lowerClassName + "DtoNull).isNull();\r\n"
				+ "	}\r\n"
				+ "\r\n"
				+ "	@Test\r\n"
				+ "	void testOptional" + className + "ToOptional" + className + "Dto_null() {\r\n"
				+ "		// WHEN\r\n"
				+ "		Optional<" + className + "Dto> optional" + className + "Dto = " + lowerClassName + "Mapper.optional" + className + "ToOptional" + className + "Dto(Optional.empty());\r\n"
				+ "\r\n"
				+ "		// THEN\r\n"
				+ "		assertThat(optional" + className + "Dto).isEmpty();\r\n"
				+ "	}\r\n"
				+ "}\r\n"
				+ "";
	}
	
	public String extractDomainFromPackage(String packageName) {
	    if (packageName == null || packageName.isBlank()) {
	        return "unknown";
	    }

	    String prefix = "com.prios.api.a.";
	    if (packageName.startsWith(prefix)) {
	        String remaining = packageName.substring(prefix.length());
	        String[] parts = remaining.split("\\.");
	        if (parts.length > 0) {
	            return parts[0]; // ex: "sale"
	        }
	    }

	    return "unknown";
	}
	
	public String toPlural(String className) {
	    if (className == null || className.isBlank()) {
	        return className;
	    }

	    if (className.endsWith("y") && className.length() > 1 && Character.isLetter(className.charAt(className.length() - 2))) {
	        // Remplacer "y" par "ies"
	        return className.substring(0, className.length() - 1) + "ies";
	    } else {
	        // Ajouter "s" par défaut
	        return className + "s";
	    }
	}


	private String getHistoryManagement() {
		return "		LocalDateTime localDateTime = LocalDateTime.now();\r\n"
        		+ "		ZoneId zoneId = ZoneId.of(\"UTC\");\r\n"
        		+ "		ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);\r\n"
        		+ "		Date date = Date.from(zonedDateTime.toInstant());			\r\n"
        		+ "\r\n"
        		+ "		HistoryManagementA historyManagementA = new HistoryManagementA();\r\n"
        		+ "		historyManagementA.setCreationDate(date); // Exemple d'initialisation\r\n"
        		+ "		historyManagementA.setCreationTime(date);\r\n"
        		+ "		historyManagementA.setProgramCreation(\"ProgramA\");\r\n"
        		+ "		historyManagementA.setUserCreation(\"UserA\");\r\n"
        		+ "\r\n"
        		+ "		HistoryManagementADto historyManagementADto = new HistoryManagementADto();\r\n"
        		+ "		historyManagementADto.setCreationDate(localDateTime); // Exemple d'initialisation\r\n"
        		+ "		historyManagementADto.setCreationTime(localDateTime);\r\n"
        		+ "		historyManagementADto.setProgramCreation(\"ProgramA\");\r\n"
        		+ "		historyManagementADto.setUserCreation(\"UserA\");\r\n";
	}
	
	private String generateServiceTest(StringBuilder entity, StringBuilder dto, boolean hasView,
			StringBuilder entityView, StringBuilder dtoView,
			String className, String packageName) {

		String lowerClassName = className.substring(0, 1).toLowerCase() + className.substring(1);
		String serviceClassName = className + "ServiceImpl";
		String repositoryClassName = className + "Repository";
		String viewRepositoryClassName = className + "ViewRepository";
		String entityName = className + "Table";
		String dtoName = className + "Dto";
		String entityViewName = className + "View";
		String dtoViewName = className + "ViewDto";

		String packageRepository = packageName.replace("shared", "repository");	
		String packageService = packageName.replace("shared", "service");	
		
		String domain = extractDomainFromPackage(packageName);

		StringBuilder serviceTest = new StringBuilder();

		// --- Package & Imports ---
		serviceTest.append("package ").append(packageService).append(";\n\n")
		.append("import static org.assertj.core.api.Assertions.assertThat;\n")
		.append("import static org.mockito.ArgumentMatchers.anyInt;\n")
		.append("import static org.mockito.Mockito.times;\n")
		.append("import static org.mockito.Mockito.verify;\n")
		.append("import static org.mockito.Mockito.when;\n\n")
		.append("import java.time.LocalDateTime;\n")
		.append("import java.time.ZoneId;\n")
		.append("import java.time.ZonedDateTime;\n")
		.append("import java.util.Date;\n")
		.append("import java.util.Optional;\n\n")
		.append("import org.junit.jupiter.api.BeforeEach;\n")
		.append("import org.junit.jupiter.api.Test;\n")
		.append("import org.junit.jupiter.api.extension.ExtendWith;\n")
		.append("import org.mockito.InjectMocks;\n")
		.append("import org.mockito.Mock;\n")
		.append("import org.mockito.junit.jupiter.MockitoExtension;\n\n")
		.append("import ").append(packageRepository).append(".").append(repositoryClassName).append(";\n");

		if (hasView) {
			serviceTest.append("import ").append(packageRepository).append(".").append(viewRepositoryClassName).append(";\n");
		}

		serviceTest.append("import ").append(packageName).append(".").append(entityName).append(";\n")
		.append("import com.prios.core.a.common.history.management.HistoryManagementA;\n")
		.append("import com.prios.core.a.shared.dto.common.HistoryManagementADto;\n")
		.append("import com.prios.core.a.shared.dto.").append(domain).append(".").append(dtoName).append(";\n");

		if (hasView) {
			serviceTest.append("import ").append(packageName).append(".").append(entityViewName).append(";\n")
			.append("import com.prios.core.a.shared.dto.").append(domain).append(".").append(dtoViewName).append(";\n");
		}

		serviceTest.append("\n@ExtendWith(MockitoExtension.class)\n")
		.append("class ").append(serviceClassName).append("Test {\n\n")
		.append("    @InjectMocks\n")
		.append("    ").append(serviceClassName).append(" ").append(lowerClassName).append("Service;\n\n")
		.append("    @Mock\n")
		.append("    private ").append(repositoryClassName).append(" ").append(lowerClassName).append("Repository;\n\n");

		if (hasView) {
			serviceTest.append("    @Mock\n")
			.append("    private ").append(viewRepositoryClassName).append(" ").append(lowerClassName).append("ViewRepository;\n\n");
		}

		serviceTest.append("    ").append(entityName).append(" ").append(lowerClassName).append(";\n")
		.append("    ").append(entityName).append(" ").append(lowerClassName).append("2;\n")
		.append("    ").append(dtoName).append(" ").append(lowerClassName).append("Dto;\n")
		.append("    ").append(dtoName).append(" ").append(lowerClassName).append("Dto2;\n\n");

		if (hasView) {
			serviceTest.append("    ").append(entityViewName).append(" ").append(lowerClassName).append("View;\n")
			.append("    ").append(entityViewName).append(" ").append(lowerClassName).append("View2;\n")
			.append("    ").append(dtoViewName).append(" ").append(lowerClassName).append("ViewDto;\n")
			.append("    ").append(dtoViewName).append(" ").append(lowerClassName).append("ViewDto2;\n\n");
		}

		// --- setUp() ---
		serviceTest.append("    @BeforeEach\n")
		.append("    void setUp() {\n")
		.append(getHistoryManagement())
		.append(System.lineSeparator())
		.append(entity)
		.append(System.lineSeparator())
		.append(dto);

		if (hasView) {
			serviceTest.append(System.lineSeparator())
			.append(entityView)
			.append(System.lineSeparator())
			.append(dtoView)
			.append(System.lineSeparator());
		}

		serviceTest.append("    }\n\n");

		// --- Tests de base : findById / findById_notFound ---
		serviceTest.append("    @Test\n")
		.append("    void testFindById() {\n")
		.append("        when(").append(lowerClassName).append("Repository.findById(anyInt())).thenReturn(Optional.of(").append(lowerClassName).append("));\n\n")
		.append("        Optional<").append(entityName).append("> result = ").append(lowerClassName).append("Service.findById(8);\n\n")
		.append("        verify(").append(lowerClassName).append("Repository, times(1)).findById(8);\n")
		.append("        assertThat(result).isPresent();\n")
		.append("        assertThat(result.get().getId()).isEqualTo(8);\n")
		.append("    }\n\n")

		.append("    @Test\n")
		.append("    void testFindById_notFound() {\n")
		.append("        when(").append(lowerClassName).append("Repository.findById(anyInt())).thenReturn(Optional.empty());\n\n")
		.append("        Optional<").append(entityName).append("> result = ").append(lowerClassName).append("Service.findById(404);\n\n")
		.append("        verify(").append(lowerClassName).append("Repository, times(1)).findById(404);\n")
		.append("        assertThat(result).isNotNull().isEmpty();\n")
		.append("    }\n\n");

		if (hasView) {
			serviceTest.append("    @Test\n")
			.append("    void testFindViewById() {\n")
			.append("        when(").append(lowerClassName).append("ViewRepository.findById(anyInt())).thenReturn(Optional.of(").append(lowerClassName).append("View));\n\n")
			.append("        Optional<").append(entityViewName).append("> result = ").append(lowerClassName).append("Service.findViewById(8);\n\n")
			.append("        verify(").append(lowerClassName).append("ViewRepository, times(1)).findById(8);\n")
			.append("        assertThat(result).isPresent();\n")
			.append("        assertThat(result.get().getId()).isEqualTo(8);\n")
			.append("    }\n\n")

			.append("    @Test\n")
			.append("    void testFindViewById_notFound() {\n")
			.append("        when(").append(lowerClassName).append("ViewRepository.findById(anyInt())).thenReturn(Optional.empty());\n\n")
			.append("        Optional<").append(entityViewName).append("> result = ").append(lowerClassName).append("Service.findViewById(404);\n\n")
			.append("        verify(").append(lowerClassName).append("ViewRepository, times(1)).findById(404);\n")
			.append("        assertThat(result).isNotNull().isEmpty();\n")
			.append("    }\n\n");
		}

		serviceTest.append("}\n");

		return serviceTest.toString();
	}

	private String generateControllerTest(StringBuilder entity, StringBuilder dto, boolean hasView,
			StringBuilder entityView, StringBuilder dtoView,
			String className, String packageName) {

		String lowerClassName = className.substring(0, 1).toLowerCase() + className.substring(1);
		String controllerClassName = className + "ControllerRest";
		String serviceClassName = className + "ServiceImpl";
		String mapperClassName = className + "Mapper";
		String viewMapperClassName = className + "ViewMapper";

		String entityName = className + "Table";
		String dtoName = className + "Dto";
		String entityViewName = className + "View";
		String dtoViewName = className + "ViewDto";		

		String packageMapper = packageName.replace("shared", "mapper");
		String packageService = packageName.replace("shared", "service");
		String packageController = packageName.replace("shared", "controller");		

		String domain = extractDomainFromPackage(packageName);
		
		StringBuilder controllerTest = new StringBuilder();

		// --- Package & Imports ---
		controllerTest.append("package ").append(packageController).append(";\n\n")
		.append("import static org.assertj.core.api.Assertions.assertThat;\n")
		.append("import static org.mockito.ArgumentMatchers.any;\n")
		.append("import static org.mockito.ArgumentMatchers.anyInt;\n")
		.append("import static org.mockito.ArgumentMatchers.anyString;\n")
		.append("import static org.mockito.Mockito.times;\n")
		.append("import static org.mockito.Mockito.verify;\n")
		.append("import static org.mockito.Mockito.when;\n\n")
		.append("import java.time.LocalDateTime;\n")
		.append("import java.time.ZoneId;\n")
		.append("import java.time.ZonedDateTime;\n")
		.append("import java.util.Date;\n")
		.append("import java.util.List;\n")
		.append("import java.util.Optional;\n\n")
		.append("import org.junit.jupiter.api.BeforeEach;\n")
		.append("import org.junit.jupiter.api.Test;\n")
		.append("import org.junit.jupiter.api.extension.ExtendWith;\n")
		.append("import org.mockito.InjectMocks;\n")
		.append("import org.mockito.Mock;\n")
		.append("import org.mockito.junit.jupiter.MockitoExtension;\n")
		.append("import org.springframework.http.HttpStatus;\n")
		.append("import org.springframework.http.ResponseEntity;\n\n")
		.append("import ").append(packageMapper).append(".").append(mapperClassName).append(";\n")
		.append("import ").append(packageService).append(".").append(serviceClassName).append(";\n")
		.append("import ").append(packageName).append(".").append(entityName).append(";\n")
		.append("import com.prios.core.a.common.history.management.HistoryManagementA;\n")
		.append("import com.prios.core.a.shared.dto.common.HistoryManagementADto;\n")
		.append("import com.prios.core.a.shared.dto.").append(domain).append(".").append(dtoName).append(";\n")
		.append("import com.prios.core.a.shared.dto.").append(domain).append(".").append("Abstract").append(className).append("Dto;\n");

		if (hasView) {
			controllerTest.append("import ").append(packageMapper).append(".").append(viewMapperClassName).append(";\n")
			.append("import ").append(packageName).append(".").append(entityViewName).append(";\n")
			.append("import com.prios.core.a.shared.dto.").append(domain).append(".").append(dtoViewName).append(";\n");
		}

		controllerTest.append("\n@ExtendWith(MockitoExtension.class)\n")
		.append("class ").append(controllerClassName).append("Test {\n\n")
		.append("    @Mock\n")
		.append("    ").append(serviceClassName).append(" ").append(lowerClassName).append("Service;\n\n")
		.append("    @Mock\n")
		.append("    ").append(mapperClassName).append(" ").append(lowerClassName).append("Mapper;\n\n");

		if (hasView) {
			controllerTest.append("    @Mock\n")
			.append("    ").append(viewMapperClassName).append(" ").append(lowerClassName).append("ViewMapper;\n\n");
		}

		controllerTest.append("    @InjectMocks\n")
		.append("    ").append(controllerClassName).append(" ").append(lowerClassName).append("ControllerRest;\n\n")
		.append("    ").append(entityName).append(" ").append(lowerClassName).append(";\n")
		.append("    ").append(entityName).append(" ").append(lowerClassName).append("2;\n");

		if (hasView) {
			controllerTest.append("    ").append(entityViewName).append(" ").append(lowerClassName).append("View;\n")
			.append("    ").append(entityViewName).append(" ").append(lowerClassName).append("View2;\n");
		}

		controllerTest.append("    ").append(dtoName).append(" ").append(lowerClassName).append("Dto;\n")
		.append("    ").append(dtoName).append(" ").append(lowerClassName).append("Dto2;\n");

		if (hasView) {
			controllerTest.append("    ").append(dtoViewName).append(" ").append(lowerClassName).append("ViewDto;\n")
			.append("    ").append(dtoViewName).append(" ").append(lowerClassName).append("ViewDto2;\n");
		}

		// --- setUp() ---
		controllerTest.append("\n    @BeforeEach\n")
		.append("    void setUp() {\n")
		.append("        LocalDateTime localDateTime = LocalDateTime.now();\n")
		.append("        ZoneId zoneId = ZoneId.of(\"UTC\");\n")
		.append("        ZonedDateTime zonedDateTime = localDateTime.atZone(zoneId);\n")
		.append("        Date date = Date.from(zonedDateTime.toInstant());\n\n")
		.append("        HistoryManagementA historyManagementA = new HistoryManagementA();\n")
		.append("        historyManagementA.setCreationDate(date);\n")
		.append("        historyManagementA.setCreationTime(date);\n")
		.append("        historyManagementA.setProgramCreation(\"ProgramA\");\n")
		.append("        historyManagementA.setUserCreation(\"UserA\");\n\n")
		.append("        HistoryManagementADto historyManagementADto = new HistoryManagementADto();\n")
		.append("        historyManagementADto.setCreationDate(localDateTime);\n")
		.append("        historyManagementADto.setCreationTime(localDateTime);\n")
		.append("        historyManagementADto.setProgramCreation(\"ProgramA\");\n")
		.append("        historyManagementADto.setUserCreation(\"UserA\");\n\n")
		.append(entity).append("\n")
		.append(dto).append("\n");

		if (hasView) {
			controllerTest.append(entityView).append("\n")
			.append(dtoView).append("\n");
		}

		controllerTest.append("    }\n\n");

		// --- Tests getAll ---
		controllerTest.append("    @Test\n")
		.append("    void testGetAll").append(className).append("With1Result() {\n")
		.append("        List<").append(entityName).append("> entities = List.of(").append(lowerClassName).append(");\n")
		.append("        List<").append(dtoName).append("> dtos = List.of(").append(lowerClassName).append("Dto);\n")
		.append("        when(").append(lowerClassName).append("Service.findAll(any(), anyInt(), anyInt(), any())).thenReturn(entities);\n")
		.append("        when(").append(lowerClassName).append("Mapper.").append(lowerClassName).append("sTo").append(className).append("Dtos(any())).thenReturn(dtos);\n")
		.append("        ResponseEntity<List<Abstract").append(className).append("Dto>> response = ")
		.append(lowerClassName).append("ControllerRest.getAll").append(className).append("(1, 2, null, \"N\", null);\n")
		.append("        verify(").append(lowerClassName).append("Mapper, times(1)).").append(lowerClassName).append("sTo").append(className).append("Dtos(any());\n")
		.append("        assertThat(response).usingRecursiveComparison().isNotNull()\n")
		.append("            .isEqualTo(ResponseEntity.status(HttpStatus.OK).body(dtos));\n")
		.append("    }\n\n");

		controllerTest.append("    @Test\n")
		.append("    void testGetAll").append(className).append("With2Results() {\n")
		.append("        List<").append(entityName).append("> entities = List.of(").append(lowerClassName).append(",").append(lowerClassName).append("2);\n")
		.append("        List<").append(dtoName).append("> dtos = List.of(").append(lowerClassName).append("Dto,").append(lowerClassName).append("Dto2);\n")
		.append("        when(").append(lowerClassName).append("Service.findAll(any(), anyInt(), anyInt(), any())).thenReturn(entities);\n")
		.append("        when(").append(lowerClassName).append("Mapper.").append(lowerClassName).append("sTo").append(className).append("Dtos(any())).thenReturn(dtos);\n")
		.append("        ResponseEntity<List<Abstract").append(className).append("Dto>> response = ")
		.append(lowerClassName).append("ControllerRest.getAll").append(className).append("(1, 2, null, \"N\", null);\n")
		.append("        verify(").append(lowerClassName).append("Mapper, times(1)).").append(lowerClassName).append("sTo").append(className).append("Dtos(any());\n")
		.append("        assertThat(response).usingRecursiveComparison().isNotNull()\n")
		.append("            .isEqualTo(ResponseEntity.status(HttpStatus.OK).body(dtos));\n")
		.append("    }\n\n");

		controllerTest.append("    @Test\n")
		.append("    void testGet").append(className).append("ById() {\n")
		.append("        when(").append(lowerClassName).append("Mapper.").append(lowerClassName).append("To").append(className).append("Dto(any())).thenReturn(").append(lowerClassName).append("Dto);\n")
		.append("        when(").append(lowerClassName).append("Service.findById(anyInt())).thenReturn(Optional.of(").append(lowerClassName).append("));\n")
		.append("        ResponseEntity<Abstract").append(className).append("Dto> response = ")
		.append(lowerClassName).append("ControllerRest.get").append(className).append("ById(1, 2, 3, null);\n")
		.append("        assertThat(response).usingRecursiveComparison().isNotNull()\n")
		.append("            .isEqualTo(ResponseEntity.status(HttpStatus.OK).body(").append(lowerClassName).append("Dto));\n")
		.append("    }\n\n");

		controllerTest.append("    @Test\n")
		.append("    void testGet").append(className).append("ById_notFound() {\n")
		.append("        when(").append(lowerClassName).append("Service.findById(anyInt())).thenReturn(Optional.empty());\n")
		.append("        ResponseEntity<Abstract").append(className).append("Dto> response = ")
		.append(lowerClassName).append("ControllerRest.get").append(className).append("ById(404, 1, 2, null);\n")
		.append("        assertThat(response).usingRecursiveComparison().isEqualTo(ResponseEntity.notFound().build());\n")
		.append("    }\n\n");

		// --- Tests pour la View ---
		if (hasView) {
			controllerTest.append("    @Test\n")
			.append("    void testGetAll").append(className).append("ViewWith1Result() {\n")
			.append("        List<").append(entityViewName).append("> views = List.of(").append(lowerClassName).append("View);\n")
			.append("        List<").append(dtoViewName).append("> viewDtos = List.of(").append(lowerClassName).append("ViewDto);\n")
			.append("        when(").append(lowerClassName).append("Service.findAllView(any(), anyInt(), anyInt(), any())).thenReturn(views);\n")
			.append("        when(").append(lowerClassName).append("ViewMapper.").append(lowerClassName).append("ViewsTo").append(className).append("ViewDtos(any())).thenReturn(viewDtos);\n")
			.append("        ResponseEntity<List<Abstract").append(className).append("Dto>> response = ")
			.append(lowerClassName).append("ControllerRest.getAll").append(className).append("(1, 2, null, \"N\", \"full\");\n")
			.append("        verify(").append(lowerClassName).append("ViewMapper, times(1)).").append(lowerClassName).append("ViewsTo").append(className).append("ViewDtos(any());\n")
			.append("        assertThat(response).usingRecursiveComparison().isNotNull()\n")
			.append("            .isEqualTo(ResponseEntity.status(HttpStatus.OK).body(viewDtos));\n")
			.append("    }\n\n");

			controllerTest.append("    @Test\n")
			.append("    void testGetAll").append(className).append("ViewWith2Results() {\n")
			.append("        List<").append(entityViewName).append("> views = List.of(").append(lowerClassName).append("View,").append(lowerClassName).append("View2);\n")
			.append("        List<").append(dtoViewName).append("> viewDtos = List.of(").append(lowerClassName).append("ViewDto,").append(lowerClassName).append("ViewDto2);\n")
			.append("        when(").append(lowerClassName).append("Service.findAllView(any(), anyInt(), anyInt(), any())).thenReturn(views);\n")
			.append("        when(").append(lowerClassName).append("ViewMapper.").append(lowerClassName).append("ViewsTo").append(className).append("ViewDtos(any())).thenReturn(viewDtos);\n")
			.append("        ResponseEntity<List<Abstract").append(className).append("Dto>> response = ")
			.append(lowerClassName).append("ControllerRest.getAll").append(className).append("(1, 2, null, \"N\",\"full\");\n")
			.append("        verify(").append(lowerClassName).append("ViewMapper, times(1)).").append(lowerClassName).append("ViewsTo").append(className).append("ViewDtos(any());\n")
			.append("        assertThat(response).usingRecursiveComparison().isNotNull()\n")
			.append("            .isEqualTo(ResponseEntity.status(HttpStatus.OK).body(viewDtos));\n")
			.append("    }\n\n");

			controllerTest.append("    @Test\n")
			.append("    void testGet").append(className).append("ViewById() {\n")
			.append("        when(").append(lowerClassName).append("ViewMapper.").append(lowerClassName).append("ViewTo").append(className).append("ViewDto(any())).thenReturn(").append(lowerClassName).append("ViewDto);\n")
			.append("        when(").append(lowerClassName).append("Service.findViewById(anyInt())).thenReturn(Optional.of(").append(lowerClassName).append("View));\n")
			.append("        ResponseEntity<Abstract").append(className).append("Dto> response = ")
			.append(lowerClassName).append("ControllerRest.get").append(className).append("ById(1, 2, 3, \"full\");\n")
			.append("        assertThat(response).usingRecursiveComparison().isNotNull()\n")
			.append("            .isEqualTo(ResponseEntity.status(HttpStatus.OK).body(").append(lowerClassName).append("ViewDto));\n")
			.append("    }\n\n");

			controllerTest.append("    @Test\n")
			.append("    void testGet").append(className).append("ViewById_notFound() {\n")
			.append("        when(").append(lowerClassName).append("Service.findViewById(anyInt())).thenReturn(Optional.empty());\n")
			.append("        ResponseEntity<Abstract").append(className).append("Dto> response = ")
			.append(lowerClassName).append("ControllerRest.get").append(className).append("ById(404, 1, 2, \"full\");\n")
			.append("        assertThat(response).usingRecursiveComparison().isEqualTo(ResponseEntity.notFound().build());\n")
			.append("    }\n\n");
		}

		controllerTest.append("}\n");

		return controllerTest.toString();
	}
	
	private static final Random random = new Random();
	
	public static BigDecimal randomBigDecimal(int maxDigits, int maxFractionDigits) {
        if (maxDigits <= 0) {
            throw new IllegalArgumentException("maxDigits must be > 0");
        }
        if (maxFractionDigits < 0) {
            throw new IllegalArgumentException("maxFractionDigits must be >= 0");
        }

        // Génère la partie entière
        BigDecimal integerPart = BigDecimal.valueOf(random.nextLong(maxDigits == 18 ? Long.MAX_VALUE : (long) Math.pow(10, maxDigits)))
                                           .abs();

        // Génère la partie fractionnaire
        BigDecimal fractionPart = BigDecimal.ZERO;
        if (maxFractionDigits > 0) {
            long fractionValue = (long) (random.nextDouble() * Math.pow(10, maxFractionDigits));
            fractionPart = BigDecimal.valueOf(fractionValue)
                                     .divide(BigDecimal.TEN.pow(maxFractionDigits), maxFractionDigits, RoundingMode.DOWN);
        }

        return integerPart.add(fractionPart);
    }
}
