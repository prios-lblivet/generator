# Generator pour core-a

- Ce projet permet de générer automatiquement des fichiers Swagger à partir de classes Java. L'outil analyse les classes Java fournies, extrait leurs annotations et génère un fichier de configuration Swagger valide en format YAML. Ce projet est configurer pour correspondre aux normes de core-a
- et des classes Java directement depuis un fichier CSV respectant un format spécifique.

## ⚙️ Prérequis

Avant de commencer, assurez-vous que vous avez installé les éléments suivants sur votre machine :

- **Java 17**
- **Spring Tool Suite (STS)** ou **IntelliJ IDEA** pour la gestion du projet Java.
- **Maven** 

## 🚀 Installation et exécution

### Cloner le projet

1. Clonez ce dépôt sur votre machine locale :

    ```bash
    git clone https://github.com/username/repository-name.git
    ```

### Démarrer l'application

1. Exécutez l'application. Si vous êtes dans un terminal éxécutez la commande :
    ```bash
    mvn spring-boot:run
    ```
   Vous pouvez aussi utiliser votre IDE.

3. L'application démarre sur le port `8091` par défaut.

### Ouvrir l'interface graphique

Une fois l'application démarrée, ouvrez le fichier `index.html` dans votre navigateur pour accéder à l'interface graphique de l'application.

- Le fichier `index.html` se trouve à la racine du projet.
- Ouvrez ce fichier avec votre navigateur préféré (par exemple, **Google Chrome** ou **Firefox**).

## 🖥️ Utilisation de l'interface graphique

L'interface graphique permet de générer un fichier Swagger en envoyant une classe Java via une interface Web.

### Générer un fichier Swagger

1. Cliquez sur `générateur de swagger` dans le menu
2. Copiez le code source Java de la classe pour laquelle vous souhaitez générer un fichier Swagger.
3. Collez ce code dans le formulaire prévu à cet effet dans l'interface graphique.
4. Cliquez sur le bouton pour générer le fichier Swagger.
5. Le fichier Swagger sera généré et vous pourrez le télécharger directement.

### Générer une classe Java à partir d’un CSV

1. Dans l'onglet `import` uploadez (ou copier-coller) un fichier CSV décrivant les champs de la classe.
2. Cliquez sur le bouton `Générer les classes java` pour générer les classes.
2. Le code Java généré s’affiche instantanément dans l’interface dans chaque onglet spécifique.
3. Vous pouvez le copier ou l’utiliser directement pour générer un Swagger.

## 📄 Exemple de fichier CSV

### Exemple de fichier CSV à uploader

```csv
BDD;Fichier;Libellé Fichier;FNom long;Nom;Seq;Libellé Champ;T;Lng;Digit;Dec;Nom long;Nom variable;Embedded;Not null
DCL;BLIPPRP;Liste de prix prévisionnels;LISTE_PRIX_PREV;ILROSUPENR;100;Suppression enreg (Oui/Non);;1;1;;IL_RPO_SUP_ENR;deleteRecord;;
DCL;BLIPPRP;Liste de prix prévisionnels;LISTE_PRIX_PREV;ILIDSOC;200;Identifiant Société;S;9;9;0;IL_IDT_SOC;idCompany;;true
DCL;BLIPPRP;Liste de prix prévisionnels;LISTE_PRIX_PREV;ILIDETB;300;Identifiant Etablissement;S;9;9;0;IL_IDT_ETB;idEstablishment;;true
DCL;BLIPPRP;Liste de prix prévisionnels;LISTE_PRIX_PREV;ILIDLSTPPR;400;Identifiant Liste de Prix Prévisionnels;S;9;9;0;IL_IDT_LST_PPR;id;;true
DCL;BLIPPRP;Liste de prix prévisionnels;LISTE_PRIX_PREV;ILCDLSTPPR;500;Code Liste de Prix Prévisionnels;;3;3;;IL_COD_LST_PPR;code;; 
```
### Structure des colonnes du CSV :

- **BDD** : Base de données associée (ex : DCL).
- **Fichier** : Nom du fichier (ex : BLIPPRP).
- **Libellé Fichier** : Description du fichier (ex : Liste de prix prévisionnels).
- **FNom long** : Nom complet de la table/entité (ex : LISTE_PRIX_PREV).
- **Nom** : Identifiant court de la colonne dans la base de données (ex : ILROSUPENR).
- **Seq** : Numéro de séquence du champ.
- **Libellé Champ** : Libellé du champ (ex : Suppression enreg (Oui/Non)).
- **Lng** : Longueur maximale du champ (en caractères).
- **Digit** : Nombre de chiffres.
- **Dec** : Nombre de décimales.
- **Nom long** : Nom complet du champ.
- **Nom variable** : Nom de la variable générée en Java.
- **Embedded** : Champ intégré dans un autre objet (vide si non applicable).
- **Not null** : Indicateur de nullité du champ (valeur `true` ou vide).

## 📚 Spécifications prises en compte lors de la génération du fichier Swagger

Lors de la génération du fichier Swagger, l'outil prend en compte les spécifications suivantes :

- **Nom du composant** : Le nom du composant Swagger sera le nom de la classe Java envoyée. Si ce nom est absent, le nom "toto" sera utilisé.
- **Info du Swagger** : Le fichier Swagger généré contient un champ `info` avec :
  - `title` : le nom du projet
  - `description` : une brève description
  - `version` : `1.0.0`
- **Types primitifs** :
  - `Long` : `format: int64`
  - `Integer` : `format: int32`
  - `Double` : `format: double`
  - `Float` : `format: float`
  - `Date` : `type: string`, `format: date-time`
  - `String` : `format: string` et  `maxLength` en fonction de l'annotation `Size`
- **Types complexes** :
  Liste des composants communs à plusieur entité dans core-a. Cette liste sera aggrandit au fur et à mesure de la création de nouveau composant commun. Ilss sont définis directement avec `type: typeDuComposantType` sans utiliser `$ref` 
    - `CommonExchange`
    - `BuildingExchange`
    - `BuildingsDeliveryOrderExchange`
    - `CellsTourLoadingExchange`
    - `DeliveredThirdPartyTourLoadingExchange`
    - `DeliveryInformationDeliveryOrderExchange`
    - `DeliveryInformationExchange`
    - `GeneralInformationThirdParty`
    - `QuantityTourLoadingExchange`
    - `SiloExchange`
    - `SilosDeliveryOrderExchange`
    - `SupplementationListTourExchange`
    - `SupplementationTourExchange`
    - `SupplementationTourLoadingExchange`
    - `VehicleBoxesTourLoadingExchange`
    - `VeterinarianDeliveryOrderExchange`
    - `HistoryManagementA`
   
Pour tous les autres types complexes, ils sont référencés par `$ref`.

Cela permet de générer des fichiers Swagger bien structurés et conformes aux spécifications du projet.

## 📋 Exemple de fichier CSV
Voici un exemple de fichier CSV à uploader pour générer des classes Java :


