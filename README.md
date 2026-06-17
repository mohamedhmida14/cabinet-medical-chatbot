# Cabinet Medical Chatbot

Application web Spring Boot pour la gestion d'un cabinet medical avec chatbot intelligent.

## Fonctionnalites

- Connexion par roles : administrateur, patient, medecin
- Dashboard administrateur
- Gestion des medecins
- Gestion des patients
- Gestion des rendez-vous
- Espace patient : consulter medecins, prendre/annuler RDV, voir consultations
- Espace medecin : consulter RDV, ajouter consultation
- Chatbot intelligent simple base sur des questions/reponses
- Chatbot Python optionnel avec OpenRouter/Gemini et acces PostgreSQL
- Base de donnees compatible PostgreSQL
- Mode H2 inclus pour tester sans installation de base de donnees

## Comptes de test

- Admin : `admin@cabinet.tn` / `admin`
- Medecin : `doctor@cabinet.tn` / `doctor`
- Patient : `patient@cabinet.tn` / `patient`

## Lancer rapidement avec H2

```bash
mvn spring-boot:run
```

Puis ouvrir :

```text
http://localhost:8080
```

Console H2 :

```text
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:cabinetdb
User: sa
Password: laisser vide
```

## Lancer avec PostgreSQL

Demarrer PostgreSQL avec Docker :

```bash
docker compose up -d
```

Lancer Spring Boot avec le profil postgres :

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

Ou avec un jar :

```bash
mvn clean package
java -jar target/cabinet-medical-chatbot-1.0.0.jar --spring.profiles.active=postgres
```

## Lancer le chatbot Python avec API OpenRouter ou Gemini

Le projet utilise OpenRouter par defaut pour le chatbot intelligent. Creez une cle API sur OpenRouter, puis preparez le fichier `.env`. Gemini reste disponible en mettant `AI_PROVIDER=gemini`.

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r python_chatbot/requirements.txt

cp python_chatbot/.env.example python_chatbot/.env
```

Mettez votre cle dans `python_chatbot/.env` :

```text
DATABASE_URL=postgresql://postgres:postgres@localhost:5432/cabinet_medical_db
AI_PROVIDER=openrouter
OPENROUTER_API_KEY=votre_cle_openrouter
OPENROUTER_MODEL=openrouter/free
GEMINI_API_KEY=votre_cle_api
GEMINI_MODEL=gemini-3.5-flash
```

Puis lancez le service Python :

```bash
uvicorn python_chatbot.main:app --host 127.0.0.1 --port 8000
```

Dans un autre terminal, activez le pont entre Spring Boot et Python :

```bash
export CHATBOT_PYTHON_URL=http://127.0.0.1:8000/ask
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

Sans `CHATBOT_PYTHON_URL`, le chatbot Spring Boot reste fonctionnel avec une reponse locale basee sur la base de donnees et les questions/reponses enregistrees.

## Deploiement Render

Le depot contient un fichier `render.yaml` pour deployer l'application avec Render Blueprint :

- `cabinet-medical-app` : application Spring Boot principale
- `cabinet-medical-chatbot-api` : service Python FastAPI pour le chatbot IA
- `cabinet-medical-db` : base PostgreSQL

Etapes :

1. Pousser le projet sur GitHub.
2. Dans Render, creer un nouveau Blueprint et choisir ce depot GitHub.
3. Renseigner la variable secrete `OPENROUTER_API_KEY` lorsque Render la demande.
4. Lancer le deploiement.

Render injecte automatiquement :

- `DATABASE_URL` pour Spring Boot et Python
- `CHATBOT_PYTHON_HOSTPORT` pour connecter Spring Boot au service Python
- `PORT` pour le port HTTP public

Le fichier `.env` local ne doit jamais etre pousse sur GitHub.

## Structure

```text
controller   : controleurs web et API
model        : entites JPA
repository   : interfaces Spring Data JPA
service      : logique metier
config       : donnees initiales
python_chatbot : chatbot Python FastAPI connecte a PostgreSQL et OpenRouter/Gemini
resources/templates : pages Thymeleaf
resources/static    : CSS et JavaScript
```

## UML

Les fichiers PlantUML sont dans `docs/uml` :

- `use-case.puml`
- `class-diagram.puml`
- `sequence-appointment.puml`
- `activity-appointment.puml`

## Base de donnees

Le script SQL theorique est dans :

```text
docs/database/schema.sql
```

Spring Boot peut creer automatiquement les tables via JPA.

## Remarque importante

Les mots de passe sont stockes en clair pour simplifier le projet universitaire. Pour une vraie application, il faut utiliser Spring Security avec BCrypt.
