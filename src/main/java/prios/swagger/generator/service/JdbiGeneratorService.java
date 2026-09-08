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
	
	public static String convertDoubleSettersToBigDecimal(String input) {
	    StringBuilder result = new StringBuilder();

	    String[] lines = input.split("\\n");

	    for (String line : lines) {
	        String trimmed = line.trim();

	        // détecte setXXX(123.456);
	        if (trimmed.matches(".*set\\w+\\(\\d+\\.\\d+\\);")) {

	            int start = trimmed.indexOf('(') + 1;
	            int end = trimmed.indexOf(')');

	            String value = trimmed.substring(start, end);

	            // remplace par BigDecimal
	            String newLine = trimmed.substring(0, start)
	                    + "new BigDecimal(\"" + value + "\")"
	                    + trimmed.substring(end);

	            result.append(newLine).append("\n");
	        } else {
	            // ne touche pas aux autres (int, etc.)
	            result.append(trimmed).append("\n");
	        }
	    }

	    return result.toString();
	}
	
	private static String safe(String[] c, int i) {
	    return (i >= 0 && i < c.length) ? c[i] : "";
	}

	private static String buildInsert(String[] c) {

	    String[] v = new String[60];

	    for (int i = 0; i < v.length; i++) {
	        v[i] = safe(c, i);
	    }

	    return "INSERT INTO DCL.VODLENP VALUES (" +

	            parseString(v[0]) + ", " +   // YAROSUPENR
	            parseInt(v[1]) + ", " +
	            parseInt(v[2]) + ", " +
	            parseInt(v[3]) + ", " +
	            parseInt(v[4]) + ", " +
	            parseInt(v[5]) + ", " +
	            parseInt(v[6]) + ", " +
	            parseInt(v[7]) + ", " +
	            parseInt(v[8]) + ", " +
	            parseInt(v[9]) + ", " +

	            parseInt(v[10]) + ", " +
	            parseInt(v[11]) + ", " +
	            parseInt(v[12]) + ", " +

	            parseDate(v[13]) + ", " +
	            parseDate(v[14]) + ", " +
	            parseInt(v[15]) + ", " +
	            parseDate(v[16]) + ", " +

	            parseInt(v[17]) + ", " +
	            quote(v[18]) + ", " +
	            quote(v[19]) + ", " +
	            parseInt(v[20]) + ", " +

	            parseDate(v[21]) + ", " +
	            parseTime(v[22]) + ", " +
	            quote(v[23]) + ", " +

	            parseDate(v[24]) + ", " +
	            parseTime(v[25]) + ", " +
	            parseDate(v[26]) + ", " +
	            parseTime(v[27]) + ", " +

	            parseInt(v[28]) + ", " +
	            parseInt(v[29]) + ", " +
	            parseInt(v[30]) + ", " +
	            parseInt(v[31]) + ", " +

	            quote(v[32]) + ", " +
	            quote(v[33]) + ", " +
	            quote(v[34]) + ", " +
	            quote(v[35]) + ", " +
	            quote(v[36]) + ", " +
	            quote(v[37]) + ", " +
	            quote(v[38]) + ", " +

	            quote(v[39]) + ", " +
	            quote(v[40]) + ", " +
	            quote(v[41]) + ", " +
	            quote(v[42]) + ", " +
	            quote(v[43]) + ", " +
	            quote(v[44]) + ", " +
	            quote(v[45]) + ", " +

	            parseDate(v[46]) + ", " +
	            parseTime(v[47]) + ", " +
	            quote(v[48]) + ", " +
	            quote(v[49]) + ", " +
	            parseDate(v[50]) + ", " +
	            parseTime(v[51]) + ", " +
	            quote(v[52]) + ", " +
	            quote(v[53]) + ", " +

	            parseDateTime(v[54]) + ", " +
	            parseDateTime(v[55]) +

	            ");";
	}

	private static String parseString(String s) {
	    return (s == null || s.isEmpty()) ? "" : "'" + s + "'";
	}
	
	public static String generateSQL(String input) {

        StringBuilder result = new StringBuilder();

        String[] lines = input.split("\\r?\\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            String[] cols = line.split("\t", -1);

            result.append(buildInsert(cols)).append("\n");
        }

        return result.toString();
    }

	
    // ==========================
    // 🛠️ Helpers
    // ==========================

    private static String parseInt(String s) {
        return (s == null || s.isEmpty()) ? "0" : s;
    }

    private static String quote(String s) {
        return "'" + s + "'";
    }

    private static String parseDate(String s) {
        if (s == null || s.length() != 8) return "0";
        return s;
    }

    private static String parseTime(String s) {
        if (s == null || s.length() != 6) return "0";
        return s;
    }

    private static String parseDateTime(String s) {
        if (s == null || s.isEmpty()) return "NULL";
        return "'" + s + "'";
    }
}