let generatedSwaggerYaml = '';  // Variable pour stocker le YAML généré
let nameSwaggerYaml = '';  // Variable pour stocker le nom du YAML

function generateSwagger() {
	let javaClassContent = document.getElementById("javaClassInput").value;
	if (!javaClassContent.trim()) {
		alert("Veuillez coller une classe Java.");
		return;
	}

	// Afficher le loader avant l'appel à fetch
	document.getElementById("loading").style.display = "block";
	document.getElementById("status").textContent = ""; // Effacer tout message précédent

	fetch("http://localhost:8091/api/swagger/generate", {
		method: "POST",
		headers: { "Content-Type": "text/plain" },
		body: javaClassContent
	})
		.then(response => {
			if (response.ok) {
				return response.json(); // Lire la réponse JSON
			}
			return response.json().then(err => { // Lire l'erreur envoyée par le serveur
				throw new Error(err.error || 'Erreur lors de la génération');
			});
		})
		.then(data => {
			nameSwaggerYaml = data.fileName			
			document.getElementById("generatedYaml").textContent = data.swaggerYaml;
	       	document.getElementById("generatedJavaEntity").textContent = data.javaEntity;
	        Prism.highlightAll();

			// Afficher le message de succès
			document.getElementById("status").textContent = "✅ Swagger généré avec succès : " + nameSwaggerYaml;
			document.getElementById("status").style.color = "green";
			// Masquer le loader
			document.getElementById("loading").style.display = "none";
			document.getElementById("javaClassInput").value = ""; // Effacer le textarea après la génération
		})
		.catch(error => {
			document.getElementById("status").textContent = "❌ Erreur : " + error.message;
			document.getElementById("status").style.color = "red";
			document.getElementById("loading").style.display = "none"; // Masquer le loader en cas d'erreur
		});
}

function generateJava() {
	let csvContent = document.getElementById("csvInput").value;
	if (!csvContent.trim()) {
		alert("Veuillez coller votre csv.");
		return;
	}

	let className = document.getElementById("className").value;
	if (!className.trim()) {
		alert("Veuillez nommer votre classe.");
		return;
	}
	className = className.includes(".") ? className.split(".")[0] : className;

	let apiName = document.getElementById("apiName").value;
	if (!apiName.trim()) {
		alert("Veuillez indiquer le nom de l'api.");
		return;
	}
	apiName = apiName.includes("-") ? apiName.split("-").pop() : apiName;


	// Afficher le loader avant l'appel à fetch
	document.getElementById("loadingImport").style.display = "block";
	document.getElementById("statusImport").textContent = ""; // Effacer tout message précédent

	fetch("http://localhost:8091/api/java/generate", {
	            method: "POST",
	            headers: { 
	            	"Content-Type": "text/plain",
	            	 "className": encodeURIComponent(className), // Ajouter className dans le header
	                 "apiName": encodeURIComponent(apiName) // Ajouter apiName dans le header
	            	},
	            body: csvContent
	        })
	        .then(response => {
	            if (response.ok) {
	                return response.json(); // Lire la réponse JSON
	            }
	            return response.json().then(err => { // Lire l'erreur envoyée par le serveur
	                throw new Error(err.error || 'Erreur lors de la génération');
	            });
	        })
	        .then(data => {
	            // Afficher le YAML dans le textarea
	            document.getElementById("generatedEntity").textContent = data.entity; 
	            document.getElementById("generatedTable").textContent = data.table;
	            document.getElementById("generatedMapper").textContent = data.mapper;
	            document.getElementById("generatedController").textContent = data.controller;
	            document.getElementById("generatedService").textContent = data.service;
	            document.getElementById("generatedServiceImpl").textContent = data.serviceImpl;
	            document.getElementById("generatedRepository").textContent = data.repository;
				document.getElementById("generatedSwagger").textContent = data.swagger;
	            Prism.highlightAll();
	            
	            // Afficher le message de succès
	            document.getElementById("statusImport").textContent = "✅ classes généré avec succès";
	            document.getElementById("statusImport").style.color = "green";
		
	            // Masquer le loader
	            document.getElementById("loadingImport").style.display = "none";
	        })
	        .catch(error => {
	            document.getElementById("statusImport").textContent = "❌ Erreur : " + error.message;
	            document.getElementById("statusImport").style.color = "red";
	            document.getElementById("loadingImport").style.display = "none"; // Masquer le loader en cas d'erreur
	        });
}


