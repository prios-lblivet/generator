package prios.swagger.generator.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.javadoc.Javadoc;

import prios.swagger.generator.response.ApiExcelResponse;

@Service
public class ExcelGeneratorService {
		
	public List<ApiExcelResponse> generate(String javaClassContent, String javaTableContent) {
		List<ApiExcelResponse> result = new ArrayList<>();
		// Parser la classe
		CompilationUnit cu = StaticJavaParser.parse(javaClassContent);

		// Récupérer la 1ère classe du fichier
		Optional<ClassOrInterfaceDeclaration> clazzOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);

		if (clazzOpt.isPresent()) {
			ClassOrInterfaceDeclaration clazz = clazzOpt.get();

			// Nom de table (extrait depuis @Table dans javaTableContent)
			String tableName = extractTableName(javaTableContent);

			// Parcourir les champs
			for (FieldDeclaration field : clazz.getFields()) {
				String fieldName = field.getVariable(0).getNameAsString();
			    
			    // Ignorer serialVersionUID
			    if ("serialVersionUID".equals(fieldName)) {
			        continue; // passe au champ suivant
			    }
				
				ApiExcelResponse dto = new ApiExcelResponse();

				dto.setNiveau(1); // par défaut
				dto.setNom(fieldName);
				dto.setType(field.getElementType().asString());
				dto.setNullable("nullable");

				// Annotations
				for (AnnotationExpr ann : field.getAnnotations()) {
					String annName = ann.getNameAsString();

					if (annName.equals("ApiObjectField")) {
						// Récupérer la description
						ann.ifNormalAnnotationExpr(a -> {
							for (MemberValuePair pair : a.getPairs()) {
								if (pair.getNameAsString().equals("description")) {
									dto.setDescription(pair.getValue().toString().replace("\"", ""));
								}
							}
						});
					}

					if (annName.equals("Digits")) {
						dto.setTaille(ann.toString());
					}

					if (annName.equals("Size")) {
						// le ' empêche excel d'interpreter le string comme une fonction 
						dto.setTaille("'" + ann.toString());
					}
					
					if (annName.equals("Id") || annName.equals("NotNull")) {
						dto.setNullable("not null");
					}

				}
				
                	if (field.hasJavaDocComment()) {
                	    Javadoc javadoc = field.getJavadoc().get();
                	    dto.setDescription(javadoc.getDescription().toText().trim());
                	}

				dto.setColonne(getColumnName(javaTableContent, fieldName));
				// Nom de table
				dto.setTable(tableName);

				result.add(dto);
			}
		}
		
		addHistoryManagement(javaTableContent, result);

