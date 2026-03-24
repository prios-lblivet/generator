package prios.swagger.generator.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;

@Service
public class JdbiGeneratorService {

	public String generateJdbi(String javaClassContent) {
		

		String className = extractClassName(javaClassContent);

		StringBuilder jdbiBuilder = new StringBuilder();

		// ✅ Imports
		jdbiBuilder.append("import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;\n")
				.append("import org.jdbi.v3.sqlobject.config.RegisterRowMapper;\n")
				.append("import org.jdbi.v3.sqlobject.customizer.Bind;\n")
				.append("import org.jdbi.v3.sqlobject.customizer.BindBean;\n")
				.append("import org.jdbi.v3.sqlobject.statement.SqlQuery;\n");
		
		// ✅ Déclaration interface
		jdbiBuilder.append("public interface ").append(className).append("DtoRepositoryJDBI {\n\n");
		
		jdbiBuilder.append("    String FIND_BY_ID = \"\"\"\n");
		jdbiBuilder.append("       SELECT\n");
		
		// Parser la classe
        CompilationUnit cu = StaticJavaParser.parse(javaClassContent);

        // Récupérer la 1ère classe du fichier
        Optional<ClassOrInterfaceDeclaration> clazzOpt = cu.findFirst(ClassOrInterfaceDeclaration.class);
        if (clazzOpt.isEmpty()) {
            return "/* Classe introuvable */";
        }

        ClassOrInterfaceDeclaration clazz = clazzOpt.get();

        // Récupérer le nom de la table
        String[] table = new String[1];
        for (AnnotationExpr ann : clazz.getAnnotations()) {
            if ("Table".equals(ann.getNameAsString())) {
                ann.ifNormalAnnotationExpr(a -> {
                    for (MemberValuePair pair : a.getPairs()) {
                        if ("name".equals(pair.getNameAsString())) {
                        	table[0] = pair.getValue().toString().replace("\"", "");
                        }
                    }
                });
            }
        }
        
        String tableName = table[0] != null ? table[0] : "UnknownTable";
        
        // Nom de l'alias = nom de la classe sans "Table"S
        String alias = className.endsWith("Table") 
                       ? className.substring(0, className.length() - "Table".length())
                       : className;

        // Parcourir tous les AttributeOverride
        List<String> columns = new ArrayList<>();
        for (AnnotationExpr ann : clazz.getAnnotations()) {
            if ("AttributeOverride".equals(ann.getNameAsString())) {
                ann.ifNormalAnnotationExpr(a -> {
                    String fieldName = null;
                    String columnName = null;
                    for (MemberValuePair pair : a.getPairs()) {
                        if ("name".equals(pair.getNameAsString())) {
                            fieldName = pair.getValue().toString().replace("\"", "");
                        }
                        if ("column".equals(pair.getNameAsString()) && pair.getValue() instanceof NormalAnnotationExpr columnAnn) {
                            for (MemberValuePair colPair : columnAnn.getPairs()) {
                                if ("name".equals(colPair.getNameAsString())) {
                                    columnName = colPair.getValue().toString().replace("\"", "");
                                }
                            }
                        }
                    }
                    if (fieldName != null && columnName != null) {
                        String aliasName = fieldName.replace(".", "_");
                        columns.add(alias + "." + columnName + " AS " + aliasName);
                    }
                });
            }
        }

        // Construire la requête SQL
        for (int i = 0; i < columns.size(); i++) {
        	jdbiBuilder.append(columns.get(i));
            if (i < columns.size() - 1) {
            	jdbiBuilder.append(",");
            }
            jdbiBuilder.append("\n");
        }
        jdbiBuilder.append("FROM ").append(tableName).append(" ").append(alias).append("\n").append("\"\"\";\n\n}");

		
		return jdbiBuilder.toString();
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
}