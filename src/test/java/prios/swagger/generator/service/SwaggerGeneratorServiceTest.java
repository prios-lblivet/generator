package prios.swagger.generator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SwaggerGeneratorServiceTest {

    private SwaggerGeneratorService swaggerGeneratorService;

    @BeforeEach
    void setUp() {
        // Initialisation de SwaggerGeneratorService
        swaggerGeneratorService = new SwaggerGeneratorService();
    }

    @Test
    void testGenerateSwaggerYaml() {
        // Exemple de classe Java en chaîne de caractères
        String javaClassContent = "package com.example;\n" +
                "import javax.persistence.*;\n" +
                "import javax.validation.constraints.Size;\n" +
                "@ApiObject(name = \"User\", description = \"A user object\")\n" +
                "public class User {\n" +
                "    @Size(max = 255)\n" +
                "    private String username;\n" +
                "    private int age;\n" +
                "    // getters and setters\n" +
                "}";

        // Appel de la méthode generateSwaggerYaml
        String swaggerYaml = swaggerGeneratorService.generateSwaggerYaml(javaClassContent);

        // Assertions pour vérifier que le Swagger YAML est généré correctement
        assertTrue(swaggerYaml.contains("title: \"User API\""));
        assertTrue(swaggerYaml.contains("description: \"A user object\""));
        assertTrue(swaggerYaml.contains("username:"));
        assertTrue(swaggerYaml.contains("type: string"));
        assertTrue(swaggerYaml.contains("maxLength: 255"));
    }

    @Test
    void testExtractClassName() {
        // Exemple de classe Java en chaîne de caractères
        String javaClassContent = "package com.example;\n" +
                "public class Product {\n" +
                "    private String name;\n" +
                "}";

        // Appel de la méthode extractClassName
        String className = swaggerGeneratorService.extractClassName(javaClassContent);

        // Assertions pour vérifier que le nom de la classe est correct
        assertEquals("Product", className);
    }

    @Test
    void testExtractClassPropertiesString() {
        // Exemple de classe Java en chaîne de caractères
        String javaClassContent = "package com.example;\n" +
                "import javax.validation.constraints.Size;\n" +
                "public class Product {\n" +
                "    @Size(max = 12)\n" +
                "    private String name;\n" +
                "}";

        // Appel de la méthode extractClassProperties
        String properties = swaggerGeneratorService.extractClassProperties(javaClassContent);

        // Assertions pour vérifier que les propriétés sont extraites correctement
        assertTrue(properties.contains("name:"));
        assertTrue(properties.contains("type: string"));
        assertTrue(properties.contains("maxLength: 12"));
    }
    
    @Test
    void testExtractClassPropertiesInteger() {
        // Exemple de classe Java en chaîne de caractères
        String javaClassContent = "package com.example;\n" +
                "import javax.validation.constraints.Size;\n" +
                "public class Product {\n" +
                "    @Size(max = 12)\n" +
                "    private Integer price;\n" +
                "}";

        // Appel de la méthode extractClassProperties
        String properties = swaggerGeneratorService.extractClassProperties(javaClassContent);

        // Assertions pour vérifier que les propriétés sont extraites correctement
        assertTrue(properties.contains("price:"));
        assertTrue(properties.contains("type: integer"));
        assertTrue(properties.contains("format: int32"));
    }    

	@Test
	void testExtractClassPropertiesLong() {
	    // Exemple de classe Java en chaîne de caractères
	    String javaClassContent = "package com.example;\n" +
	            "public class Product {\n" +
	            "    private Long id;\n" +
	            "}";
	
	    // Appel de la méthode extractClassProperties
	    String properties = swaggerGeneratorService.extractClassProperties(javaClassContent);
	
	    // Assertions pour vérifier que les propriétés sont extraites correctement
	    assertTrue(properties.contains("id:"));
	    assertTrue(properties.contains("type: integer"));
	    assertTrue(properties.contains("format: int64"));
	}
	
	@Test
	void testExtractClassPropertiesDouble() {
	    // Exemple de classe Java en chaîne de caractères
	    String javaClassContent = "package com.example;\n" +
	            "public class Product {\n" +
	            "    private Double weight;\n" +
	            "}";

	    // Appel de la méthode extractClassProperties
	    String properties = swaggerGeneratorService.extractClassProperties(javaClassContent);

	    // Assertions pour vérifier que les propriétés sont extraites correctement
	    assertTrue(properties.contains("weight:"));
	    assertTrue(properties.contains("type: number"));
	    assertTrue(properties.contains("format: double"));
	}

	@Test
	void testExtractClassPropertiesFloat() {
	    // Exemple de classe Java en chaîne de caractères
	    String javaClassContent = "package com.example;\n" +
	            "public class Product {\n" +
	            "    private Float height;\n" +
	            "}";

	    // Appel de la méthode extractClassProperties
	    String properties = swaggerGeneratorService.extractClassProperties(javaClassContent);

	    // Assertions pour vérifier que les propriétés sont extraites correctement
	    assertTrue(properties.contains("height:"));
	    assertTrue(properties.contains("type: number"));
	    assertTrue(properties.contains("format: float"));
	}

	@Test
	void testExtractClassPropertiesDate() {
	    // Exemple de classe Java en chaîne de caractères
	    String javaClassContent = "package com.example;\n" +
	            "import java.util.Date;\n" +
	            "public class Product {\n" +
	            "    private Date createdDate;\n" +
	            "}";

	    // Appel de la méthode extractClassProperties
	    String properties = swaggerGeneratorService.extractClassProperties(javaClassContent);

	    // Assertions pour vérifier que les propriétés sont extraites correctement
	    assertTrue(properties.contains("createdDate:"));
	    assertTrue(properties.contains("type: string"));
	    assertTrue(properties.contains("format: date-time"));
	}	

	@Test
	void testExtractClassPropertiesBoolean() {
	    // Exemple de classe Java en chaîne de caractères
	    String javaClassContent = "package com.example;\n" +
	            "public class Product {\n" +
	            "    private boolean inStock;\n" +
	            "}";
	
	    // Appel de la méthode extractClassProperties
	    String properties = swaggerGeneratorService.extractClassProperties(javaClassContent);
	
	    // Assertions pour vérifier que les propriétés sont extraites correctement
	    assertTrue(properties.contains("inStock:"));
	    assertTrue(properties.contains("type: boolean"));
	}
	
	@Test
	void testExtractClassPropertiesComplexType() {
	    // Exemple de classe Java avec un type complexe
	    String javaClassContent = "package com.example;\n" +
	            "public class Product {\n" +
	            "    private CommonExchange exchange;\n" +
	            "}";
	
	    // Appel de la méthode extractClassProperties
	    String properties = swaggerGeneratorService.extractClassProperties(javaClassContent);
	
	    // Assertions pour vérifier que les propriétés sont extraites correctement
	    assertTrue(properties.contains("exchange:"));
	    assertTrue(properties.contains("type: commonExchangeType"));
	}
	
	@Test
	void testExtractClassPropertiesSpecialComplexType() {
	    // Exemple de classe Java avec un type complexe spécial
	    String javaClassContent = "package com.example;\n" +
	            "public class Product {\n" +
	            "    private VehicleBoxesTourLoadingExchange vehicleBoxes;\n" +
	            "}";
	
	    // Appel de la méthode extractClassProperties
	    String properties = swaggerGeneratorService.extractClassProperties(javaClassContent);
	
	    // Assertions pour vérifier que les propriétés sont extraites correctement
	    assertTrue(properties.contains("vehicleBoxes:"));
	    assertTrue(properties.contains("type: vehicleBoxesTourLoadingExchangeType"));
	}
	
	@Test
	void testExtractClassPropertiesNonSpecialComplexType() {
	    // Exemple de classe Java avec un type complexe non spécial
	    String javaClassContent = "package com.example;\n" +
	            "public class Product {\n" +
	            "    private CustomerDetails customerDetails;\n" +
	            "}";

	    // Appel de la méthode extractClassProperties
	    String properties = swaggerGeneratorService.extractClassProperties(javaClassContent);

	    // Assertions pour vérifier que les propriétés sont extraites correctement
	    assertTrue(properties.contains("customerDetails:"));
	    assertTrue(properties.contains("$ref: '#/components/schemas/CustomerDetails'"));
	}

    @Test
    void testGenerateSwaggerYamlWithDefaultValues() {
        // Exemple de classe Java sans annotations
        String javaClassContent = "package com.example;\n" +
                "public class DefaultClass {\n" +
                "    private String field;\n" +
                "}";

        // Appel de la méthode generateSwaggerYaml
        String swaggerYaml = swaggerGeneratorService.generateSwaggerYaml(javaClassContent);

        // Assertions pour vérifier que le Swagger YAML utilise les valeurs par défaut
        assertTrue(swaggerYaml.contains("title: \"DefaultClass API\""));
        assertTrue(swaggerYaml.contains("description: \"Automatically generated API documentation\""));
    }
}