function generateExcel() {
	let classContent = document.getElementById("excelJavaClassInput").value;
	if (!classContent.trim()) {
		alert("Veuillez coller votre class");
		return;
	}

	let tableContent = document.getElementById("excelJavaTableInput").value;
	if (!tableContent.trim()) {
		alert("Veuillez coller votre table.");
		return;
	}

	// Afficher le loader avant l'appel à fetch
	document.getElementById("loadingExcel").style.display = "block";
	document.getElementById("statusExcel").textContent = ""; // Effacer tout message précédent

	fetch("http://localhost:8091/api/excel/generate", {
	        method: "POST",
	        headers: {
				"Content-Type": "application/json"
			},	            
        	body: JSON.stringify({
           		classContent: classContent,
           		tableContent: tableContent
           		})
	        })
	        .then(response => {
	            if (response.ok) {
	                return response.json(); // Lire la réponse JSON
	            }
	            return response.json().then(err => { // Lire l'erreur envoyée par le serveur
	                throw new Error(err.error || 'Erreur lors de la génération');
	            });
	        })
	        .then(data => {
	            // Afficher le YAML dans le textarea
	            let csv;
			
			     data.forEach(row => {
			        csv += `${row.niveau}\t${row.taille}\t${row.nullable}\t${row.type}\t${row.nom}\t${row.description}\t${row.table}\t${row.colonne}\n`;
			    });
			
			    // Afficher dans un textarea pour copie facile
			    document.getElementById("generatedExcel").textContent = csv; 
			    			    	            
	            // Afficher le message de succès
	            document.getElementById("statusExcel").textContent = "✅ excel généré avec succès";
	            document.getElementById("statusExcel").style.color = "green";
		
	            // Masquer le loader
	            document.getElementById("loadingExcel").style.display = "none";
	        })
	        .catch(error => {
	            document.getElementById("statusExcel").textContent = "❌ Erreur : " + error.message;
	            document.getElementById("statusExcel").style.color = "red";
	            document.getElementById("loadingExcel").style.display = "none"; // Masquer le loader en cas d'erreur
	        });
}


function generateJdbi() {
	let classContent = document.getElementById("jdbiJavaClassInput").value;
	if (!classContent.trim()) {
		alert("Veuillez coller votre class");
		return;
	}

	// Afficher le loader avant l'appel à fetch
	document.getElementById("loadingJdbi").style.display = "block";
	document.getElementById("statusJdbi").textContent = ""; // Effacer tout message précédent

	fetch("http://localhost:8091/api/jdbi/generate", {
			method: "POST",
			headers: { "Content-Type": "text/plain" },
			body: classContent
		})
			.then(response => {
				if (response.ok) {
					return response.text(); // Lire la réponse JSON
				}
				return response.text().then(err => { // Lire l'erreur envoyée par le serveur
					throw new Error(err.error || 'Erreur lors de la génération');
				});
			})
			.then(data => {	
				document.getElementById("generatedJdbi").textContent = data;
		        Prism.highlightAll();

				// Afficher le message de succès
				document.getElementById("statusJdbi").textContent = "✅ Swagger généré avec succès : " + nameSwaggerYaml;
				document.getElementById("statusJdbi").style.color = "green";
				// Masquer le loader
				document.getElementById("loadingJdbi").style.display = "none";
			})
			.catch(error => {
				document.getElementById("statusJdbi").textContent = "❌ Erreur : " + error.message;
				document.getElementById("statusJdbi").style.color = "red";
				document.getElementById("loadingJdbi").style.display = "none"; // Masquer le loader en cas d'erreur
			});
}


