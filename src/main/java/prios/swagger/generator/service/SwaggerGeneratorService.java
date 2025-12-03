package prios.swagger.generator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.javadoc.Javadoc;

@Service
public class SwaggerGeneratorService {

	public String generateSwaggerYaml(String javaClassContent) {
		String className = extractClassName(javaClassContent);
		String properties = extractClassProperties(javaClassContent);

		final String[] tabInfo = { null, null }; // Tableau pour stocker le titre et la description
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
										tabInfo[0] = pair.getValue().toString().replace("\"", ""); // Stocker dans le
																									// tableau
									}
									if ("description".equals(pair.getNameAsString())) {
										tabInfo[1] = pair.getValue().toString().replace("\"", "").replace(":", ""); // Stocker
																													// dans
																													// le
																													// tableau
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

		// Si aucune annotation @ApiObject n'est trouvée, utiliser des valeurs par
		// défaut
		String title = tabInfo != null && tabInfo[0] != null ? tabInfo[0] : className; // Utiliser le nom de la classe
																						// par défaut
		String description = tabInfo != null && tabInfo[1] != null ? tabInfo[1]
				: "Automatically generated API documentation"; // Utiliser le nom de la classe par défaut
		String routes = generateRoutes(className);

		return "openapi: 3.0.1\n" + "info:\n" + "  title: \"" + title + " API\"\n" + "  description: \"" + description
				+ "\"\n" + "  version: 1.0.0\n\n" + routes + "components:\n" + "  schemas:\n" + "    " + className
				+ ":\n" + "      type: object\n" + "      properties:\n" + properties;
	}

	private String generateRoutes(String className) {
		return "paths:\n" + "  /v1/" + endPointName(className) + "s:\n" + "    get:\n"
				+ "      summary: Récupère la liste des " + className + "\n"
				+ "      description: Récupère la liste des " + className + "\n" + "      operationId: getAll"
				+ className + "\n" + "      tags:\n" + "        - " + className + "\n" + "      parameters:\n"
				+ "        - name: criteriaParam\n" + "          in: query\n"
				+ "          description: \"Paramètres dynamiques sous forme de clé=valeur\"\n"
				+ "          required: false\n" + "          style: form\n" + "          explode: true\n"
				+ "          schema:\n" + "            type: object\n" + "            	additionalProperties: true\n"
				+ "        - name: idCompany\n" + "          in: header\n" + "          required: true\n"
				+ "          schema:\n" + "            type: integer\n" + "        - name: idEstablishment\n"
				+ "          in: header\n" + "          required: true\n" + "          schema:\n"
				+ "            type: integer\n" + "        - name: deleteRecord\n" + "          in: query\n"
				+ "          required: false\n" + "          schema:\n" + "            type: string\n"
				+ "            enum: [all, true, false]\n" + "        - name: detail\n" + "          in: query\n"
				+ "          description: Paramètre optionnel pour spécifier les détails\n"
				+ "          required: false\n" + "          schema:\n" + "            type: string\n"
				+ "      responses:\n" + "        \"200\":\n" + "          description: Liste des " + className
				+ "s récupérée avec succès\n" + "          content:\n" + "            application/json:\n"
				+ "              schema:\n" + "                type: array\n" + "                items:\n"
				+ "                  $ref: \"#/components/schemas/" + className + "\"\n\n" + "  /v1/"
				+ endPointName(className) + "s/{id}:\n" + "    get:\n" + "      summary: Récupère un " + className
				+ " par son id\n" + "      description: Récupère un " + className + " par son id\n"
				+ "      operationId: get" + className + "ById\n" + "      tags:\n" + "        - " + className + "\n"
				+ "      parameters:\n" + "        - name: id\n" + "          in: path\n" + "          required: true\n"
				+ "          schema:\n" + "            type: integer\n" + "        - name: idCompany\n"
				+ "          in: header\n" + "          required: true\n" + "          schema:\n"
				+ "            type: integer\n" + "        - name: idEstablishment\n" + "          in: header\n"
				+ "          required: true\n" + "          schema:\n" + "            type: integer\n"
				+ "      responses:\n" + "        \"200\":\n" + "          description: " + className
				+ " récupéré avec succès\n" + "          content:\n" + "            application/json:\n"
				+ "              schema:\n" + "                $ref: \"#/components/schemas/" + className + "\"\n"
				+ "        \"404\":\n" + "          description: " + className + " non trouvé\n\n";
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
		} else if (baseString.contains("address")) {
			// Exemple d'adresse
			sb.append("123 Rue de l'Exemple 75001 Paris");
		} else if (baseString.contains("mail")) {
			// Exemple d'email
			sb.append("exemple@email.com");
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

		int endIndex = Math.min(sb.toString().length(), maxLength); // On cherche la valeur minimal pour ne pas couper
																	// une chaine plus petite que le maxLength

		return sb.toString().substring(0, endIndex); // Couper pour s'assurer que la longueur est respectée
	}

