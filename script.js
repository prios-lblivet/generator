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
	document.getElementById("downloadButton").style.display = "none";  // Cacher le bouton de téléchargement pendant la génération

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
			// Afficher le YAML dans le textarea
			document.getElementById("javaClassInput").value = data.swaggerYaml;  // Mettre le YAML dans le textarea
			generatedSwaggerYaml = data.swaggerYaml;  // Sauvegarder le YAML dans la variable
			nameSwaggerYaml = data.fileName

			// Afficher le message de succès
			document.getElementById("status").textContent = "✅ Swagger généré avec succès : " + nameSwaggerYaml;
			document.getElementById("status").style.color = "green";

			// Afficher le bouton de téléchargement maintenant
			document.getElementById("downloadButton").style.display = "inline-block";

			// Masquer le loader
			document.getElementById("loading").style.display = "none";
		})
		.catch(error => {
			document.getElementById("status").textContent = "❌ Erreur : " + error.message;
			document.getElementById("status").style.color = "red";
			document.getElementById("loading").style.display = "none"; // Masquer le loader en cas d'erreur
		});
}

function downloadSwagger() {
	if (!generatedSwaggerYaml) {
		alert("Aucun fichier Swagger généré.");
		return;
	}

	// Créer un Blob avec le YAML et déclencher le téléchargement
	let blob = new Blob([generatedSwaggerYaml], { type: "text/yaml" });
	saveAs(blob, nameSwaggerYaml); // saveAs est une méthode de FileSaver.js
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
	            document.getElementById("generatedEntity").value = data.entity; 
	            document.getElementById("generatedTable").value = data.table;
	            document.getElementById("generatedMapper").value = data.mapper;
	            document.getElementById("generatedController").value = data.controller;
	            document.getElementById("generatedService").value = data.service;
	            document.getElementById("generatedServiceImpl").value = data.serviceImpl;
	            document.getElementById("generatedRepository").value = data.repository;
	            document.getElementById("generatedSwagger").value = data.swagger;
	            
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



function generateTest() {
	let javaContent = document.getElementById("javaInput").value;
	if (!javaContent.trim()) {
		alert("Veuillez coller votre classe");
		return;
	}

	// Afficher le loader avant l'appel à fetch
	document.getElementById("loadingTestImport").style.display = "block";
	document.getElementById("statusImport").textContent = ""; // Effacer tout message précédent

	fetch("http://localhost:8091/api/test/generate", {
	            method: "POST",
	            headers: { 
	            	"Content-Type": "text/plain"
	            	},
	            body: javaContent
	        })
	        .then(response => {
	            if (response.ok) {
	                return response.text(); // Lire la réponse
	            }
	            return response.text().then(err => { // Lire l'erreur envoyée par le serveur
	                throw new Error(err.error || 'Erreur lors de la génération');
	            });
	        })
	        .then(data => {
	            // Afficher l'entité dans le textarea
	            console.log(data)
	            document.getElementById("generatedTestEntity").value = data; 
	            
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