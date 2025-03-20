# Swagger Generator pour core-a

Ce projet permet de générer automatiquement des fichiers Swagger à partir de classes Java. L'outil analyse les classes Java fournies, extrait leurs annotations et génère un fichier de configuration Swagger valide en format YAML. Ce projet est configurer pour correspondre aux normes de core-a

## Prérequis

Avant de commencer, assurez-vous que vous avez installé les éléments suivants sur votre machine :

- **Java 17**
- **Spring Tool Suite (STS)** ou **IntelliJ IDEA** pour la gestion du projet Java.
- **Maven** 

## Installation et exécution

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

3. L'application démarre sur le port `8080` par défaut.

### Ouvrir l'interface graphique

Une fois l'application démarrée, ouvrez le fichier `index.html` dans votre navigateur pour accéder à l'interface graphique de l'application.

- Le fichier `index.html` se trouve à la racine du projet.
- Ouvrez ce fichier avec votre navigateur préféré (par exemple, **Google Chrome** ou **Firefox**).

## Utilisation de l'interface graphique

L'interface graphique permet de générer un fichier Swagger en envoyant une classe Java via une interface Web.

### Générer un fichier Swagger

1. Copiez le code source Java de la classe pour laquelle vous souhaitez générer un fichier Swagger.
2. Collez ce code dans le formulaire prévu à cet effet dans l'interface graphique.
3. Cliquez sur le bouton pour générer le fichier Swagger.
4. Le fichier Swagger sera généré et vous pourrez le télécharger directement.

## Spécifications prises en compte lors de la génération du fichier Swagger

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