	private String endPointName(String className) {
		String endPointName = toCamelCase(className);
		if (endPointName.endsWith("y")) {
			endPointName = endPointName.substring(0, endPointName.length() - 1) + "ie";
		}
		return endPointName;
	}

	public String replaceApiObjectFieldWithComment(String javaClassContent) {

		JavaParser javaParser = new JavaParser();
		try {
			CompilationUnit compilationUnit = javaParser.parse(javaClassContent).getResult()
					.orElseThrow(() -> new ParseException("Invalid Java code"));

			// 1) Remplacer @ApiObjectField par Javadoc
			compilationUnit.accept(new ModifierVisitor<Void>() {
				@Override
				public Visitable visit(FieldDeclaration field, Void arg) {
					field.getAnnotationByName("ApiObjectField").ifPresent(annotation -> {
						String description = "";

						if (annotation instanceof SingleMemberAnnotationExpr) {
							description = ((SingleMemberAnnotationExpr) annotation).getMemberValue().toString()
									.replaceAll("^\"|\"$", "");
						} else if (annotation instanceof NormalAnnotationExpr) {
							for (MemberValuePair pair : ((NormalAnnotationExpr) annotation).getPairs()) {
								if (pair.getNameAsString().equals("description")) {
									description = pair.getValue().toString().replaceAll("^\"|\"$", "");
									break;
								}
							}
						}

						field.getAnnotations().remove(annotation);

						if (!description.isBlank()) {
							field.setJavadocComment(new JavadocComment(description));
						}
					});

					// 2) Remplacer les DoubleConverter par BigDecimalConverter et DateConverter par
					// LocalDateTimeDecimalConverter
					field.getAnnotationByName("Convert").ifPresent(annotation -> {
						String converter = "";

						if (annotation instanceof NormalAnnotationExpr) {
							for (MemberValuePair pair : ((NormalAnnotationExpr) annotation).getPairs()) {
								if (pair.getNameAsString().equals("converter")) {
									converter = pair.getValue().toString();
									if (converter.equals("DoubleConverter.class")) {
										pair.setValue(new NameExpr("BigDecimalConverter.class"));
										compilationUnit.addImport("com.prios.core.a.util.BigDecimalConverter");
										compilationUnit.getImports().removeIf(i -> i.getNameAsString()
												.equals("com.prios.tools.config.data.DoubleConverter"));
									}
									if (converter.equals("FloatConverter.class")) {
										pair.setValue(new NameExpr("BigDecimalConverter.class"));
										compilationUnit.addImport("com.prios.core.a.util.BigDecimalConverter");
										compilationUnit.getImports().removeIf(i -> i.getNameAsString()
												.equals("com.prios.tools.config.data.FloatConverter"));
									}
									if (converter.equals("DateConverter.class")) {
										pair.setValue(new NameExpr("LocalDateTimeConverter.class"));
										compilationUnit.addImport("com.prios.tools.config.data.LocalDateTimeConverter");
										compilationUnit.getImports().removeIf(i -> i.getNameAsString()
												.equals("com.prios.tools.config.data.DateConverter"));
									}
									break;
								}
							}
						}

					});

					// 3) Remplacer les types Date → LocalDateTime et Double/Float → BigDecimal et
					// Boolean → boolean
					field.getVariables().forEach(variable -> {
						
						if (variable.getNameAsString().equals("serialVersionUID")) {
							field.removeJavaDocComment();
						}
						
						field.getElementType().ifClassOrInterfaceType(type -> {
							String typeName = type.getNameAsString();

							switch (typeName) {
							case "Boolean":
								type.setName("boolean");
								break;
							case "Date":
								type.setName("LocalDateTime");
								compilationUnit.addImport("java.time.LocalDateTime");
								compilationUnit.getImports()
										.removeIf(i -> i.getNameAsString().equals("java.util.Date"));
								break;
							case "Double":
							case "Float":
							case "BigDecimal":
								int integerValue = 0;
								int fractionValue = 0;

								Optional<AnnotationExpr> annotationOpt = field.getAnnotationByName("Digits");
								if (annotationOpt.isPresent()) {
									AnnotationExpr annotation = annotationOpt.get();
									if (annotation.isNormalAnnotationExpr()) {
										for (MemberValuePair pair : annotation.asNormalAnnotationExpr().getPairs()) {
											if (pair.getNameAsString().equals("integer")) {
												integerValue = Integer.parseInt(pair.getValue().toString());
											} else if (pair.getNameAsString().equals("fraction")) {
												fractionValue = Integer.parseInt(pair.getValue().toString());
											}
										}
									}
								}

								int precision = integerValue + fractionValue;
								int scale = fractionValue;

								boolean hasColumn = field.getAnnotationByName("Column").isPresent();
								if (!hasColumn) {
									NormalAnnotationExpr columnAnnotation = new NormalAnnotationExpr();
									columnAnnotation.setName("Column");
									NodeList<MemberValuePair> pairs = new NodeList<>();
									pairs.add(new MemberValuePair("precision",
											new IntegerLiteralExpr(String.valueOf(precision))));
									pairs.add(new MemberValuePair("scale",
											new IntegerLiteralExpr(String.valueOf(scale))));
									columnAnnotation.setPairs(pairs);
									field.addAnnotation(columnAnnotation);

									// S'assurer que l'import de javax.persistence.Column est présent
									compilationUnit.addImport("javax.persistence.Column");
								}

								type.setName("BigDecimal");
								compilationUnit.addImport("java.math.BigDecimal");
								compilationUnit.getImports()
										.removeIf(i -> i.getNameAsString().equals("java.lang.Double")
												|| i.getNameAsString().equals("java.lang.Float"));

							}
						});
					});

					return super.visit(field, arg);
				}
			}, null);
			
			// 4) Transformer @ApiObject(description = "...") en JavaDoc sur la classe
			compilationUnit.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
		        Optional<AnnotationExpr> apiObjectOpt = clazz.getAnnotationByName("ApiObject");

		        if (apiObjectOpt.isPresent()) {
		            AnnotationExpr apiObject = apiObjectOpt.get();

		            String description = null;

		            // Cas @ApiObject(name = "Food", description = "Produit - Complément aliment")
		            if (apiObject.isNormalAnnotationExpr()) {
		                NormalAnnotationExpr normal = apiObject.asNormalAnnotationExpr();
		                for (MemberValuePair pair : normal.getPairs()) {
		                    if ("description".equals(pair.getNameAsString())
		                            && pair.getValue().isStringLiteralExpr()) {
		                        description = pair.getValue().asStringLiteralExpr().asString();
		                        break;
		                    }
		                }
		            }

		            // Cas @ApiObject("...") ou autre forme single-member (peu probable ici)
		            if (description == null && apiObject.isSingleMemberAnnotationExpr()) {
		                Expression value = apiObject.asSingleMemberAnnotationExpr().getMemberValue();
		                if (value.isStringLiteralExpr()) {
		                    description = value.asStringLiteralExpr().asString();
		                }
		            }

		            // Si on a réussi à récupérer une description, on la met en JavaDoc
		            if (description != null && !description.isBlank()) {
		                // Remplace la JavaDoc existante si elle existe
						clazz.setJavadocComment(new JavadocComment(description));
		            }

		            // Supprimer l'annotation @ApiObject
		            clazz.getAnnotations().remove(apiObject);
		        }
		    });

