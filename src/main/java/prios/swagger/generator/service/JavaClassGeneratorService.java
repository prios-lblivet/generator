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
import com.github.javaparser.javadoc.Javadoc;

@Service
public class JavaClassGeneratorService {
	
	private static final Random random = new Random();

	public Map<String, String> generate(String javaClassContent, String javaViewClassContent, boolean deleteRecord,
			boolean idCompany) {
		Map<String, String> response = new HashMap<>();
		boolean hasView = javaViewClassContent != null && !javaViewClassContent.isEmpty();

		String className = extractClassName(javaClassContent);
		String packageName = extractPackageName(javaClassContent);
		String api = extractDomainFromPackage(packageName);
		String entityName = className + "Table";
		String lowerClassName = className.substring(0, 1).toLowerCase() + className.substring(1);
		String lowerClassNamePlural = toPlural(lowerClassName);
		String classNamePlural = toPlural(className);
		String classNameImport = classNameToPackage(className);
		
		StringBuilder entity = new StringBuilder();
        StringBuilder dto = new StringBuilder();
        StringBuilder entityView = new StringBuilder();
        StringBuilder dtoView = new StringBuilder();
        generateEntityAndDto(javaClassContent, "", entity, dto, className, entityName, lowerClassName, "");
        entity.append(System.lineSeparator());
        dto.append(System.lineSeparator());
        generateEntityAndDto(javaClassContent, "", entity, dto, className, entityName, lowerClassName, "2");
        if (hasView) {
    		entityView.append(System.lineSeparator());
    		dtoView.append(System.lineSeparator());
            generateEntityAndDto(javaViewClassContent, javaClassContent, entityView, dtoView, className + "View", className + "View", lowerClassName + "View", "");
            entityView.append(System.lineSeparator());
            dtoView.append(System.lineSeparator());
            generateEntityAndDto(javaViewClassContent, javaClassContent, entityView, dtoView, className + "View", className + "View", lowerClassName + "View", "2");
        }

		response.put("mapper", generateMapper(api, classNameImport, className, classNamePlural, lowerClassName,
				lowerClassNamePlural, entityName));
		response.put("mapperTest", generateMapperTest(api, classNameImport, className, classNamePlural, lowerClassName,
				lowerClassNamePlural, entityName, entity.toString(), dto.toString()));
		if (hasView) {
			response.put("mapperView", generateMapper(api, classNameImport, className + "View", className + "Views", lowerClassName + "View",
					lowerClassName + "Views", className + "View"));

			response.put("mapperViewTest", generateMapperTest(api, classNameImport, className + "View", className + "Views", lowerClassName + "View",
					lowerClassName + "Views", className + "View", entityView.toString(), dtoView.toString()));

		}
		response.put("repository", generateRepository(api, classNameImport, className));
		response.put("service", generateService(api, classNameImport, className, lowerClassName, lowerClassNamePlural,
				entityName, hasView, deleteRecord, idCompany));
		response.put("serviceImpl",
				generateServiceImpl(api, classNameImport, className, lowerClassName, hasView, deleteRecord, idCompany));		
		response.put("serviceImplTest",
				generateServiceTest(api, classNameImport, className, classNamePlural, lowerClassName,
						lowerClassNamePlural, entityName, entity.toString(), entityView.toString(), hasView, deleteRecord, idCompany));
		response.put("controller", generateController(api, classNameImport, className, classNamePlural, lowerClassName,
				lowerClassNamePlural, entityName, hasView, deleteRecord, idCompany));
		response.put("controllerTest",
						generateControllerTest(api, classNameImport, className, classNamePlural, lowerClassName,
								lowerClassNamePlural, entityName, entity.toString(), entityView.toString(), dto.toString(), dtoView.toString(), hasView, deleteRecord, idCompany));
		
		response.put("feign", generateFeignClient(api, classNameImport, className, classNamePlural, lowerClassName,
				lowerClassNamePlural, entityName, hasView, deleteRecord, idCompany));
		
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

                    if (classDecl.getJavadoc().isPresent()) {
                        Javadoc javadoc = classDecl.getJavadoc().get();
                        tabInfo[0] = className;
                        tabInfo[1] = javadoc.getDescription().toText(); // uniquement le texte descriptif
                    } else {                    	
						// Parcours des annotations sur la classe
						for (AnnotationExpr annotation : classDecl.getAnnotations()) {
							if (annotation instanceof NormalAnnotationExpr) {
								NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;

								// Vérifier l'annotation @ApiObject pour récupérer le nom et la description
								if ("ApiObject".equals(normalAnnotation.getNameAsString())) {
									for (MemberValuePair pair : normalAnnotation.getPairs()) {
										if ("name".equals(pair.getNameAsString())) {
											tabInfo[0] = pair.getValue().toString().replace("\"", ""); // Stocker dans
																										// le tableau
										}
										if ("description".equals(pair.getNameAsString())) {
											tabInfo[1] = pair.getValue().toString().replace("\"", "").replace(":", "");
										}
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
        
		String title = tabInfo != null && tabInfo[0] != null ? tabInfo[0] : className;  // Utiliser le nom de la classe par défaut
	    String description = tabInfo != null && tabInfo[1] != null ? tabInfo[1] : title;  // Utiliser le nom de la classe par défaut
		    
		response.put("swagger", generateSwagger(api, classNameImport, className, classNamePlural, lowerClassName,
				lowerClassNamePlural, entityName, hasView, deleteRecord, idCompany, title, description, javaClassContent, javaViewClassContent));

		return response;
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
					.map(clazz -> clazz.getNameAsString()).orElse("toto"); // Si aucune classe trouvée, on retourne
																			// "toto"
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
			return compilationUnit.getPackageDeclaration().map(pkg -> pkg.getNameAsString()).orElse("defaultPackage");
		} catch (ParseException | IllegalArgumentException e) {
			return "errorPackage"; // Nom par défaut en cas d'erreur
		}
	}

	public String classNameToPackage(String className) {
		if (className == null || className.isEmpty()) {
			return "";
		}

		// On parcourt chaque caractère pour détecter les majuscules
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < className.length(); i++) {
			char c = className.charAt(i);
			if (Character.isUpperCase(c)) {
				if (i != 0) {
					sb.append('.');
				}
				sb.append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public String generateMapper(String api, String classNameImport, String className, String classNamePlural, String lowerClassName,
			String lowerClassNamePlural, String entityName) {

		StringBuilder mapperBuilder = new StringBuilder();

		// ✅ Package
		mapperBuilder.append("package com.prios.api.a.").append(api).append(".mapper.").append(classNameImport)
				.append(";\n\n");

		// ✅ Imports
		mapperBuilder.append("import java.util.List;\n").append("import java.util.Optional;\n\n")
				.append("import org.mapstruct.Mapper;\n\n").append("import com.prios.api.a.").append(api)
				.append(".shared.").append(classNameImport).append(".").append(entityName).append(";\n")
				.append("import com.prios.core.a.shared.dto.").append(api).append(".").append(className)
				.append("Dto;\n\n");

		// ✅ Déclaration interface
		mapperBuilder.append("@Mapper\n").append("public interface ").append(className).append("Mapper {\n\n");

		// ✅ Méthodes de mapping
		mapperBuilder.append("    List<").append(entityName).append("> ").append(lowerClassName).append("DtosTo")
				.append(classNamePlural).append("(List<").append(className).append("Dto> ").append(lowerClassName)
				.append("Dtos);\n\n");

		mapperBuilder.append("    ").append(className).append("Dto ").append(lowerClassName).append("To")
				.append(className).append("Dto(").append(entityName).append(" ").append(lowerClassName)
				.append(");\n\n");

		mapperBuilder.append("    List<").append(className).append("Dto> ").append(lowerClassNamePlural).append("To")
				.append(className).append("Dtos(List<").append(entityName).append("> ").append(lowerClassNamePlural)
				.append(");\n\n");

		// ✅ Méthode Optional
		mapperBuilder.append("    default Optional<").append(className).append("Dto> optional").append(className)
				.append("ToOptional").append(className).append("Dto(Optional<").append(entityName).append("> optional")
				.append(className).append(") {\n").append("        return optional").append(className)
				.append(".map(this::").append(lowerClassName).append("To").append(className).append("Dto);\n")
				.append("    }\n\n");

		mapperBuilder.append("}\n");
		return mapperBuilder.toString();
	}
	
	public String generateRepository(String api, String classNameImport, String className) {

		StringBuilder repositoryBuilder = new StringBuilder();

		// ✅ Package
		repositoryBuilder.append("package com.prios.api.a.").append(api).append(".repository.").append(classNameImport)
				.append(";\n\n");

		// ✅ Imports
		repositoryBuilder.append("import org.springframework.data.jpa.repository.JpaRepository;\n")
				.append("import org.springframework.data.jpa.repository.JpaSpecificationExecutor;\n\n");

		repositoryBuilder.append("import com.prios.api.a.").append(api).append(".shared.").append(classNameImport)
				.append(".").append(className).append("Table;\n\n");

		// ✅ Déclaration interface
		repositoryBuilder.append("public interface ").append(className).append("Repository extends JpaRepository<")
				.append(className).append("Table, Integer>, JpaSpecificationExecutor<").append(className).append("Table> {\n");

		repositoryBuilder.append("}");

		return repositoryBuilder.toString();
	}

	public String generateService(String api, String classNameImport, String className, String lowerClassName,
			String lowerClassNamePlural, String entityName, boolean hasView, boolean deleteRecord, boolean idCompany) {

		StringBuilder serviceBuilder = new StringBuilder();

		// ✅ Package
		serviceBuilder.append("package com.prios.api.a.").append(api).append(".service.").append(classNameImport)
				.append(";\n\n");

		// ✅ Imports
		serviceBuilder.append("import java.util.List;\n").append("import java.util.Optional;\n\n");

		serviceBuilder.append("import com.prios.api.a.").append(api).append(".shared.").append(classNameImport)
				.append(".").append(className).append("Table;\n");

		if (hasView) {
			serviceBuilder.append("import com.prios.api.a.").append(api).append(".shared.").append(classNameImport)
					.append(".").append(className).append("View;\n");
		}

		serviceBuilder.append("import com.prios.tools.util.criteria.SearchCriteria;\n\n");

		// ✅ Déclaration interface
		serviceBuilder.append("public interface ").append(className).append("Service {\n\n");

		// ✅ findAll
		serviceBuilder.append("    List<").append(className)
				.append("Table> findAll(final List<SearchCriteria> requestParams");

		if (idCompany) {
			serviceBuilder.append(", final int idCompany, final int idEstablishment");
		}
		if (deleteRecord) {
			serviceBuilder.append(", final String deleteRecord");
		}

		serviceBuilder.append(");\n\n");

		// ✅ findAllView si hasView
		if (hasView) {
			serviceBuilder.append("    List<").append(className)
					.append("View> findAllView(final List<SearchCriteria> requestParams");
			if (idCompany) {
				serviceBuilder.append(", final int idCompany, final int idEstablishment");
			}
			if (deleteRecord) {
				serviceBuilder.append(", final String deleteRecord");
			}
			serviceBuilder.append(");\n\n");
		}

		// ✅ findById
		serviceBuilder.append("    Optional<").append(className).append("Table> findById(final int id);\n\n");

		// ✅ findViewById si hasView
		if (hasView) {
			serviceBuilder.append("    Optional<").append(className).append("View> findViewById(final int id);\n\n");
		}

		serviceBuilder.append("}");

		return serviceBuilder.toString();
	}

	public String generateServiceImpl(String api, String classNameImport, String className, String lowerClassName,
			boolean hasView, boolean deleteRecord, boolean idCompany) {

		StringBuilder serviceImplBuilder = new StringBuilder();

		// ✅ PACKAGE
		serviceImplBuilder.append("package com.prios.api.a.").append(api).append(".service.").append(classNameImport)
				.append(";\n\n");

		// ✅ IMPORTS
		serviceImplBuilder.append("import java.util.List;\n").append("import java.util.Optional;\n\n")
				.append("import org.springframework.stereotype.Service;\n")
				.append("import org.springframework.web.server.ResponseStatusException;\n\n")
				.append("import com.prios.api.a.").append(api).append(".repository.").append(classNameImport)
				.append(".").append(className).append("Repository;\n");

		if (hasView) {
			serviceImplBuilder.append("import com.prios.api.a.").append(api).append(".repository.")
					.append(classNameImport).append(".").append(className).append("ViewRepository;\n");
		}

		serviceImplBuilder.append("import com.prios.api.a.").append(api).append(".shared.").append(classNameImport)
				.append(".").append(className).append("Table;\n");

		if (hasView) {
			serviceImplBuilder.append("import com.prios.api.a.").append(api).append(".shared.").append(classNameImport)
					.append(".").append(className).append("View;\n");
		}

		serviceImplBuilder.append("import com.prios.core.a.util.criteria.specification.table");
		if (hasView) {
			serviceImplBuilder.append(".view");
		}
		serviceImplBuilder.append(".Table");
		if (hasView) {
			serviceImplBuilder.append("View");
		}
		if (idCompany) {
			serviceImplBuilder.append("ManagementLevel");
		}
		if (deleteRecord) {
			serviceImplBuilder.append("DeleteRecord");
		}
		serviceImplBuilder.append("Specification;\n").append("import com.prios.tools.util.criteria.SearchCriteria;\n")
				.append("import com.prios.tools.util.criteria.specification.PriosParams;\n\n")
				.append("import lombok.RequiredArgsConstructor;\n\n");

		// ✅ CLASS DECLARATION
		serviceImplBuilder.append("@RequiredArgsConstructor\n").append("@Service\n").append("public class ")
				.append(className).append("ServiceImpl extends Table");
		if (hasView) {
			serviceImplBuilder.append("View");
		}
		if (idCompany) {
			serviceImplBuilder.append("ManagementLevel");
		}
		if (deleteRecord) {
			serviceImplBuilder.append("DeleteRecord");
		}
		serviceImplBuilder.append("Specification<").append(className).append("Table");

		if (hasView) {
			serviceImplBuilder.append(",").append(className).append("View");
		}

		serviceImplBuilder.append("> implements ").append(className).append("Service {\n\n");

		// ✅ FIELDS
		serviceImplBuilder.append("    private final ").append(className).append("Repository ")
				.append(lowerClassName).append("Repository;\n\n");

		if (hasView) {
			serviceImplBuilder.append("    private final ").append(className)
					.append("ViewRepository ").append(lowerClassName).append("ViewRepository;\n\n");
		}

		// ✅ findAll
		serviceImplBuilder.append("    @Override\n").append("    public List<").append(className)
				.append("Table> findAll(List<SearchCriteria> requestParams");
		if (idCompany) {
			serviceImplBuilder.append(", int idCompany, int idEstablishment");
		}
		if (deleteRecord) {
			serviceImplBuilder.append(", String deleteRecord");
		}
		serviceImplBuilder.append(") {\n")
				.append("        PriosParams params = new PriosParams(");
		if (idCompany) {
			serviceImplBuilder.append("idCompany, idEstablishment, ");
		}
		serviceImplBuilder.append("requestParams");
		if (deleteRecord) {
			serviceImplBuilder.append(", deleteRecord");
		}
		serviceImplBuilder.append(");\n        return ").append(lowerClassName)
				.append("Repository.findAll(table(params));\n").append("    }\n\n");

		// ✅ findAllView (si hasView)
		if (hasView) {
			serviceImplBuilder.append("    @Override\n").append("    public List<").append(className)
					.append("View> findAllView(List<SearchCriteria> requestParams");
			if (idCompany) {
				serviceImplBuilder.append(", int idCompany,\n, int idEstablishment");
			}
			if (deleteRecord) {
				serviceImplBuilder.append(", String deleteRecord");
			}
			serviceImplBuilder.append(") { \n").append("        PriosParams params = new PriosParams(");
			if (idCompany) {
				serviceImplBuilder.append("idCompany,idEstablishment,");
			}
			serviceImplBuilder.append("requestParams");
			if (deleteRecord) {
				serviceImplBuilder.append(",deleteRecord");
			}
			serviceImplBuilder.append(");\n        return ").append(lowerClassName)
					.append("ViewRepository.findAll(view(params));\n").append("    }\n\n");
		}

		// ✅ findById
		serviceImplBuilder.append("    @Override\n").append("    public Optional<").append(className)
				.append("Table> findById(int id) {\n").append("        return ").append(lowerClassName)
				.append("Repository.findById(id);\n").append("    }\n\n");

		// ✅ findViewById
		if (hasView) {
			serviceImplBuilder.append("    @Override\n").append("    public Optional<").append(className)
					.append("View> findViewById(int id) {\n").append("        return ").append(lowerClassName)
					.append("ViewRepository.findById(id);\n").append("    }\n\n");
		}

		serviceImplBuilder.append("}\n");

		return serviceImplBuilder.toString();
	}

	public String generateController(String api, String classNameImport, String className, String classNamePlural,
			String lowerClassName, String lowerClassNamePlural, String entityName, boolean hasView,
			boolean deleteRecord, boolean idCompany) {
		StringBuilder controllerBuilder = new StringBuilder();

		// Package
		controllerBuilder.append("package com.prios.api.a.").append(api).append(".controller.").append(classNameImport)
				.append(";\n\n");

		// Imports de base
		controllerBuilder.append("import java.util.ArrayList;\n").append("import java.util.List;\n")
				.append("import java.util.Map;\n\n").append("import javax.validation.Valid;\n")
				.append("import javax.validation.constraints.NotNull;\n\n")
				.append("import org.springframework.http.ResponseEntity;\n")
				.append("import org.springframework.web.bind.annotation.RestController;\n\n");

		// Imports spécifiques
		controllerBuilder.append("import com.prios.api.a.").append(api).append(".mapper.").append(classNameImport)
				.append(".").append(className).append("Mapper;\n");

		if (hasView) {
			controllerBuilder.append("import com.prios.api.a.").append(api).append(".mapper.").append(classNameImport)
					.append(".").append(className).append("ViewMapper;\n");
		}

		controllerBuilder.append("import com.prios.api.a.").append(api).append(".service.").append(classNameImport)
				.append(".").append(className).append("Service;\n");

		controllerBuilder.append("import com.prios.core.a.controller.").append(api).append(".").append(className)
				.append("Controller;\n");
		controllerBuilder.append("import com.prios.core.a.shared.dto.").append(api).append(".");
		if (hasView) {
			controllerBuilder.append("Abstract");
		}
		controllerBuilder.append(className).append("Dto;\n");
		controllerBuilder.append("import com.prios.tools.util.MethodUtils;\n")
				.append("import com.prios.tools.util.StringUtils;\n")
				.append("import com.prios.tools.util.criteria.SearchUtils;\n\n");

		// Annotations de classe
		controllerBuilder.append("import lombok.RequiredArgsConstructor;\n\n");
		controllerBuilder.append("@RequiredArgsConstructor\n");
		controllerBuilder.append("@RestController\n");
		controllerBuilder.append("public class ").append(className).append("ControllerRest implements ")
				.append(className).append("Controller {\n\n");

		// Déclaration des services et mappers
		controllerBuilder.append("    private final ").append(className).append("Service ").append(lowerClassName)
				.append("Service;\n");
		controllerBuilder.append("    private final ").append(className).append("Mapper ").append(lowerClassName)
				.append("Mapper;\n");
		if (hasView) {
			controllerBuilder.append("    private final ").append(className).append("ViewMapper ")
					.append(lowerClassName).append("ViewMapper;\n");
		}
		controllerBuilder.append("\n");

		// Méthode getAll
		controllerBuilder.append("    @Override\n");
		controllerBuilder.append("    public ResponseEntity<List<");
		if (hasView) {
			controllerBuilder.append("Abstract");
		}
		controllerBuilder.append(className).append("Dto>> getAll").append(classNamePlural).append("(");
		controllerBuilder.append("@NotNull Integer idCompany, @NotNull Integer idEstablishment");
	
		if (hasView) {
			controllerBuilder.append(", @Valid String detail");
		}
		if (deleteRecord) {
			controllerBuilder.append(", @Valid String deleteRecord");
		}
		controllerBuilder.append(", @Valid Map<String, Object> criteriaParam");
		controllerBuilder.append(") {\n");

		if (hasView) {
			controllerBuilder.append("        if(StringUtils.isFull(detail)) {\n")
					.append("            return ResponseEntity.ok(new ArrayList<>(\n").append("                ")
					.append(lowerClassName).append("ViewMapper.").append(lowerClassName).append("ViewsTo")
					.append(className).append("ViewDtos(\n").append("                    ").append(lowerClassName)
					.append("Service.findAllView(SearchUtils.getRequestParams(this, MethodUtils.getMethodName(), criteriaParam)");
			if (idCompany) {
				controllerBuilder.append(", idCompany, idEstablishment");
			}
				
			if (deleteRecord)
				controllerBuilder.append(", deleteRecord");
			controllerBuilder.append("))));\n").append("        }\n");
		}

		controllerBuilder.append("        return ResponseEntity.ok(new ArrayList<>(\n").append("            ")
				.append(lowerClassName).append("Mapper.").append(lowerClassNamePlural).append("To").append(className)
				.append("Dtos(\n").append("                ").append(lowerClassName)
				.append("Service.findAll(SearchUtils.getRequestParams(this, MethodUtils.getMethodName(), criteriaParam)");
		if (idCompany)
			controllerBuilder.append(", idCompany, idEstablishment");
		if (deleteRecord)
			controllerBuilder.append(", deleteRecord");
		controllerBuilder.append("))));\n");
		controllerBuilder.append("    }\n\n");

		// Méthode getById
		controllerBuilder.append("    @Override\n");
		controllerBuilder.append("    public ResponseEntity<");
		if (hasView) {
			controllerBuilder.append("Abstract");
		}
		controllerBuilder.append(className).append("Dto> get").append(className).append("ById(@NotNull Integer id, ");
		controllerBuilder.append("@NotNull Integer idCompany, @NotNull Integer idEstablishment");
		if (hasView)
			controllerBuilder.append(", @Valid String detail");
		controllerBuilder.append(") {\n");

		if (hasView) {
			controllerBuilder.append("        if(StringUtils.isFull(detail)) {\n").append("            return ")
					.append(lowerClassName).append("Service.findViewById(id)\n").append("                .map(")
					.append(lowerClassName).append("ViewMapper::").append(lowerClassName).append("ViewTo")
					.append(className).append("ViewDto)\n").append("                .map(Abstract").append(className)
					.append("Dto.class::cast)\n").append("                .map(ResponseEntity::ok)\n")
					.append("                .orElse(ResponseEntity.notFound().build());\n").append("        }\n\n");
		}

		controllerBuilder.append("        return ").append(lowerClassName).append("Service.findById(id)\n")
				.append("            .map(").append(lowerClassName).append("Mapper::").append(lowerClassName)
				.append("To").append(className).append("Dto)\n");
		if (hasView) {
			controllerBuilder.append("            .map(Abstract").append(className).append("Dto.class::cast)\n");
		}
		controllerBuilder.append("            .map(ResponseEntity::ok)\n")
				.append("            .orElse(ResponseEntity.notFound().build());\n");
		controllerBuilder.append("    }\n");

		controllerBuilder.append("}\n");

		return controllerBuilder.toString();
	}

	public String generateSwagger(String api, String classNameImport, String className, String classNamePlural,
			String lowerClassName, String lowerClassNamePlural, String entityName, boolean hasView,
			boolean deleteRecord, boolean idCompany, String title, String description, String javaClass, String viewClass) {

		StringBuilder swaggerBuilder = new StringBuilder();

		swaggerBuilder.append("openapi: 3.0.1\n");
		swaggerBuilder.append("info:\n");
		swaggerBuilder.append("  title: \"").append(title).append(" API\"\n");
		swaggerBuilder.append("  description: \"").append(description).append("\"\n");
		swaggerBuilder.append("  version: 1.0.0\n\n");
		swaggerBuilder.append("paths:\n");
		swaggerBuilder.append("  /v1/").append(lowerClassNamePlural).append(":\n");
		swaggerBuilder.append("    get:\n");
		swaggerBuilder.append("      summary: Récupère la liste des ").append(className).append("\n");
		swaggerBuilder.append("      description: Récupère la liste des ").append(className).append("\n");
		swaggerBuilder.append("      operationId: getAll").append(classNamePlural).append("\n");
		swaggerBuilder.append("      tags:\n");
		swaggerBuilder.append("        - ").append(className).append("\n");
		swaggerBuilder.append("      parameters:\n");
		swaggerBuilder.append("        - name: idCompany\n");
		swaggerBuilder.append("          in: header\n");
		swaggerBuilder.append("          required: true\n");
		swaggerBuilder.append("          schema:\n");
		swaggerBuilder.append("            type: integer\n");
		swaggerBuilder.append("        - name: idEstablishment\n");
		swaggerBuilder.append("          in: header\n");
		swaggerBuilder.append("          required: true\n");
		swaggerBuilder.append("          schema:\n");
		swaggerBuilder.append("            type: integer\n");
		if (deleteRecord) {
			swaggerBuilder.append("        - name: deleteRecord\n");
			swaggerBuilder.append("          in: query\n");
			swaggerBuilder.append("          required: false\n");
			swaggerBuilder.append("          schema:\n");
			swaggerBuilder.append("            type: string\n");
			swaggerBuilder.append("            enum: [all, true, false]\n");
		}
		if (hasView) {
			swaggerBuilder.append("        - name: detail\n");
			swaggerBuilder.append("          in: query\n");
			swaggerBuilder.append("          description: Paramètre optionnel pour spécifier les détails\n");
			swaggerBuilder.append("          required: false\n");
			swaggerBuilder.append("          schema:\n");
			swaggerBuilder.append("            type: string\n");
		}
		swaggerBuilder.append("        - name: criteriaParam\n");
		swaggerBuilder.append("          in: query\n");
		swaggerBuilder.append("          description: \"Paramètres dynamiques sous forme de clé=valeur\"\n");
		swaggerBuilder.append("          required: false\n");
		swaggerBuilder.append("          style: form\n");
		swaggerBuilder.append("          explode: true\n");
		swaggerBuilder.append("          schema:\n");
		swaggerBuilder.append("            type: object\n");
		swaggerBuilder.append("            additionalProperties: true\n");
		swaggerBuilder.append("      responses:\n");
		swaggerBuilder.append("        \"200\":\n");
		swaggerBuilder.append("          description: Liste des ").append(classNamePlural)
				.append(" récupéréée avec succès\n");
		swaggerBuilder.append("          content:\n");
		swaggerBuilder.append("            application/json:\n");
		swaggerBuilder.append("              schema:\n");
		swaggerBuilder.append("                type: array\n");
		swaggerBuilder.append("                items:\n");
		swaggerBuilder.append("                  $ref: \"#/components/schemas/");
		if (hasView) {
			swaggerBuilder.append("Abstract");
		}
		swaggerBuilder.append(className).append("\"\n\n");
		swaggerBuilder.append("            application/xml:\n");
		swaggerBuilder.append("              schema:\n");
		swaggerBuilder.append("                type: array\n");
		swaggerBuilder.append("                items:\n");
		swaggerBuilder.append("                  $ref: \"#/components/schemas/");
		if (hasView) {
			swaggerBuilder.append("Abstract");
		}
		swaggerBuilder.append(className).append("\"\n\n");

		swaggerBuilder.append("  /v1/").append(lowerClassNamePlural).append("/{id}:\n");
		swaggerBuilder.append("    get:\n");
		swaggerBuilder.append("      summary: Récupère un ").append(className).append(" par son id\n");
		swaggerBuilder.append("      description: Récupère un ").append(className).append(" par son id\n");
		swaggerBuilder.append("      operationId: get").append(className).append("ById\n");
		swaggerBuilder.append("      tags:\n");
		swaggerBuilder.append("        - ").append(className).append("\n");
		swaggerBuilder.append("      parameters:\n");
		swaggerBuilder.append("        - name: id\n");
		swaggerBuilder.append("          in: path\n");
		swaggerBuilder.append("          required: true\n");
		swaggerBuilder.append("          schema:\n");
		swaggerBuilder.append("            type: integer\n");
		swaggerBuilder.append("        - name: idCompany\n");
		swaggerBuilder.append("          in: header\n");
		swaggerBuilder.append("          required: true\n");
		swaggerBuilder.append("          schema:\n");
		swaggerBuilder.append("            type: integer\n");
		swaggerBuilder.append("        - name: idEstablishment\n");
		swaggerBuilder.append("          in: header\n");
		swaggerBuilder.append("          required: true\n");
		swaggerBuilder.append("          schema:\n");
		swaggerBuilder.append("            type: integer\n");
		if (hasView) {
			swaggerBuilder.append("        - name: detail\n");
			swaggerBuilder.append("          in: query\n");
			swaggerBuilder.append("          description: Paramètre optionnel pour spécifier les détails\n");
			swaggerBuilder.append("          required: false\n");
			swaggerBuilder.append("          schema:\n");
			swaggerBuilder.append("            type: string\n");
		}
		swaggerBuilder.append("      responses:\n");
		swaggerBuilder.append("        \"200\":\n");
		swaggerBuilder.append("          description: ").append(className).append(" récupéré avec succès\n");
		swaggerBuilder.append("          content:\n");
		swaggerBuilder.append("            application/json:\n");
		swaggerBuilder.append("              schema:\n");
		swaggerBuilder.append("                $ref: \"#/components/schemas/");
		if (hasView) {
			swaggerBuilder.append("Abstract");
		}
		swaggerBuilder.append(className).append("\"\n");
		swaggerBuilder.append("        \"404\":\n");
		swaggerBuilder.append("          description: ").append(className).append(" non trouvé\n\n");
		swaggerBuilder.append("components:\n");
		swaggerBuilder.append("  schemas:\n");
		swaggerBuilder.append("    ").append(className).append(":\n");
		swaggerBuilder.append("      type: object\n");
		swaggerBuilder.append("      properties:\n");
		swaggerBuilder.append(extractClassProperties(javaClass));
		
		if (hasView) {
			swaggerBuilder.append("\n    ").append(className).append("View:\n")
			.append("      allOf:\n")
			.append("        - $ref: '#/components/schemas/").append(className).append("'\n")
			.append("      type: object\n")
			.append("      properties:\n")
			.append(extractClassProperties(viewClass)).append("\n")
			
			.append("    Abstract").append(className).append(":\n")
			.append("      oneOf:\n")
			.append("        - $ref: '#/components/schemas/").append(className).append("'\n")
			.append("        - $ref: '#/components/schemas/").append(className).append("View'\n");
		}

		return swaggerBuilder.toString();

	}
	
	public String generateMapperTest(String api, String classNameImport, String className, String classNamePlural, String lowerClassName,
			String lowerClassNamePlural, String entityName, String entity, String dto) {

		StringBuilder mapperTestBuilder = new StringBuilder();
		
		// ✅ Package
		mapperTestBuilder.append("package com.prios.api.a.").append(api).append(".mapper.").append(classNameImport)
				.append(";\n\n");
		
		// ✅ Imports
	    mapperTestBuilder.append("import static org.assertj.core.api.Assertions.assertThat;\n\n")
	            .append("import java.math.BigDecimal;\n")
	            .append("import java.time.LocalDateTime;\n")
	            .append("import java.time.ZoneId;\n")
	            .append("import java.time.ZonedDateTime;\n")
	            .append("import java.time.temporal.ChronoUnit;\n")
	            .append("import java.util.Comparator;\n")
	            .append("import java.util.Date;\n")
	            .append("import java.util.List;\n")
	            .append("import java.util.Optional;\n\n")
	            .append("import org.junit.jupiter.api.BeforeEach;\n")
	            .append("import org.junit.jupiter.api.Test;\n\n")
	            .append("import com.prios.core.a.common.history.management.HistoryManagementA;\n")
	            .append("import com.prios.core.a.shared.dto.common.HistoryManagementADto;\n")
	            .append("import com.prios.core.a.shared.dto.").append(api).append(".").append(className).append("Dto;\n")
	            .append("import com.prios.api.a.").append(api).append(".shared.").append(classNameImport).append(".").append(entityName).append(";\n\n");
		
	 // ✅ Déclaration de la classe
		mapperTestBuilder.append("class ").append(className).append("MapperTest {\n\n")
				.append("    private ").append(className).append("Mapper ").append(lowerClassName).append("Mapper;\n\n")
				.append("    ")	.append(className).append("Dto ").append(lowerClassName).append("Dto;\n")
				.append("    ").append(className).append("Dto ").append(lowerClassName).append("Dto2;\n\n")
				.append("    ").append(entityName).append(" ").append(lowerClassName).append(";\n")
				.append("    ").append(entityName).append(" ").append(lowerClassName).append("2;\n\n");

		// ✅ Setup
		mapperTestBuilder.append("    @BeforeEach\n").append("    void setUp() {\n")
				.append("        ").append(lowerClassName).append("Mapper = new ").append(className).append("MapperImpl();\n\n")
				.append(getHistoryManagement()).append("\n").append(entity).append("\n").append(dto).append("    }\n\n");

		// ✅ Tests
		// testDtosToEntities
		mapperTestBuilder.append("    @Test\n").append("    void test").append(className).append("DtosTo")
				.append(classNamePlural).append("() {\n")
				.append("        //GIVEN \n")
				.append("        List<").append(className).append("Dto> ")
				.append(lowerClassName).append("Dtos = List.of(").append(lowerClassName).append("Dto, ")
				.append(lowerClassName).append("Dto2);\n").append("        List<").append(entityName).append("> ")
				.append(lowerClassNamePlural).append("ToCompare = List.of(").append(lowerClassName).append(", ")
				.append(lowerClassName).append("2);\n\n").append("        //WHEN \n").append("        List<").append(entityName).append("> ")
				.append(lowerClassNamePlural).append(" = ").append(lowerClassName).append("Mapper.")
				.append(lowerClassName).append("DtosTo").append(classNamePlural).append("(").append(lowerClassName)
				.append("Dtos);\n\n").append("        //THEN \n").append("        assertThat(").append(lowerClassNamePlural)
				.append("ToCompare).usingRecursiveComparison()\n")
				.append("                .withComparatorForType(Comparator.comparing(d -> d.truncatedTo(ChronoUnit.MILLIS)), LocalDateTime.class)\n")
				.append("                .isNotNull().isEqualTo(").append(lowerClassNamePlural).append(");\n")
				.append("    }\n\n");

		// testEntityToDto
		mapperTestBuilder.append("    @Test\n").append("    void test").append(className).append("To").append(className)
				.append("Dto() {\n").append("        //GIVEN WHEN \n").append("        ").append(className).append("Dto ").append(lowerClassName)
				.append("DtoToCompare = ").append(lowerClassName).append("Mapper.").append(lowerClassName).append("To")
				.append(className).append("Dto(").append(lowerClassName).append(");\n\n").append("        //THEN \n").append("        assertThat(")
				.append(lowerClassName).append("DtoToCompare).usingRecursiveComparison()\n")
				.append("                .withComparatorForType(Comparator.comparing(d -> d.truncatedTo(ChronoUnit.MILLIS)), LocalDateTime.class)\n")
				.append("                .isEqualTo(").append(lowerClassName).append("Dto);\n").append("    }\n\n");

		// testEntitiesToDtos
		mapperTestBuilder.append("    @Test\n").append("    void test").append(classNamePlural).append("To")
				.append(className).append("Dtos() {\n").append("        //GIVEN \n").append("        List<").append(entityName).append("> ")
				.append(lowerClassNamePlural).append(" = List.of(").append(lowerClassName).append(", ")
				.append(lowerClassName).append("2);\n").append("        List<").append(className).append("Dto> ")
				.append(lowerClassName).append("Dtos = List.of(").append(lowerClassName).append("Dto, ")
				.append(lowerClassName).append("Dto2);\n\n").append("        //WHEN \n").append("        List<").append(className).append("Dto> ")
				.append(lowerClassName).append("DtosToCompare = ").append(lowerClassName).append("Mapper.")
				.append(lowerClassNamePlural).append("To").append(className).append("Dtos(")
				.append(lowerClassNamePlural).append(");\n\n").append("        //THEN \n").append("        assertThat(").append(lowerClassName)
				.append("DtosToCompare).usingRecursiveComparison()\n")
				.append("                .withComparatorForType(Comparator.comparing(d -> d.truncatedTo(ChronoUnit.MILLIS)), LocalDateTime.class)\n")
				.append("                .isNotNull().isEqualTo(").append(lowerClassName).append("Dtos);\n")
				.append("    }\n\n");

		// testOptionalEntityToOptionalDto
		mapperTestBuilder.append("    @Test\n").append("    void testOptional").append(className).append("ToOptional")
				.append(className).append("Dto() {\n").append("        //GIVEN \n").append("        Optional<").append(entityName)
				.append("> optional").append(className).append(" = Optional.of(").append(lowerClassName).append(");\n\n").append("        //WHEN \n")
				.append("        Optional<").append(className).append("Dto> optional").append(className)
				.append("Dto = ").append(lowerClassName).append("Mapper.optional").append(className)
				.append("ToOptional").append(className).append("Dto(optional").append(className).append(");\n\n").append("        //THEN \n")
				.append("        assertThat(optional").append(className).append("Dto).usingRecursiveComparison()\n")
				.append("                .withComparatorForType(Comparator.comparing(d -> d.truncatedTo(ChronoUnit.MILLIS)), LocalDateTime.class)\n")
				.append("                .isNotNull().isEqualTo(Optional.of(").append(lowerClassName).append("Dto));\n")
				.append("    }\n\n");

		// testOptionalEmpty
		mapperTestBuilder.append("    @Test\n").append("    void testOptional").append(className).append("ToOptional")
				.append(className).append("DtoEmpty() {\n").append("        //GIVEN \n").append("        Optional<").append(entityName)
				.append("> optional").append(entityName).append(" = Optional.empty();\n\n").append("        //WHEN \n").append("        Optional<")
				.append(className).append("Dto> optional").append(className).append("Dto = ").append(lowerClassName)
				.append("Mapper.optional").append(className).append("ToOptional").append(className)
				.append("Dto(optional").append(entityName).append(");\n\n").append("        //THEN \n").append("        assertThat(optional")
				.append(className).append("Dto).isEmpty();\n").append("    }\n\n");

		// testDtosToEntities_emptyList
		mapperTestBuilder.append("    @Test\n").append("    void test").append(className).append("DtosTo")
				.append(classNamePlural).append("_emptyList() {\n").append("        //GIVEN \n").append("        List<").append(className)
				.append("Dto> ").append(lowerClassName).append("Dtos = List.of();\n\n").append("        //WHEN \n").append("        List<")
				.append(entityName).append("> ").append(lowerClassNamePlural).append(" = ").append(lowerClassName)
				.append("Mapper.").append(lowerClassName).append("DtosTo").append(classNamePlural).append("(")
				.append(lowerClassName).append("Dtos);\n\n").append("        //THEN \n").append("        assertThat(").append(lowerClassNamePlural)
				.append(").isNotNull().isEmpty();\n").append("    }\n\n");

		// testEntityToDto_null
		mapperTestBuilder.append("    @Test\n").append("    void test").append(className).append("To").append(className)
				.append("Dto_null() {\n").append("        //GIVEN WHEN\n").append("        ").append(className).append("Dto ").append(lowerClassName)
				.append("DtoNull = ").append(lowerClassName).append("Mapper.").append(lowerClassName).append("To")
				.append(className).append("Dto(null);\n\n").append("        //THEN \n").append("        assertThat(").append(lowerClassName)
				.append("DtoNull).isNull();\n").append("    }\n\n");

		// testOptionalEmpty_null
		mapperTestBuilder.append("    @Test\n").append("    void testOptional").append(className).append("ToOptional")
				.append(className).append("Dto_null() {\n").append("        //GIVEN WHEN\n").append("        Optional<").append(className)
				.append("Dto> optional").append(className).append("Dto = ").append(lowerClassName)
				.append("Mapper.optional").append(className).append("ToOptional").append(className)
				.append("Dto(Optional.empty());\n\n").append("        //THEN \n").append("        assertThat(optional").append(className)
				.append("Dto).isEmpty();\n").append("    }\n\n");

		// ✅ Fin de la classe
		mapperTestBuilder.append("}\n");

	    return mapperTestBuilder.toString();
	}
	
	private String generateServiceTest(String api, String classNameImport, String className, String classNamePlural, String lowerClassName,
			String lowerClassNamePlural, String entityName, String entity, String entityView, boolean hasView, boolean deleteRecord, boolean idCompany) {

		StringBuilder serviceTest = new StringBuilder();

		// --- Package & Imports ---
		serviceTest.append("package com.prios.api.a.").append(api).append(".service.").append(classNameImport).append(";\n\n")
		.append("import static org.assertj.core.api.Assertions.assertThat;\n")
		.append("import static org.mockito.ArgumentMatchers.anyInt;\n")
		.append("import static org.mockito.Mockito.times;\n")
		.append("import static org.mockito.Mockito.verify;\n")
		.append("import static org.mockito.Mockito.when;\n\n")
		.append("import java.math.BigDecimal;\n")
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
		.append("import com.prios.api.a.").append(api).append(".repository.").append(classNameImport).append(".").append(className).append("Repository;\n");

		if (hasView) {
			serviceTest.append("import com.prios.api.a.").append(api).append(".repository.").append(classNameImport).append(".").append(className).append("ViewRepository;\n");
		}

		serviceTest.append("import com.prios.api.a.").append(api).append(".shared.").append(classNameImport).append(".").append(entityName).append(";\n")
		.append("import com.prios.core.a.common.history.management.HistoryManagementA;\n");

		if (hasView) {
			serviceTest.append("import com.prios.api.a.").append(api).append(".shared.").append(classNameImport).append(".").append(className).append("View;\n");
		}

		serviceTest.append("\n@ExtendWith(MockitoExtension.class)\n")
		.append("class ").append(className).append("ServiceImplTest {\n\n")
		.append("    @InjectMocks\n")
		.append("    ").append(className).append("ServiceImpl ").append(lowerClassName).append("Service;\n\n")
		.append("    @Mock\n")
		.append("    private ").append(className).append("Repository ").append(lowerClassName).append("Repository;\n\n");

		if (hasView) {
			serviceTest.append("    @Mock\n")
			.append("    private ").append(className).append("ViewRepository ").append(lowerClassName).append("ViewRepository;\n\n");
		}

		serviceTest.append("    ").append(entityName).append(" ").append(lowerClassName).append(";\n")
		.append("    ").append(entityName).append(" ").append(lowerClassName).append("2;\n");

		if (hasView) {
			serviceTest.append("    ").append(className).append("View ").append(lowerClassName).append("View;\n")
			.append("    ").append(className).append("View ").append(lowerClassName).append("View2;\n");
		}

		// --- setUp() ---
		serviceTest.append("    @BeforeEach\n")
		.append("    void setUp() {\n")
		.append(getHistoryManagement())
		.append(System.lineSeparator())
		.append(entity);

		if (hasView) {
			serviceTest.append(System.lineSeparator())
			.append(entityView)
			.append(System.lineSeparator());
		}

		serviceTest.append("    }\n\n");

		// --- Tests de base : findById / findById_notFound ---
		serviceTest.append("    @Test\n")
		.append("    void testFindById() {\n").append("        //GIVEN \n")
		.append("        when(").append(lowerClassName).append("Repository.findById(anyInt())).thenReturn(Optional.of(").append(lowerClassName).append("));\n\n").append("        //WHEN \n")
		.append("        Optional<").append(entityName).append("> result = ").append(lowerClassName).append("Service.findById(8);\n\n").append("        //THEN \n")
		.append("        verify(").append(lowerClassName).append("Repository, times(1)).findById(8);\n")
		.append("        assertThat(result).isPresent();\n")
		.append("        assertThat(result.get().getId()).isEqualTo(8);\n")
		.append("    }\n\n")

		.append("    @Test\n")
		.append("    void testFindById_notFound() {\n").append("        //GIVEN \n")
		.append("        when(").append(lowerClassName).append("Repository.findById(anyInt())).thenReturn(Optional.empty());\n\n").append("        //WHEN \n")
		.append("        Optional<").append(entityName).append("> result = ").append(lowerClassName).append("Service.findById(404);\n\n").append("        //THEN \n")
		.append("        verify(").append(lowerClassName).append("Repository, times(1)).findById(404);\n")
		.append("        assertThat(result).isNotNull().isEmpty();\n")
		.append("    }\n\n");

		if (hasView) {
			serviceTest.append("    @Test\n")
			.append("    void testFindViewById() {\n").append("        //GIVEN \n")
			.append("        when(").append(lowerClassName).append("ViewRepository.findById(anyInt())).thenReturn(Optional.of(").append(lowerClassName).append("View));\n\n").append("        //WHEN \n")
			.append("        Optional<").append(className).append("View> result = ").append(lowerClassName).append("Service.findViewById(8);\n\n").append("        //THEN \n")
			.append("        verify(").append(lowerClassName).append("ViewRepository, times(1)).findById(8);\n")
			.append("        assertThat(result).isPresent();\n")
			.append("        assertThat(result.get().getId()).isEqualTo(8);\n")
			.append("    }\n\n")

			.append("    @Test\n")
			.append("    void testFindViewById_notFound() {\n").append("        //GIVEN \n")
			.append("        when(").append(lowerClassName).append("ViewRepository.findById(anyInt())).thenReturn(Optional.empty());\n\n").append("        //WHEN \n")
			.append("        Optional<").append(className).append("View> result = ").append(lowerClassName).append("Service.findViewById(404);\n\n").append("        //THEN \n")
			.append("        verify(").append(lowerClassName).append("ViewRepository, times(1)).findById(404);\n")
			.append("        assertThat(result).isNotNull().isEmpty();\n")
			.append("    }\n\n");
		}

		serviceTest.append("}\n");

		return serviceTest.toString();
	}

	private String generateControllerTest(String api, String classNameImport, String className, String classNamePlural,
			String lowerClassName, String lowerClassNamePlural, String entityName, String entity, String entityView,
			String dto, String dtoView, boolean hasView, boolean deleteRecord, boolean idCompany) {

				
		StringBuilder controllerTest = new StringBuilder();

		// --- Package & Imports ---
		controllerTest.append("package com.prios.api.a.").append(api).append(".controller.").append(classNameImport).append(";\n\n")
		.append("import static org.assertj.core.api.Assertions.assertThat;\n")
		.append("import static org.mockito.ArgumentMatchers.any;\n")
		.append("import static org.mockito.ArgumentMatchers.anyInt;\n")
		.append("import static org.mockito.ArgumentMatchers.anyString;\n")
		.append("import static org.mockito.Mockito.times;\n")
		.append("import static org.mockito.Mockito.verify;\n")
		.append("import static org.mockito.Mockito.when;\n\n")
		.append("import java.math.BigDecimal;\n")
		.append("import java.time.LocalDateTime;\n")
		.append("import java.time.ZoneId;\n")
		.append("import java.time.ZonedDateTime;\n")
		.append("import java.time.temporal.ChronoUnit;\n")
        .append("import java.util.Comparator;\n")
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
		.append("import com.prios.api.a.").append(api).append(".mapper.").append(classNameImport).append(".").append(className).append("Mapper;\n");
		if (hasView) {
			controllerTest.append("import com.prios.api.a.").append(api).append(".mapper.").append(classNameImport).append(".").append(className).append("ViewMapper;\n");
		}
		controllerTest.append("import com.prios.api.a.").append(api).append(".service.").append(classNameImport).append(".").append(className).append("ServiceImpl;\n")
		.append("import com.prios.api.a.").append(api).append(".controller.").append(classNameImport).append(".").append(className).append("ControllerRest;\n")
		.append("import com.prios.api.a.").append(api).append(".shared.").append(classNameImport).append(".").append(entityName).append(";\n")
		.append("import com.prios.core.a.common.history.management.HistoryManagementA;\n")
		.append("import com.prios.core.a.shared.dto.common.HistoryManagementADto;\n")
		.append("import com.prios.core.a.shared.dto.").append(api).append(".").append(className).append("Dto;\n");

		if (hasView) {
			controllerTest.append("import com.prios.api.a.").append(api).append(".mapper.").append(classNameImport).append(".").append(className).append("ViewMapper;\n")
			.append("import com.prios.api.a.").append(api).append(".shared.").append(classNameImport).append(".").append(className).append("View;\n")
			.append("import com.prios.core.a.shared.dto.").append(api).append(".").append(className).append("ViewDto;\n")
			.append("import com.prios.core.a.shared.dto.").append(api).append(".").append("Abstract").append(className).append("Dto;\n");
		}

		controllerTest.append("\n@ExtendWith(MockitoExtension.class)\n")
		.append("class ").append(className).append("ControllerRestTest {\n\n")
		.append("    @Mock\n")
		.append("    ").append(className).append("ServiceImpl ").append(lowerClassName).append("Service;\n\n")
		.append("    @Mock\n")
		.append("    ").append(className).append("Mapper ").append(lowerClassName).append("Mapper;\n\n");

		if (hasView) {
			controllerTest.append("    @Mock\n")
			.append("    ").append(className).append("ViewMapper ").append(lowerClassName).append("ViewMapper;\n\n");
		}

		controllerTest.append("    @InjectMocks\n")
		.append("    ").append(className).append("ControllerRest ").append(lowerClassName).append("ControllerRest;\n\n")
		.append("    ").append(entityName).append(" ").append(lowerClassName).append(";\n")
		.append("    ").append(entityName).append(" ").append(lowerClassName).append("2;\n");

		if (hasView) {
			controllerTest.append("    ").append(className).append("View ").append(lowerClassName).append("View;\n")
			.append("    ").append(className).append("View ").append(lowerClassName).append("View2;\n");
		}

		controllerTest.append("    ").append(className).append("Dto ").append(lowerClassName).append("Dto;\n")
		.append("    ").append(className).append("Dto ").append(lowerClassName).append("Dto2;\n");

		if (hasView) {
			controllerTest.append("    ").append(className).append("ViewDto ").append(lowerClassName).append("ViewDto;\n")
			.append("    ").append(className).append("ViewDto ").append(lowerClassName).append("ViewDto2;\n");
		}

		// --- setUp() ---
		controllerTest.append("\n    @BeforeEach\n")
		.append("    void setUp() {\n")
		.append(getHistoryManagement()).append("\n")
		.append(entity).append("\n")
		.append(dto).append("\n");

		if (hasView) {
			controllerTest.append(entityView).append("\n")
			.append(dtoView).append("\n");
		}

		controllerTest.append("    }\n\n");

		// --- Tests getAll ---
		controllerTest.append("    @Test\n")
		.append("    void testGetAll").append(className).append("With1Result() {\n").append("        //GIVEN \n")
		.append("        List<").append(entityName).append("> ").append(lowerClassNamePlural).append(" = List.of(").append(lowerClassName).append(");\n")
		.append("        List<").append(className).append("Dto> ").append(lowerClassName).append("Dtos = List.of(").append(lowerClassName).append("Dto);\n")
		.append("        when(").append(lowerClassName).append("Service.findAll(any()");
		if (idCompany) {
			controllerTest.append(", anyInt(), anyInt()");
		}
		if (deleteRecord) {
			controllerTest.append(", any()");
		}
		controllerTest.append(")).thenReturn(").append(lowerClassNamePlural).append(");\n")
			.append("        when(")
				.append(lowerClassName).append("Mapper.").append(lowerClassNamePlural).append("To").append(className)
				.append("Dtos(any())).thenReturn(").append(lowerClassName).append("Dtos);\n\n").append("        //WHEN \n")
				.append("        ResponseEntity<List<");
		if (hasView) {
			controllerTest.append("Abstract");
		}			
		controllerTest.append(className).append("Dto>> response = ")
				.append(lowerClassName).append("ControllerRest.getAll").append(classNamePlural).append("(1, 2");
		if (deleteRecord) {
			controllerTest.append(",\"N\"");
		}
		if (hasView) {
			controllerTest.append(", null");
		}
		controllerTest.append(", null);\n\n").append("        //THEN \n")
		.append("        verify(").append(lowerClassName).append("Service, times(1)).findAll(any()");
		if (idCompany) {
			controllerTest.append(", anyInt(), anyInt()");
		}
		if (deleteRecord) {
			controllerTest.append(", any()");
		}
		controllerTest.append(");\n")
		.append("        verify(").append(lowerClassName).append("Mapper, times(1)).").append(lowerClassNamePlural).append("To").append(className).append("Dtos(any());\n")
		.append("        assertThat(response).usingRecursiveComparison()\n")
		.append("            .withComparatorForType(Comparator.comparing(d -> d.truncatedTo(ChronoUnit.MILLIS)), LocalDateTime.class)\n")
		.append("            .isNotNull().isEqualTo(ResponseEntity.status(HttpStatus.OK).body(").append(lowerClassName).append("Dtos));\n")
		.append("    }\n\n");

		controllerTest.append("    @Test\n")
		.append("    void testGetAll").append(className).append("With2Results() {\n").append("        //GIVEN \n")
		.append("        List<").append(entityName).append("> ").append(lowerClassNamePlural).append(" = List.of(").append(lowerClassName).append(", ").append(lowerClassName).append("2);\n")
		.append("        List<").append(className).append("Dto> ").append(lowerClassName).append("Dtos = List.of(").append(lowerClassName).append("Dto, ").append(lowerClassName).append("Dto2);\n")
		.append("        when(").append(lowerClassName).append("Service.findAll(any()");
		if (idCompany) {
			controllerTest.append(", anyInt(), anyInt()");
		}
		if (deleteRecord) {
			controllerTest.append(", any()");
		}
		controllerTest.append(")).thenReturn(").append(lowerClassNamePlural).append(");\n")	
		.append("        when(").append(lowerClassName).append("Mapper.").append(lowerClassNamePlural).append("To").append(className).append("Dtos(any())).thenReturn(").append(lowerClassName).append("Dtos);\n\n").append("        //WHEN \n")
		.append("        ResponseEntity<List<");
		if (hasView) {
			controllerTest.append("Abstract");
		}			
		controllerTest.append(className).append("Dto>> response = ")
				.append(lowerClassName).append("ControllerRest.getAll").append(classNamePlural).append("(1, 2");
		if (deleteRecord) {
			controllerTest.append(",\"N\"");
		}
		if (hasView) {
			controllerTest.append(", null");
		}
		controllerTest.append(", null);\n\n").append("        //THEN \n")
			.append("        verify(").append(lowerClassName).append("Service, times(1)).findAll(any()");
		if (idCompany) {
			controllerTest.append(", anyInt(), anyInt()");
		}
		if (deleteRecord) {
			controllerTest.append(", any()");
		}
		controllerTest.append(");\n")
		.append("        verify(").append(lowerClassName).append("Mapper, times(1)).").append(lowerClassNamePlural).append("To").append(className).append("Dtos(any());\n")
		.append("        assertThat(response).usingRecursiveComparison()\n")
		.append("            .withComparatorForType(Comparator.comparing(d -> d.truncatedTo(ChronoUnit.MILLIS)), LocalDateTime.class)\n")
		.append("            .isNotNull().isEqualTo(ResponseEntity.status(HttpStatus.OK).body(").append(lowerClassName).append("Dtos));\n")
		.append("    }\n\n");

		controllerTest.append("    @Test\n")
		.append("    void testGet").append(className).append("ById() {\n").append("        //GIVEN \n")
		.append("        when(").append(lowerClassName).append("Mapper.").append(lowerClassName).append("To").append(className).append("Dto(any())).thenReturn(").append(lowerClassName).append("Dto);\n")
		.append("        when(").append(lowerClassName).append("Service.findById(anyInt())).thenReturn(Optional.of(").append(lowerClassName).append("));\n\n").append("        //WHEN \n")
		.append("        ResponseEntity<");
		if (hasView) {
			controllerTest.append("Abstract");
		}
		controllerTest.append(className).append("Dto> response = ")
		.append(lowerClassName).append("ControllerRest.get").append(className).append("ById(1, 2, 3");
		if (hasView) {
			controllerTest.append(", null");
		}
		controllerTest.append(");\n\n").append("        //THEN \n")
		.append("        assertThat(response).usingRecursiveComparison().isNotNull()\n")
		.append("            .isEqualTo(ResponseEntity.status(HttpStatus.OK).body(").append(lowerClassName).append("Dto));\n")
		.append("    }\n\n");

		controllerTest.append("    @Test\n")
		.append("    void testGet").append(className).append("ById_notFound() {\n").append("        //GIVEN \n")
		.append("        when(").append(lowerClassName).append("Service.findById(anyInt())).thenReturn(Optional.empty());\n\n").append("        //WHEN \n")
		.append("        ResponseEntity<");
		if (hasView) {
			controllerTest.append("Abstract");
		}
		controllerTest.append(className).append("Dto> response = ")
		.append(lowerClassName).append("ControllerRest.get").append(className).append("ById(404, 1, 2");
		if (hasView) {
			controllerTest.append(", null");
		}
		controllerTest.append(");\n\n").append("        //THEN \n")
		.append("        assertThat(response).usingRecursiveComparison().isEqualTo(ResponseEntity.notFound().build());\n")
		.append("    }\n\n");

		// --- Tests pour la View ---
		if (hasView) {
			controllerTest.append("    @Test\n")
			.append("    void testGetAll").append(className).append("ViewWith1Result() {\n").append("        //GIVEN \n")
			.append("        List<").append(className).append("View> views = List.of(").append(lowerClassName).append("View);\n")
			.append("        List<").append(className).append("ViewDto> viewDtos = List.of(").append(lowerClassName).append("ViewDto);\n")
			.append("        when(").append(lowerClassName).append("Service.findAllView(any()");
			if (idCompany) {
				controllerTest.append(", anyInt(), anyInt()");
			}
			if (deleteRecord) {
				controllerTest.append(", any()");
			}
			controllerTest.append(")).thenReturn(views);\n")
			.append("        when(").append(lowerClassName).append("ViewMapper.").append(lowerClassName).append("ViewsTo").append(className).append("ViewDtos(any())).thenReturn(viewDtos);\n\n").append("        //WHEN \n")
			.append("        ResponseEntity<List<Abstract").append(className).append("Dto>> response = ")
			.append(lowerClassName).append("ControllerRest.getAll").append(classNamePlural).append("(1, 2");
			if (idCompany) {
				controllerTest.append(", anyInt(), anyInt()");
			}
			if (deleteRecord) {
				controllerTest.append(", \"N\"");
			}
			controllerTest.append(", \"full\", null);\n\n").append("        //THEN \n")
			.append("        verify(").append(lowerClassName).append("ViewMapper, times(1)).").append(lowerClassName).append("ViewsTo").append(className).append("ViewDtos(any());\n")
			.append("        assertThat(response).usingRecursiveComparison().isNotNull()\n")
			.append("            .isEqualTo(ResponseEntity.status(HttpStatus.OK).body(viewDtos));\n")
			.append("    }\n\n");

			controllerTest.append("    @Test\n")
			.append("    void testGetAll").append(className).append("ViewWith2Results() {\n").append("        //GIVEN \n")
			.append("        List<").append(className).append("View> views = List.of(").append(lowerClassName).append("View,").append(lowerClassName).append("View2);\n")
			.append("        List<").append(className).append("ViewDto> viewDtos = List.of(").append(lowerClassName).append("ViewDto,").append(lowerClassName).append("ViewDto2);\n")
			.append("        when(").append(lowerClassName).append("Service.findAllView(any()");
			if (idCompany) {
				controllerTest.append(", anyInt(), anyInt()");
			}
			if (deleteRecord) {
				controllerTest.append(", any()");
			}			
			controllerTest.append(")).thenReturn(views);\n")
			.append("        when(").append(lowerClassName).append("ViewMapper.").append(lowerClassName).append("ViewsTo").append(className).append("ViewDtos(any())).thenReturn(viewDtos);\n\n").append("        //WHEN \n")
			.append("        ResponseEntity<List<Abstract").append(className).append("Dto>> response = ")
			.append(lowerClassName).append("ControllerRest.getAll").append(classNamePlural).append("(1, 2");
			if (deleteRecord) {
				controllerTest.append(", \"N\"");
			}	
			controllerTest.append(",\"full\", null);\n\n").append("        //THEN \n")
			.append("        verify(").append(lowerClassName).append("ViewMapper, times(1)).").append(lowerClassName).append("ViewsTo").append(className).append("ViewDtos(any());\n")
			.append("        assertThat(response).usingRecursiveComparison().isNotNull()\n")
			.append("            .isEqualTo(ResponseEntity.status(HttpStatus.OK).body(viewDtos));\n")
			.append("    }\n\n");

			controllerTest.append("    @Test\n")
			.append("    void testGet").append(className).append("ViewById() {\n").append("        //GIVEN \n")
			.append("        when(").append(lowerClassName).append("ViewMapper.").append(lowerClassName).append("ViewTo").append(className).append("ViewDto(any())).thenReturn(").append(lowerClassName).append("ViewDto);\n")
			.append("        when(").append(lowerClassName).append("Service.findViewById(anyInt())).thenReturn(Optional.of(").append(lowerClassName).append("View));\n\n").append("        //WHEN \n")
			.append("        ResponseEntity<Abstract").append(className).append("Dto> response = ")
			.append(lowerClassName).append("ControllerRest.get").append(className).append("ById(1, 2, 3, \"full\");\n\n").append("        //THEN \n")
			.append("        assertThat(response).usingRecursiveComparison().isNotNull()\n")
			.append("            .isEqualTo(ResponseEntity.status(HttpStatus.OK).body(").append(lowerClassName).append("ViewDto));\n")
			.append("    }\n\n");

			controllerTest.append("    @Test\n")
			.append("    void testGet").append(className).append("ViewById_notFound() {\n").append("        //GIVEN \n")
			.append("        when(").append(lowerClassName).append("Service.findViewById(anyInt())).thenReturn(Optional.empty());\n\n").append("        //WHEN \n")
			.append("        ResponseEntity<Abstract").append(className).append("Dto> response = ")
			.append(lowerClassName).append("ControllerRest.get").append(className).append("ById(404, 1, 2, \"full\");\n\n").append("        //THEN \n")
			.append("        assertThat(response).usingRecursiveComparison().isEqualTo(ResponseEntity.notFound().build());\n")
			.append("    }\n\n");
		}

		controllerTest.append("}\n");

		return controllerTest.toString();
	}
	
	private String generateFeignClient(String api, String classNameImport, String className, String classNamePlural,
			String lowerClassName, String lowerClassNamePlural, String entityName, boolean hasView,
			boolean deleteRecord, boolean idCompany) {
		
		StringBuilder feign = new StringBuilder();

		// --- Package & Imports ---		
		feign.append("package com.prios.core.a.feign.").append(api).append(".").append(classNameImport).append(";\n\n")
		.append("import java.util.List;\n")
		.append("import java.util.Map;\n")
		.append("import org.springframework.cloud.openfeign.FeignClient;\n")
		.append("import org.springframework.web.bind.annotation.GetMapping;\n")
		.append("import org.springframework.web.bind.annotation.PathVariable;\n")
		.append("import org.springframework.web.bind.annotation.RequestHeader;\n")
		.append("import org.springframework.web.bind.annotation.RequestParam;\n\n")
		.append("import com.prios.core.a.shared.dto.").append(api).append(".").append(className).append("Dto;\n");
		if (hasView) {
			feign.append("import com.prios.core.a.shared.dto.").append(api).append(".").append(className).append("ViewDto;\n");
		}
		// --- Feign Client Interface ---
		feign.append("\n@FeignClient(name = \"api-a-").append(api).append("\", path = \"/v1/").append(lowerClassNamePlural).append("\")\n")
		.append("public interface ").append(className).append("FeignClient {\n\n")
		.append("    @GetMapping\n")
		.append("    List<").append(className).append("Dto> getAll(\n")
		.append("        @RequestHeader int idCompany,\n")
		.append("        @RequestHeader int idEstablishment,\n");
		if (hasView) {
			feign.append("        @RequestParam(required = false) String detail,\n");
		}
		if (deleteRecord) {
			feign.append("        @RequestParam(required = false) String deleteRecord,\n");
		}
		feign.append("        @RequestParam(required = false) Map<String, Object> criteriaParam);\n\n");
		
		if (hasView) {
			feign.append("    @GetMapping\n")
			.append("    List<").append(className).append("ViewDto> getAllView(\n")
			.append("        @RequestHeader int idCompany,\n")
			.append("        @RequestHeader int idEstablishment,\n")
			.append("        @RequestParam(required = false) String detail");			
			if (deleteRecord) {
				feign.append(",\n        @RequestParam(required = false) String deleteRecord");
			}
			feign.append(",\n        @RequestParam(required = false) Map<String, Object> priosParams);\n\n");
		}
		feign.append("    @GetMapping(\"/{id}\")\n")
		.append("    ").append(className).append("Dto getById(\n")
		.append("        @PathVariable int id,\n")
		.append("        @RequestHeader int idCompany,\n")
		.append("        @RequestHeader int idEstablishment");
		if (hasView) {
			feign.append(",\n        @RequestParam(required = false) String detail");
		}
		feign.append(");\n");
		
		if (hasView) {
			feign.append("\n    @GetMapping(\"/{id}\")\n")
			.append("    List<").append(className).append("ViewDto> getViewById(\n")
			.append("        @PathVariable int id,\n")
			.append("        @RequestHeader int idCompany,\n")
			.append("        @RequestHeader int idEstablishment,\n")
			.append("        @RequestParam(required = false) String detail);\n");
		}
		feign.append("}");
		
		return feign.toString();
	}

	private void generateEntityAndDto(String javaClassContent, String javaClassContent2, StringBuilder entity, StringBuilder dto, String className, String entityName, String lowerClassName, String number) {
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
            	Integer randomInt = ThreadLocalRandom.current().nextInt(0, maxDigits);
            	if (fieldName.equals("id") && number.equals("")) {
            		randomInt = 8;
            	}
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(" + randomInt + ");\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(" + randomInt + ");\n";
                break;  
            case "BigDecimal":
            	BigDecimal randomBigDecimal = randomBigDecimal(maxDigits, maxFractionDigits);
            	entity += "		" + className + number + ".set" + capitalizedFieldName + "(new BigDecimal(\"" + randomBigDecimal + "\"));\n";
            	dto += "		" + className + "Dto" + number + ".set" + capitalizedFieldName + "(new BigDecimal(\"" + randomBigDecimal + "\"));\n";
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

	public static BigDecimal randomBigDecimal(int maxDigits, int maxFractionDigits) {
		if (maxDigits <= 0) {
			throw new IllegalArgumentException("maxDigits must be > 0");
		}
		if (maxFractionDigits < 0) {
			throw new IllegalArgumentException("maxFractionDigits must be >= 0");
		}

		// Génère la partie entière
		BigDecimal integerPart = BigDecimal
				.valueOf(random.nextLong(maxDigits == 18 ? Long.MAX_VALUE : (long) Math.pow(10, maxDigits))).abs();

		// Génère la partie fractionnaire
		BigDecimal fractionPart = BigDecimal.ZERO;
		if (maxFractionDigits > 0) {
			long fractionValue = (long) (random.nextDouble() * Math.pow(10, maxFractionDigits));
			fractionPart = BigDecimal.valueOf(fractionValue).divide(BigDecimal.TEN.pow(maxFractionDigits),
					maxFractionDigits, RoundingMode.DOWN);
		}

		return integerPart.add(fractionPart);
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
		int maxLength = 255; // Valeur par défaut pour le maxLength
		String description = null; // Initialiser la description à null
		BigDecimal maxInteger = BigDecimal.ZERO;
		BigDecimal maxFraction = BigDecimal.ZERO;
		String maximum = "99";
		String minimum = "-99";
		String fraction = "";
		String multipleOf = "0.0";

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

				// Vérifier l'annotation @Digits pour le maximum et minimum
				if ("Digits".equals(normalAnnotation.getNameAsString())) {
					for (MemberValuePair pair : normalAnnotation.getPairs()) {
						if ("integer".equals(pair.getNameAsString())) {
							int integerDigits = Integer.parseInt(pair.getValue().toString());
							maxInteger = BigDecimal.TEN.pow(integerDigits).subtract(BigDecimal.ONE);
						}
						if ("fraction".equals(pair.getNameAsString())) {
							int fractionDigits = Integer.parseInt(pair.getValue().toString());
							if (fractionDigits > 0) {
								maxFraction = BigDecimal.TEN.pow(fractionDigits).subtract(BigDecimal.ONE);
								fraction = ".".concat(String.valueOf(maxFraction));
								multipleOf = BigDecimal.ONE.divide(BigDecimal.TEN.pow(fractionDigits), fractionDigits,
										RoundingMode.UNNECESSARY).toPlainString();
							}
						}
					}
					maximum = maxInteger.toPlainString().concat(fraction);
					minimum = "-".concat(maximum);
				}

				// Vérifier l'annotation @ApiObjectField pour la description
				if ("ApiObjectField".equals(normalAnnotation.getNameAsString())) {
					for (MemberValuePair pair : normalAnnotation.getPairs()) {
						if ("description".equals(pair.getNameAsString())) {
							description = pair.getValue().toString().replace("\"", "").replace(": ", ""); // Retirer les
																											// guillemets
						}
					}
				}

				if (description == null && field.hasJavaDocComment()) {
					Javadoc javadoc = field.getJavadoc().get();
					description = javadoc.getDescription().toText().replace("\"", "").replace(": ", "").trim();
				}

			}
		}

		switch (fieldType) {
		case "Long":
			swaggerProperty += "          type: integer\n          format: int64\n          description: " + description
					+ "\n          example: 12345\n          maximum: " + maximum + "\n          minimum: " + minimum
					+ "\n";
			break;
		case "Integer":
			Integer randomInt = ThreadLocalRandom.current().nextInt(0, maxInteger.intValue());
			String exampleInt = randomInt.toString();
			if (fieldName.toLowerCase().contains("month")) {
				exampleInt = "12";
			}if (fieldName.toLowerCase().contains("year")) {
				exampleInt = "2025";
			}
			swaggerProperty += "          type: integer\n          format: int32\n          description: " + description
					+ "\n          example: " + exampleInt + "\n          maximum: " + maximum + "\n          minimum: " + minimum
					+ "\n";
			break;
		case "Double":
			swaggerProperty += "          type: number\n          description: " + description
					+ "\n          example: 99.99\n          maximum: " + maximum + "\n          minimum: " + minimum
					+ "\n          multipleOf: " + multipleOf + "\n";
			break;
		case "Float":
			swaggerProperty += "          type: number\n          description: " + description
					+ "\n          example: 99.99\n          maximum: " + maximum + "\n          minimum: " + minimum
					+ "\n          multipleOf: " + multipleOf + "\n";
			break;
		case "BigDecimal":
			swaggerProperty += "          type: number\n          description: " + description
					+ "\n          example: 99.99\n          maximum: " + maximum + "\n          minimum: " + minimum
					+ "\n          multipleOf: " + multipleOf + "\n";
			break;
		case "Date":
			swaggerProperty += "          type: string\n          example: '2025-03-19T10:00:00Z'\n          description: "
					+ description + "\n          format: date-time\n";
			break;
		case "LocalDateTime":
			swaggerProperty += "          type: string\n          example: '2025-03-19T10:00:00Z'\n          description: "
					+ description + "\n          format: date-time\n";
			break;
		case "boolean":
			swaggerProperty += "          type: boolean\n          description: " + description
					+ "\n          example: true\n";
			break;
		case "Boolean":
			swaggerProperty += "          type: boolean\n          description: " + description
					+ "\n          example: true\n";
			break;
		case "String":
			swaggerProperty += "          type: string\n          maxLength: " + maxLength + "\n          description: "
					+ description + "\n          example: \"" + generateStringExample(fieldName, maxLength) + "\"\n";
			break;
		default:
			// Gestion des types complexes (par exemple, en ajoutant le type directement
			// sans $ref pour les types spéciaux)
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
		String[] specialTypes = { "CommonExchange", "BuildingExchange", "BuildingsDeliveryOrderExchange",
				"CellsTourLoadingExchange", "DeliveredThirdPartyTourLoadingExchange", "DeliveryInformationListExchange",
				"DeliveryInformationExchange", "GeneralInformationThirdParty", "QuantityTourLoadingExchange",
				"SiloExchange", "SilosDeliveryOrderExchange", "SupplementationListTourExchange",
				"SupplementationTourExchange", "SupplementationTourLoadingExchange", "VehicleBoxesTourLoadingExchange",
				"HistoryManagementA", "HistoryManagement" };
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
	        } else if (baseString.contains("mail")) {
	            // Exemple d'email
	            sb.append("exemple@email.com"); 
	        } else if (baseString.contains("address")) {
	            // Exemple d'adresse
	            sb.append("123 Rue de l'Exemple 75001 Paris");
	        } else if (baseString.contains("ean128")) {
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

	private String toCamelCase(String className) {
		return Character.toLowerCase(className.charAt(0)) + className.substring(1);
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

		if (className.endsWith("y") && className.length() > 1
				&& Character.isLetter(className.charAt(className.length() - 2))) {
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
        		+ "		historyManagementA.setCreationDate(date);\r\n"
        		+ "		historyManagementA.setCreationTime(date);\r\n"
        		+ "		historyManagementA.setProgramCreation(\"ProgramA\");\r\n"
        		+ "		historyManagementA.setUserCreation(\"UserA\");\r\n"
        		+ "\r\n"
        		+ "		HistoryManagementADto historyManagementADto = new HistoryManagementADto();\r\n"
        		+ "		historyManagementADto.setCreationDate(localDateTime);\r\n"
        		+ "		historyManagementADto.setCreationTime(localDateTime);\r\n"
        		+ "		historyManagementADto.setProgramCreation(\"ProgramA\");\r\n"
        		+ "		historyManagementADto.setUserCreation(\"UserA\");\r\n";
	}
}
