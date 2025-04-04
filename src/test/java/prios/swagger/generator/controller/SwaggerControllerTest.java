package prios.swagger.generator.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SwaggerControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testGenerateSwagger() {
        // Exemple de classe Java pour tester la génération du Swagger
        String javaClassContent = "public class Example { private String name; }";

        // Envoi de la requête POST pour générer le Swagger
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/swagger/generate", 
                javaClassContent, 
                Map.class
        );

        // Vérifier que la réponse est OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Vérifier que la réponse contient le nom du fichier et le Swagger YAML
        assertTrue(response.getBody().containsKey("fileName"));
        assertTrue(response.getBody().containsKey("swaggerYaml"));
        assertTrue(((String) response.getBody().get("swaggerYaml")).contains("components"));
    }

    @Test
    public void testGenerateSwaggerError() {
        // Exemple de contenu Java erroné pour tester la gestion des erreurs
        String javaClassContent = "Pas du java";

        // Envoi de la requête POST pour générer le Swagger
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/swagger/generate", 
                javaClassContent, 
                Map.class
        );

        // Vérifier que la réponse est une erreur interne du serveur
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        // Vérifier que la réponse contient un message d'erreur
        assertTrue(response.getBody().containsKey("error"));
        assertTrue(((String) response.getBody().get("error")).contains("Vous devez envoyer une classe Java"));
    }
}