function switchSection(evt, sectionId) {
	let sections = document.getElementsByClassName("content-section");
	for (let i = 0; i < sections.length; i++) {
		sections[i].classList.remove("active");
	}
	document.getElementById(sectionId).classList.add("active");

	let links = document.getElementsByClassName("menu")[0].getElementsByTagName("a");
	for (let i = 0; i < links.length; i++) {
		links[i].classList.remove("active");
	}
	evt.currentTarget.classList.add("active");
}

function openTab(evt, tabName) {
	let tabcontent = document.getElementsByClassName("tab-content");
	for (let i = 0; i < tabcontent.length; i++) {
		tabcontent[i].classList.remove("active");
	}
	document.getElementById(tabName).classList.add("active");

	let tablinks = document.getElementsByClassName("tab");
	for (let i = 0; i < tablinks.length; i++) {
		tablinks[i].classList.remove("active");
	}
	evt.currentTarget.classList.add("active");
}

function handleFileUpload() {
	const fileInput = document.getElementById('fileInput');
	const file = fileInput.files[0]; // On récupère le fichier sélectionné
	
	if (fileInput.files.length === 0) {
		alert("Veuillez sélectionner un fichier.");
		return;
	}
	
	const fileType = file.name.split('.').pop().toLowerCase(); // Récupère l'extension du fichier
	console.log(fileType)
	
	if (fileType === "pdf") {
		console.log("Fichier PDF sélectionné :", file.name);
		sendPDF(file);
	} else if (fileType === "csv") {
		console.log("Fichier CSV sélectionné :", file.name);
		handleCsvUpload(file);
	} else {
		alert("Format de fichier non pris en charge. Veuillez sélectionner un fichier PDF ou CSV.");
	}

}

// Fonction de traitement du CSV
function handleCsvUpload(file) {
    const reader = new FileReader();

    reader.onload = function(event) {
        let arrayBuffer = event.target.result;  // Récupérer l'ArrayBuffer
        let textDecoder = new TextDecoder("ISO-8859-1"); // Décodeur pour gérer les accents mal affichés
        let decodedText = textDecoder.decode(new Uint8Array(arrayBuffer)); // Convertir en string
        document.getElementById("csvInput").value = decodedText; // Afficher le texte décodé
    };

    reader.readAsArrayBuffer(file); // Lire le fichier en tant que buffer binaire
}

function sendPDF(file) {

    if (!file) {
        alert('Veuillez sélectionner un fichier PDF');
        return;
    }

    // Créer un objet FormData pour envoyer le fichier PDF
    const formData = new FormData();
    formData.append("pdfFile", file);  // 'pdfFile' est le nom du champ côté serveur    
    
	document.getElementById("loadingImport").style.display = "block";
	document.getElementById("statusImport").textContent = ""; // Effacer tout message précédent

    // Envoi du fichier PDF via fetch
    fetch("http://localhost:8091/api/csv/generate", {
        method: "POST",
        body: formData  // Le corps de la requête est l'objet FormData
    })
    .then(response => {
        if (response.ok) {
            return response.json();  // Lecture de la réponse en JSON
        }
        return response.json().then(err => {  // Lecture de l'erreur retournée par le serveur
            throw new Error(err.error || 'Erreur lors de l\'envoi du PDF');
        });
    })
    .then(data => {
        // Manipulation des données retournées
        document.getElementById("csvInput").value = data.extractedCSV; 
        document.getElementById("statusImport").textContent = "✅ PDF envoyé avec succès";
        document.getElementById("statusImport").style.color = "green";
        document.getElementById("loadingImport").style.display = "none";
    })
    .catch(error => {
        console.error("Erreur : " + error.message);
        document.getElementById("statusImport").textContent = "❌ Erreur : " + error.message;
        document.getElementById("statusImport").style.color = "red";
        document.getElementById("loadingImport").style.display = "none";  // Masquer le loader en cas d'erreur
    });
}