		return result;   		
    } 

	private String extractTableName(String javaTableContent) {
        CompilationUnit cu = StaticJavaParser.parse(javaTableContent);
        Optional<ClassOrInterfaceDeclaration> clazzOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (clazzOpt.isPresent()) {
            for (AnnotationExpr ann : clazzOpt.get().getAnnotations()) {
                if (ann.getNameAsString().equals("Table")) {
                	for (MemberValuePair pair : ann.asNormalAnnotationExpr().getPairs()) {
						if ("name".equals(pair.getNameAsString())) {
							return pair.getValue().toString().replace("\"", "");
						}
					}
                }
            }
        }
        return "UNKNOWN_TABLE";
    }
	
	private String getColumnName(String javaTableContent, String fieldName) {
		// Parser la classe
		CompilationUnit cu = StaticJavaParser.parse(javaTableContent);

		// Récupérer la 1ère classe du fichier
		Optional<ClassOrInterfaceDeclaration> clazzOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);

		if (clazzOpt.isPresent()) {
			ClassOrInterfaceDeclaration clazz = clazzOpt.get();

			for (AnnotationExpr ann : clazz.getAnnotations()) {				
				final String[] nom = {null};
				final String[] column = {null};
				nom[0] = "_vide";

				String annName = ann.getNameAsString();
				if (annName.equals("AttributeOverride")) {
					ann.ifNormalAnnotationExpr(a -> {
						for (MemberValuePair pair : a.getPairs()) {
							if ("name".equals(pair.getNameAsString())) {
								nom[0] = pair.getValue().toString().replace("\"", "");
							}
							if ("column".equals(pair.getNameAsString()) && pair.getValue() instanceof NormalAnnotationExpr columnAnn) {
								for (MemberValuePair columnPair : columnAnn.getPairs()) {
									if ("name".equals(columnPair.getNameAsString())) {
										column[0] = columnPair.getValue().toString().replace("\"", "");
									}
								}
							}
						}
					});
					if (fieldName.equals(nom[0])) {
						return column[0];
					} 
					if (nom[0].contains("." + fieldName)) {
						return column[0];
					}
				}			
				
			}
		}
		
        return null;
    }
	
	private void addHistoryManagement(String javaTableContent, List<ApiExcelResponse> apiExcelResponses) {
		// Parser la classe
		CompilationUnit cu = StaticJavaParser.parse(javaTableContent);

		// Récupérer la 1ère classe du fichier
		Optional<ClassOrInterfaceDeclaration> clazzOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);

		if (clazzOpt.isPresent()) {
			ClassOrInterfaceDeclaration clazz = clazzOpt.get();

			for (AnnotationExpr ann : clazz.getAnnotations()) {				
				final String[] nom = {null};
				final String[] column = {null};

				String annName = ann.getNameAsString();
				if (annName.equals("AttributeOverride")) {
					ann.ifNormalAnnotationExpr(a -> {
						for (MemberValuePair pair : a.getPairs()) {
							if ("name".equals(pair.getNameAsString())) {
								nom[0] = pair.getValue().toString().replace("\"", "");
							}
							if ("column".equals(pair.getNameAsString()) && pair.getValue() instanceof NormalAnnotationExpr columnAnn) {
								for (MemberValuePair columnPair : columnAnn.getPairs()) {
									if ("name".equals(columnPair.getNameAsString())) {
										column[0] = columnPair.getValue().toString().replace("\"", "");
									}
								}
							}
						}
					});
					
					if (nom[0].contains("historyManagement")) {
						ApiExcelResponse apiExcelResponse = new ApiExcelResponse();
						apiExcelResponse.setNom(nom[0]);
						apiExcelResponse.setColonne(column[0]);
						apiExcelResponse.setNiveau(2);					
						if (nom[0].contains("creationDate")) {
							apiExcelResponse.setNom("creationDate");
							apiExcelResponse.setDescription("Trace création : Date");
							apiExcelResponse.setType("Date");
							apiExcelResponse.setNullable("not null");
							apiExcelResponse.setTaille("");
						}else if (nom[0].contains("creationTime")) {
							apiExcelResponse.setNom("creationTime");
							apiExcelResponse.setDescription("Trace création : Heure");
							apiExcelResponse.setType("Date");
							apiExcelResponse.setNullable("not null");
							apiExcelResponse.setTaille("");
						}else if (nom[0].contains("programCreation")) {
							apiExcelResponse.setNom("programCreation");
							apiExcelResponse.setDescription("Trace création : Programme");
							apiExcelResponse.setType("String");
							apiExcelResponse.setNullable("not null");
							apiExcelResponse.setTaille("'@Size(max = 10)");
						}else if (nom[0].contains("userCreation")) {
							apiExcelResponse.setNom("userCreation");
							apiExcelResponse.setDescription("Trace création : Utilisateur");
							apiExcelResponse.setType("String");
							apiExcelResponse.setNullable("not null");
							apiExcelResponse.setTaille("'@Size(max = 10)");
						}else if (nom[0].contains("modificationDate")) {
							apiExcelResponse.setNom("modificationDate");
							apiExcelResponse.setDescription("Trace Màj : Date");
							apiExcelResponse.setType("Date");
							apiExcelResponse.setNullable("not null");
							apiExcelResponse.setTaille("");
						}else if (nom[0].contains("modificationTime")) {
							apiExcelResponse.setNom("modificationTime");
							apiExcelResponse.setDescription("Trace Màj : Heure");
							apiExcelResponse.setType("Date");
							apiExcelResponse.setNullable("not null");
							apiExcelResponse.setTaille("");
						}else if (nom[0].contains("programModification")) {
							apiExcelResponse.setNom("programModification");
							apiExcelResponse.setDescription("Trace Màj : Programme");
							apiExcelResponse.setType("String");
							apiExcelResponse.setNullable("not null");
							apiExcelResponse.setTaille("'@Size(max = 10)");
						}else if (nom[0].contains("userModification")) {
							apiExcelResponse.setNom("userModification");
							apiExcelResponse.setDescription("Trace Màj : Utilisateur");
							apiExcelResponse.setType("String");
							apiExcelResponse.setNullable("not null");
							apiExcelResponse.setTaille("'@Size(max = 10)");
						}
						apiExcelResponses.add(apiExcelResponse);
					}
				}
			}
		}
    }
	
}
