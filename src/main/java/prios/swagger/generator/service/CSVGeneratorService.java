package prios.swagger.generator.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class CSVGeneratorService {
	
	public String extractCsvFromPDF(byte[] pdfBytes) throws IOException {
        // Étape 1: Extraction du texte à partir du fichier PDF
        String pdfText = extractTextFromPDF(pdfBytes);

        // Étape 2: Transformation du texte extrait en liste de maps
        List<Map<String, String>> dataList = pdfToList(pdfText);

        // Étape 3: Conversion de la liste en format CSV
        String csvOutput = listToCsv(listPdfToListCsv(dataList));

        // Retourner le CSV sous forme de String
        return csvOutput;
    }

    
	public String extractTextFromPDF(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

	public List<Map<String, String>> pdfToList(String pdfText) {
	    List<Map<String, String>> dataList = new ArrayList<>();
	    
	    // Séparer le texte en lignes
	    String[] lines = pdfText.split("\n");
	    StringBuilder currentLine = new StringBuilder();
	    StringBuilder continuation = new StringBuilder();  // Pour garder les lignes qui débordent

	    for (String line : lines) {
	        // Si la ligne semble être une continuation de la précédente (par exemple, commence par une lettre seule ou un code)
	        if (line.matches("^\\s+[a-zA-Z].*") && !line.matches("^[A-Z]$")) {
	            continuation.append(" ").append(line.trim());  // Ajouter la ligne à la continuation
	        } else {
	            // Si une ligne complète (non continuation), combiner avec les débordements éventuels
	            if (continuation.length() > 0) {
	                currentLine.append(" ").append(continuation.toString());  // Fusionner les débordements
	                continuation.setLength(0);  // Réinitialiser la continuation
	            }

	            // Ajouter cette ligne complète à currentLine
	            currentLine.append(" ").append(line.trim());

	            // Traiter la ligne finale après fusion
	            processLine(currentLine.toString(), dataList);
	            currentLine.setLength(0);  // Réinitialiser pour la prochaine ligne
	        }
	    }

	    // Traiter la dernière ligne si elle reste dans currentLine
	    if (currentLine.length() > 0) {
	        processLine(currentLine.toString(), dataList);
	    }

	    return dataList;
	}

	private void processLine(String line, List<Map<String, String>> dataList) {
	    // Pattern pour matcher les lignes
	    Pattern pattern = Pattern.compile("(\\w+_\\w+_\\w+)\\s+(.*?)\\s+(\\d+)\\s*(\\d*|)\\s*([A-Z]*)\\s*(\\w+)\\s+(\\w+_\\w+_\\w+)\\s*(\\d*|)\\s*(\\d*|)\\s*([A-Z]*)");
	    Matcher matcher = pattern.matcher(line);
	    
	    // Vérifier si le pattern trouve une correspondance dans la ligne
	    if (matcher.find()) {
	        Map<String, String> dataMap = new HashMap<>();

	        // Extraire les groupes du matcher
	        String nomCourt = matcher.group(6);  // Groupe 6 : nom court
	        String groupe10 = matcher.group(10);  // Groupe 10 : complément si présent

	        // Vérifier si groupe 10 est non vide et concaténer avec nomCourt
	        if (!groupe10.isEmpty()) {
	            nomCourt += groupe10;
	        }

	        // Ajouter les valeurs extraites au map
	        dataMap.put("nom", matcher.group(1));  // Groupe 1 : nom
	        dataMap.put("designation", matcher.group(2));  // Groupe 2 : désignation
	        dataMap.put("lg", matcher.group(3));  // Groupe 3 : longueur
	        dataMap.put("dc", matcher.group(4).isEmpty() ? "" : matcher.group(4));  // Groupe 4 : valeur numérique
	        dataMap.put("ty", matcher.group(5).isEmpty() ? "" : matcher.group(5));  // Groupe 5 : type
	        dataMap.put("nom court", nomCourt);  // Utiliser la version corrigée du nom court
	        dataMap.put("md", matcher.group(7));  // Groupe 7 : identifiant
	        dataMap.put("vt", matcher.group(8).isEmpty() ? "" : matcher.group(8));  // Groupe 8 : valeur supplémentaire
	        dataMap.put("id", matcher.group(9).isEmpty() ? "" : matcher.group(9));  // Groupe 9 : identifiant

	        // Ajouter la ligne traitée dans la liste
	        dataList.add(dataMap);
	    }
	}



	public List<Map<String, String>> listPdfToListCsv(List<Map<String, String>> pdfList) {
		List<Map<String, String>> csvList = new ArrayList<>();
		for (Map<String, String> data : pdfList) {
            Map<String, String> dataMap = new HashMap<>();
			dataMap.put("BDD", "");
            dataMap.put("Fichier", "");
            dataMap.put("Libellé Fichier", "");
            dataMap.put("FNom long", "");
            dataMap.put("Nom", data.get("nom court"));
            dataMap.put("Seq", "");
            dataMap.put("Libellé Champ", data.get("designation"));
            dataMap.put("T", data.get("ty"));
            dataMap.put("Lng", data.get("lg"));
            dataMap.put("Digit", data.get("lg"));
            dataMap.put("Dec", data.get("dc"));
            dataMap.put("Nom variable", "");
            dataMap.put("Embedded", "");
            dataMap.put("Not null", "");

            // Ajouter les données extraites à la liste
            csvList.add(dataMap);
		}

       return csvList;
   }
	
	public String listToCsv(List<Map<String, String>> dataList) {
		 // Construction du CSV sous forme de String
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("BDD;Fichier;Libellé Fichier;FNom long;Nom;Seq;Libellé Champ;T;Lng;Digit;Dec;Nom long;Nom variable;Embedded;Not null\n");
        
        // Ajout des données au format CSV
        for (Map<String, String> data : dataList) {
            csvBuilder.append(String.format("%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%s\n",
                data.get("BDD"),
                data.get("Fichier"),
                data.get("Libellé Fichier"),
                data.get("FNom long"),
                data.get("Nom"),
                data.get("Seq"),
                data.get("Libellé Champ"),
                data.get("T"),
                data.get("Lng"),
                data.get("Digit"),
                data.get("Dec"),
                data.get("Nom long"),
                data.get("Nom variable"),
                data.get("Embedded"),
                data.get("Not null")
            ));
        }

        return csvBuilder.toString();
    }
}