document.addEventListener("DOMContentLoaded", function() {
	const dropZone = document.getElementById("dropZone");
	const fileInput = document.getElementById("fileInput");
	const fileNameDisplay = document.getElementById("fileName");

	["dragover", "dragenter"].forEach(event => {
		dropZone.addEventListener(event, e => {
			e.preventDefault();
			dropZone.classList.add("dragover");
		});
	});

	["dragleave", "drop"].forEach(event => {
		dropZone.addEventListener(event, e => {
			e.preventDefault();
			dropZone.classList.remove("dragover");
		});
	});

	dropZone.addEventListener("drop", e => {
		e.preventDefault();
		if (e.dataTransfer.files.length > 0) {
			fileInput.files = e.dataTransfer.files;
			fileNameDisplay.textContent = `📄 Fichier sélectionné : ${fileInput.files[0].name}`;
			handleFileUpload(fileInput.files[0]);
		}
	});

	dropZone.addEventListener("click", () => {
		fileInput.click();
	});

	fileInput.addEventListener("change", () => {
		if (fileInput.files.length > 0) {
			fileNameDisplay.textContent = `📄 Fichier sélectionné : ${fileInput.files[0].name}`;
			handleFileUpload(fileInput.files[0]);
		}
	});
});


function generateJavaClass() {

	// Afficher le loader avant l'appel à fetch
	document.getElementById("loadingJavaClassImport").style.display = "block";
	document.getElementById("statusJavaClassImport").textContent = ""; // Effacer tout message précédent
	
	const javaClassInputMain = document.getElementById("javaClassInputMain").value.trim();
    const javaClassInputSecondary = document.getElementById("javaClassInputSecondary").value.trim();

    // Tes checkboxes
	const deleteRecord = document.getElementById("deleteRecord").checked;
	const idCompany = document.getElementById("idCompany").checked;
	const postAll = document.getElementById("postAll").checked;
	const putAll = document.getElementById("putAll").checked;
	const patchById = document.getElementById("patchById").checked;
	const deleteAll = document.getElementById("deleteAll").checked;

	if (!javaClassInputMain.trim()) {
		alert("Veuillez coller votre classe");
		return;
	}

	console.log("javaClassInputMain");
	console.log(javaClassInputMain);
    // Construire le corps JSON (2 classes)
    const body = {
        mainClassContent: javaClassInputMain,
        secondaryClassContent: javaClassInputSecondary || null
    };

	fetch("http://localhost:8091/api/javaClass/generate", {
	        method: "POST",
	        headers: {
	            "Content-Type": "application/json",
				"deleteRecord": deleteRecord,
				"idCompany": idCompany,
				"postAll": postAll,
				"putAll": putAll,
				"patchById": patchById,
				"deleteAll": deleteAll
	        },
	        body: JSON.stringify(body)
	        })
	        .then(response => {
	            if (response.ok) {
	                return response.json(); // Lire la réponse
	            }
	            return response.json().then(err => { // Lire l'erreur envoyée par le serveur
	                throw new Error(err.error || 'Erreur lors de la génération');
	            });
	        })
	        .then(data => {
	            // Afficher l'entité dans le textarea
	            console.log(data);
				document.getElementById("generatedJavaClassMapper").textContent = data.mapper; 
				document.getElementById("generatedJavaClassMapperTest").textContent = data.mapperTest; 
				document.getElementById("generatedJavaClassMapperView").textContent = data.mapperView; 
				document.getElementById("generatedJavaClassMapperViewTest").textContent = data.mapperViewTest; 
				document.getElementById("generatedJavaClassRepository").textContent = data.repository; 
				document.getElementById("generatedJavaClassService").textContent = data.service; 
				document.getElementById("generatedJavaClassServiceImpl").textContent = data.serviceImpl; 
				document.getElementById("generatedJavaClassServiceImplTest").textContent = data.serviceImplTest; 
				document.getElementById("generatedJavaClassController").textContent = data.controller; 
				document.getElementById("generatedJavaClassControllerTest").textContent = data.controllerTest; 
				document.getElementById("generatedJavaClassSwagger").textContent = data.swagger; 
				document.getElementById("generatedJavaClassFeign").textContent = data.feign; 
	            Prism.highlightAll();
	            
	            // Afficher le message de succès
	            document.getElementById("statusJavaClassImport").textContent = "✅ classes généré avec succès";
	            document.getElementById("statusJavaClassImport").style.color = "green";
		
	            // Masquer le loader
	            document.getElementById("loadingJavaClassImport").style.display = "none";
	        })
	        .catch(error => {
	            document.getElementById("statusJavaClassImport").textContent = "❌ Erreur : " + error.message;
	            document.getElementById("statusJavaClassImport").style.color = "red";
	            document.getElementById("loadingJavaClassImport").style.display = "none"; // Masquer le loader en cas d'erreur
	        });
}

