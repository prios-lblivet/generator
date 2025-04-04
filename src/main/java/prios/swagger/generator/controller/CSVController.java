package prios.swagger.generator.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import prios.swagger.generator.response.ApiResponse;
import prios.swagger.generator.service.CSVGeneratorService;
import prios.swagger.generator.service.JavaGeneratorService;

@RestController
@RequestMapping("/api/csv")
@CrossOrigin(origins = "*")
class CSVController {

    private final CSVGeneratorService csvGeneratorService;

    public CSVController(CSVGeneratorService csvGeneratorService) {
        this.csvGeneratorService = csvGeneratorService;
    }
    
    @PostMapping("/generate")
    public ResponseEntity<?> generateJavaClasses(@RequestParam("pdfFile") MultipartFile pdfFile) {
        try {
            // Vérifier si un fichier est bien reçu
            if (pdfFile.isEmpty()) {
                return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Aucun fichier reçu"));
            }

            // Lire le contenu du fichier PDF (tu peux ensuite l'envoyer au service pour traitement)
            byte[] pdfBytes = pdfFile.getBytes();
            System.out.println("PDF reçu, taille: " + pdfBytes.length + " octets");

            // Ici, tu peux appeler un service pour extraire du texte depuis le PDF
            String extractedCSV = csvGeneratorService.extractCsvFromPDF(pdfBytes);

            // Construire la réponse JSON (ou autre format selon tes besoins)
            return ResponseEntity.ok(Collections.singletonMap("extractedCSV", extractedCSV));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erreur lors du traitement du PDF : " + e.getMessage()));
        }
    }   
}