			return compilationUnit.toString();

		} catch (Exception e) {
			e.printStackTrace();
			return javaClassContent;
		}
	}

	public String replaceAttributeOverrides(String javaClassContent) {
		CompilationUnit cu = StaticJavaParser.parse(javaClassContent);
		
	    // 1) Traitement des @AttributeOverrides au niveau des classes
		cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
			List<AnnotationExpr> toAdd = new ArrayList<>();
			List<AnnotationExpr> toRemove = new ArrayList<>();

			for (AnnotationExpr annotation : clazz.getAnnotations()) {
				if (annotation.getNameAsString().equals("AttributeOverrides")) {
					// Cas NormalAnnotationExpr (avec @AttributeOverride multiple)
					if (annotation instanceof NormalAnnotationExpr normal) {
						normal.getPairs().forEach(pair -> {
							Expression value = pair.getValue();
							if (value instanceof ArrayInitializerExpr array) {
								array.getValues().forEach(expr -> {
									if (expr.isAnnotationExpr()) {
										toAdd.add(expr.asAnnotationExpr());
									}
								});
							} else if (value.isAnnotationExpr()) {
								toAdd.add(value.asAnnotationExpr());
							}
						});
					}
					// Cas SingleMemberAnnotationExpr (rare)
					else if (annotation instanceof SingleMemberAnnotationExpr single) {
						Expression value = single.getMemberValue();
						if (value instanceof ArrayInitializerExpr array) {
							array.getValues().forEach(expr -> {
								if (expr.isAnnotationExpr()) {
									toAdd.add(expr.asAnnotationExpr());
								}
							});
						} else if (value.isAnnotationExpr()) {
							toAdd.add(value.asAnnotationExpr());
						}
					}

					toRemove.add(annotation);
				}
			}

			toRemove.forEach(clazz::remove);
			toAdd.forEach(clazz::addAnnotation);
		});
		
		// 2) Suppression de la Javadoc sur les champs serialVersionUID
	    cu.findAll(FieldDeclaration.class).forEach(field -> {
	        boolean hasSerialVersionUID = field.getVariables().stream()
	                .anyMatch(v -> "serialVersionUID".equals(v.getNameAsString()));

	        if (hasSerialVersionUID) {
	            field.removeJavaDocComment(); // enlève la Javadoc si présente
	        }
	    });
	    
		return cu.toString();
	}

	private String toCamelCase(String className) {
		return Character.toLowerCase(className.charAt(0)) + className.substring(1);
	}

	public static String toSnakeCase(String className) {
		return className.replaceAll("([a-z])([A-Z])", "$1_$2") // Ajoute un "_" entre les lettres minuscules et
																// majuscules
				.toLowerCase(); // Convertit en minuscules
	}
}