function generateTest() {

	// Afficher le loader avant l'appel à fetch
	document.getElementById("loadingTestImport").style.display = "block";
	document.getElementById("statusImport").textContent = ""; // Effacer tout message précédent
	
	const javaInputMain = document.getElementById("javaInputMain").value.trim();
    const javaInputSecondary = document.getElementById("javaInputSecondary").value.trim();

    // Tes checkboxes
    const deleteRecord = document.getElementById("deleteRecord").checked;
    const idCompany = document.getElementById("idCompany").checked;


	console.log("javaInputMain");
	console.log(javaInputMain);
	if (!javaInputMain.trim()) {
		alert("Veuillez coller votre classe");
		return;
	}
    // Construire le corps JSON (2 classes)
    const body = {
        mainClassContent: javaInputMain,
        secondaryClassContent: javaInputSecondary || null
    };

	fetch("http://localhost:8091/api/test/generate", {
	        method: "POST",
	        headers: {
	            "Content-Type": "application/json",
	            "deleteRecord": deleteRecord,
	            "idCompany": idCompany
	        },
	        body: JSON.stringify(body)
	        })
	        .then(response => {
	            if (response.ok) {
	                return response.json(); // Lire la réponse
	            }
	            return response.json().then(err => { // Lire l'erreur envoyée par le serveur
	                throw new Error(err.error || 'Erreur lors de la génération');
	            });
	        })
	        .then(data => {
	            // Afficher l'entité dans le textarea
	            console.log(data);
	            document.getElementById("generatedTestEntity").textContent = data.entity; 
	            document.getElementById("generatedTestDto").textContent = data.dto; 
	            document.getElementById("generatedTestMapper").textContent = data.mapper; 
	            document.getElementById("generatedTestMapperView").textContent = data.mapperView; 
	            document.getElementById("generatedTestService").textContent = data.service; 
	            document.getElementById("generatedTestController").textContent = data.controller; 
	            Prism.highlightAll();
	            
	            // Afficher le message de succès
	            document.getElementById("statusTestImport").textContent = "✅ classes généré avec succès";
	            document.getElementById("statusTestImport").style.color = "green";
		
	            // Masquer le loader
	            document.getElementById("loadingTestImport").style.display = "none";
	        })
	        .catch(error => {
	            document.getElementById("statusTestImport").textContent = "❌ Erreur : " + error.message;
	            document.getElementById("statusTestImport").style.color = "red";
	            document.getElementById("loadingTestImport").style.display = "none"; // Masquer le loader en cas d'erreur
	        });
}

function copyToClipboard(elementId, button) {
  const text = document.getElementById(elementId).textContent;

  navigator.clipboard.writeText(text).then(() => {
    const originalText = button.textContent;
    button.textContent = "Copié !";
    button.disabled = true; // facultatif : empêche les clics rapides

    setTimeout(() => {
      button.textContent = originalText;
      button.disabled = false;
    }, 2000);
  });
}
