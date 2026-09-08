package prios.swagger.generator.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import prios.swagger.generator.shared.Data;

@Service
public class ErrorBDDService {

    ErrorBDDService() {
    }
		
    public List<Data> generate(String log, String table) {
		List<Data> dataList = new ArrayList<>();
		
		// Extraction des colonnes
	    Pattern columnsPattern = Pattern.compile(
	            "into\\s+\\w+\\s*\\((.*?)\\)\\s*values",
	            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	    Matcher columnsMatcher = columnsPattern.matcher(log);

	    List<String> columns = new ArrayList<>();

	    if (columnsMatcher.find()) {
	        String[] cols = columnsMatcher.group(1).split(",");
	        for (String col : cols) {
	            columns.add(col.trim());
	        }
	    }

	    // Extraction des valeurs
	    Pattern valuePattern = Pattern.compile(
	            "binding parameter \\(\\d+:(.*?)\\) <- \\[(.*?)]");

	    Matcher valueMatcher = valuePattern.matcher(log);

	    List<String> values = new ArrayList<>();
	    List<String> types = new ArrayList<>();

	    while (valueMatcher.find()) {
	        types.add(valueMatcher.group(1));
	        values.add(valueMatcher.group(2));
	    }

	    // Association colonne / valeur
	    int size = Math.min(columns.size(), values.size());

	    for (int i = 0; i < size; i++) {
	        Data data = new Data();
	        data.setColumn(columns.get(i));
	        data.setValue(values.get(i));
	        data.setDataType(types.get(i));
	        dataList.add(data);
	    }
	    
		// Complète les informations à partir du CREATE TABLE
		enrichWithCreateTable(dataList, table);

		// Récupère uniquement les données invalides
		List<Data> invalidData = dataList.stream().filter(Data::isInvalid).toList();

		return invalidData;

	}
	
    private void enrichWithCreateTable(List<Data> dataList, String table) {

        Pattern pattern = Pattern.compile(
            "\\[(\\w+)]\\s+\\[(\\w+)](?:\\((\\d+)(?:\\s*,\\s*(\\d+))?\\))?",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(table);

        while (matcher.find()) {

            String column = matcher.group(1);
            String type = matcher.group(2);

            int precision = 0;
            int scale = 0;

            if (matcher.group(3) != null) {
                precision = Integer.parseInt(matcher.group(3));
            }

            if (matcher.group(4) != null) {
                scale = Integer.parseInt(matcher.group(4));
            }
            
            for (Data data : dataList) {

                if (column.trim().equalsIgnoreCase("LWACITFTIE")) {
    				System.out.println("scale is 0 for column: " + column);
    			}

                if (data.getColumn().trim().equalsIgnoreCase(column.trim())) {

					data.setDataType(type);
					
					// Pour varchar, nvarchar, char...
					data.setNbCharactersMax(precision);

                    // Pour numeric / decimal
                    data.setNbCharactersMaxDecimal(scale);

                    break;
                }
            }
        }
    }
}


