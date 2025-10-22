package prios.swagger.generator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

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

		response.put("mapper",
				generateMapper(api, classNameImport, className, lowerClassName, lowerClassNamePlural, entityName));
		if (hasView) {
			String lowerClassNameViewPlural = toPlural(lowerClassName + "View");
			response.put("mapperView", generateMapper(api, classNameImport, className + "View", lowerClassName + "View",
					lowerClassNameViewPlural, className + "View"));

		}
		response.put("service", generateService(api, classNameImport, className, lowerClassName, lowerClassNamePlural,
				entityName, hasView, deleteRecord, idCompany));
		response.put("serviceImpl",
				generateServiceImpl(api, classNameImport, className, entityName, hasView, deleteRecord, idCompany));
		response.put("controller", generateController(api, classNameImport, className, classNamePlural, lowerClassName,
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

	public String generateMapper(String api, String classNameImport, String className, String lowerClassName,
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
				.append(className).append("s(List<").append(className).append("Dto> ").append(lowerClassName)
				.append("Dtos);\n\n");

		mapperBuilder.append("    ").append(className).append("Dto ").append(lowerClassName).append("To")
				.append(className).append("Dto(").append(entityName).append(" ").append(lowerClassName)
				.append(");\n\n");

		mapperBuilder.append("    List<").append(className).append("Dto> ").append(lowerClassName).append("sTo")
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
				.append("Table> findAll(final List<SearchCriteria> requestParams,");

		if (idCompany) {
			serviceBuilder.append("final int idCompany,");
		}

		serviceBuilder.append("final int idEstablishment,final String deleteRecord);\n\n");

		// ✅ findAllView si hasView
		if (hasView) {
			serviceBuilder.append("    List<").append(className)
					.append("View> findAllView(final List<SearchCriteria> requestParams,");
			if (idCompany) {
				serviceBuilder.append("final int idCompany,");
			}
			serviceBuilder.append("final int idEstablishment,final String deleteRecord);\n\n");
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
				.append("import lombok.NonNull;\n").append("import lombok.RequiredArgsConstructor;\n\n");

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
		serviceImplBuilder.append("    @NonNull\n").append("    private final ").append(className).append("Repository ")
				.append(lowerClassName).append("Repository;\n\n");

		if (hasView) {
			serviceImplBuilder.append("    @NonNull\n").append("    private final ").append(className)
					.append("ViewRepository ").append(lowerClassName).append("ViewRepository;\n\n");
		}

		// ✅ findAll
		serviceImplBuilder.append("    @Override\n").append("    public List<").append(className)
				.append("Table> findAll(List<SearchCriteria> requestParams, int idCompany,\n")
				.append("            int idEstablishment, String deleteRecord) throws ResponseStatusException {\n")
				.append("        PriosParams params = new PriosParams(");
		if (idCompany) {
			serviceImplBuilder.append("idCompany,idEstablishment,");
		}
		serviceImplBuilder.append("requestParams");
		if (deleteRecord) {
			serviceImplBuilder.append(",deleteRecord);\n");
		}
		serviceImplBuilder.append("        return ").append(lowerClassName)
				.append("Repository.findAll(table(params));\n").append("    }\n\n");

		// ✅ findAllView (si hasView)
		if (hasView) {
			serviceImplBuilder.append("    @Override\n").append("    public List<").append(className)
					.append("View> findAllView(List<SearchCriteria> requestParams, int idCompany,\n")
					.append("            int idEstablishment, String deleteRecord) throws ResponseStatusException {\n")
					.append("        PriosParams params = new PriosParams(");
			if (idCompany) {
				serviceImplBuilder.append("idCompany,idEstablishment,");
			}
			serviceImplBuilder.append("requestParams");
			if (deleteRecord) {
				serviceImplBuilder.append(",deleteRecord);\n");
			}
			serviceImplBuilder.append("        return ").append(lowerClassName)
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

		if (idCompany) {
			controllerBuilder.append("@NotNull Integer idCompany, @NotNull Integer idEstablishment, ");
		}
		controllerBuilder.append("@Valid Map<String, Object> criteriaParam");
		if (hasView) {
			controllerBuilder.append(", @Valid String detail");
		}
		if (deleteRecord) {
			controllerBuilder.append(", @Valid String deleteRecord");
		}
		controllerBuilder.append(") {\n");

		if (hasView) {
			controllerBuilder.append("        if(StringUtils.isFull(detail)) {\n")
					.append("            return ResponseEntity.ok(new ArrayList<>(\n").append("                ")
					.append(lowerClassName).append("ViewMapper.").append(lowerClassName).append("ViewsTo")
					.append(className).append("ViewDtos(\n").append("                    ").append(lowerClassName)
					.append("Service.findAllView(SearchUtils.getRequestParams(this, MethodUtils.getMethodName(), criteriaParam)");
			if (idCompany)
				controllerBuilder.append(", idCompany");
			controllerBuilder.append(", idEstablishment");
			if (deleteRecord)
				controllerBuilder.append(", deleteRecord");
			controllerBuilder.append("))));\n").append("        }\n");
		}

		controllerBuilder.append("        return ResponseEntity.ok(new ArrayList<>(\n").append("            ")
				.append(lowerClassName).append("Mapper.").append(lowerClassName).append("sTo").append(className)
				.append("Dtos(\n").append("                ").append(lowerClassName)
				.append("Service.findAll(SearchUtils.getRequestParams(this, MethodUtils.getMethodName(), criteriaParam)");
		if (idCompany)
			controllerBuilder.append(", idCompany");
		controllerBuilder.append(", idEstablishment");
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
		controllerBuilder.append(className).append("Dto> get").append(className).append("ById(Integer id, ");
		if (idCompany)
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
				.append("To").append(className).append("Dto)\n").append("            .map(");
		if (hasView) {
			controllerBuilder.append("Abstract");
		}
		controllerBuilder.append(className).append("Dto.class::cast)\n")
				.append("            .map(ResponseEntity::ok)\n")
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
		swaggerBuilder.append("  /v1/").append(lowerClassNamePlural).append("s:\n");
		swaggerBuilder.append("    get:\n");
		swaggerBuilder.append("      summary: Récupère la liste des ").append(className).append("\n");
		swaggerBuilder.append("      description: Récupère la liste des ").append(className).append("\n");
		swaggerBuilder.append("      operationId: getAll").append(className).append("\n");
		swaggerBuilder.append("      tags:\n");
		swaggerBuilder.append("        - ").append(className).append("\n");
		swaggerBuilder.append("      parameters:\n");
		swaggerBuilder.append("        - name: criteriaParam\n");
		swaggerBuilder.append("          in: query\n");
		swaggerBuilder.append("          description: \"Paramètres dynamiques sous forme de clé=valeur\"\n");
		swaggerBuilder.append("          required: false\n");
		swaggerBuilder.append("          style: form\n");
		swaggerBuilder.append("          explode: true\n");
		swaggerBuilder.append("          schema:\n");
		swaggerBuilder.append("            type: object\n");
		swaggerBuilder.append("            additionalProperties: true\n");
		if (idCompany) {
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
		}
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
		swaggerBuilder.append("      responses:\n");
		swaggerBuilder.append("        \"200\":\n");
		swaggerBuilder.append("          description: Liste des ").append(className)
				.append("s récupérée avec succès\n");
		swaggerBuilder.append("          content:\n");
		swaggerBuilder.append("            application/json:\n");
		swaggerBuilder.append("              schema:\n");
		swaggerBuilder.append("                type: array\n");
		swaggerBuilder.append("                items:\n");
		swaggerBuilder.append("                  $ref: \"#/components/schemas/").append(className).append("\"\n\n");
		swaggerBuilder.append("            application/xml:\n");
		swaggerBuilder.append("              schema:\n");
		swaggerBuilder.append("                type: array\n");
		swaggerBuilder.append("                items:\n");
		swaggerBuilder.append("                  $ref: \"#/components/schemas/").append(className).append("\"\n\n");

		swaggerBuilder.append("  /v1/").append(lowerClassNamePlural).append("s/{id}:\n");
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
		if (idCompany) {
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
		}
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
		swaggerBuilder.append("                $ref: \"#/components/schemas/").append(className).append("\"\n");
		swaggerBuilder.append("        \"404\":\n");
		swaggerBuilder.append("          description: ").append(className).append(" non trouvé\n\n");
		swaggerBuilder.append("components:\n");
		swaggerBuilder.append("  schemas:\n");
		swaggerBuilder.append("    ").append(className).append(":\n");
		swaggerBuilder.append("      type: object\n");
		swaggerBuilder.append("      properties:\n");
		swaggerBuilder.append(extractClassProperties(javaClass));

		return swaggerBuilder.toString();

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
							description = pair.getValue().toString().replace("\"", "").replace(":", ""); // Retirer les
																											// guillemets
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
			swaggerProperty += "          type: integer\n          format: int64\n          description: " + description
					+ "\n          example: 12345\n          maximum: " + maximum + "\n          minimum: " + minimum
					+ "\n";
			break;
		case "Integer":
			swaggerProperty += "          type: integer\n          format: int32\n          description: " + description
					+ "\n          example: 100\n          maximum: " + maximum + "\n          minimum: " + minimum
					+ "\n";
			break;
		case "Double":
			swaggerProperty += "          type: number\n          format: bigdecimal\n          description: " + description
					+ "\n          example: 99.99\n          maximum: " + maximum + "\n          minimum: " + minimum
					+ "\n          multipleOf: " + multipleOf + "\n";
			break;
		case "Float":
			swaggerProperty += "          type: number\n          format: bigdecimal\n          description: " + description
					+ "\n          example: 99.99\n          maximum: " + maximum + "\n          minimum: " + minimum
					+ "\n          multipleOf: " + multipleOf + "\n";
			break;
		case "BigDecimal":
			swaggerProperty += "          type: number\n          format: bigdecimal\n          description: " + description
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
}
