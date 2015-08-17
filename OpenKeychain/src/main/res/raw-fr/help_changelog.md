[//] : # (NOTE : veuillez mettre chaque phrase sur sa propre ligne. Transifex met chaque ligne dans son propre champ de traduction !)

## 3.5

  * Key revocation on key deletion
  * Improved checks for insecure cryptography
  * Fix: Don't close OpenKeychain after first time wizard succeeds
  * API: Version 8

## 3.4

  * Téléchargement anonyme de clefs avec Tor
  * Prise en charge des serveurs mandataires
  * Meilleur gestion des erreurs de la ClefYubi

## 3.3

  * Nouvel écran de déchiffrement
  * Déchiffrement simultané de plusieurs fichiers
  * Meilleure gestion des erreurs ClefYubi

## 3.2

  * Première version avec prise en charge complète de la ClefYubi, proposée dans l'interface utilisateur : modifier les clefs, relier la clef Yubi au clefs...
  * Conception matérielle
  * Intégration de la lecture de code QR (nouvelles permissions exigées)
  * Amélioration de l'assistant de création de clef
  * Correctif - Contacts manquants après la synchro
  * Android 4 exigé
  * Nouvelle conception de l'écran des clefs
  * Simplification des préférences cryptographiques, meilleure sélection de codes de chiffrement sécurisés
  * API : signatures détachées, sélection libre de la clef de signature...
  * Correctif - Certaines clefs valides apparaissaient comme révoquées ou expirées
  * Ne pas accepter de signatures par des sous-clefs expirées ou révoquées
  * Prise en charge de keybase.io dans la vue avancée
  * Méthode pour mettre toutes les clefs à jour en même temps


## 3.1.2

  * Correctif - Exportation des clefs vers des fichiers (vraiment, maintenant)


## 3.1.1

  * Correctif - Exportation des clefs vers des fichiers (elles n'étaient écrites que partiellement)
  * Correctif - Plantage sur Android 2.3


## 3.1

  * Correctif - Plantage sur Android 5
  * Nouvel écran de certification
  * Échange sécurisé directement de la liste des clefs (bibliothèque SafeSlinger)
  * Nouveau flux de programme pour les codes QR
  * Écran de déchiffrement redessiné
  * Nouveaux agencement et couleurs d'icônes
  * Importation des clefs secrètes corrigée de Symantec Encryption Desktop
  * Prise en charge expérimentale de la ClefYubi : les ID de sous-clefs sont maintenant vérifiés correctement


## 3.0.1

  * Meilleure gestion de l'importation de nombreuses clefs
  * Sélection des sous-clefs améliorée


## 3.0

  * Des applis compatibles installables sont proposées dans la liste des applis
  * Nouvelle conception pour les écrans de déchiffrement
  * Nombreux correctifs d'importation des clefs, corrigent aussi les clefs dépouillées
  * Accepter et afficher les drapeaux d'authentification des clefs
  * Interface utilisateur pour générer des clefs personnalisées
  * Corrigé - Certificats de révocation des ID utilisateurs
  * Nouvelle recherche nuagique (dans les serveurs traditionnels et dans keybase.io)
  * Prise en charge du dépouillement des clefs dans OpenKeychain
  * Prise en charge expérimentale de la ClefYubi : prise en charge de la génération de signature et le déchiffrement


## 2.9.2

  * Correctif - Clefs brisées dans 2.9.1
  * Prise en charge expérimentale de la ClefYubi : le déchiffrement fonctionne maintenant avec l'API


## 2.9.1

  * Partage de l'écran de chiffrement en deux
  * Correctif - Gestion des drapeaux de clefs (prend maintenant en charge les clefs Mailvelope 0.7)
  * Gestion des phrases de passe améliorée
  * Partage de clefs par SafeSlinger
  * Prise en charge expérimentale de la ClefYubi : préférence pour permettre d'autres NIP, seule la signature par l'API OpenPGP fonctionne actuellement, mais pas dans OpenKeychain
  * Correctif - Utilisation de clefs dépouillées
  * SHA256 par défaut pour la compatibilité
  * L'API des intentions a changé, voir https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * L'API d'OpenPGP gère maintenant les clefs révoquées/expirées et retourne tous les ID utilisateurs


## 2.9

  * Correction des plantages présents dans v2.8
  * Prise en charge expérimentale CCE
  * Prise en charge expérimentale de la ClefYubi : signature seulement avec les clefs importées


## 2.8

  * Tellement de bogues ont été réglés dans cette version que nous nous concentrons sur les nouvelles caractéristiques principales.
  * Modification des clefs : nouvelle et superbe conception, révocations des clefs
  * Importation des clefs : nouvelle et superbe conception, connexion sécurisé aux serveurs de clefs par hkps, résolution des serveurs de clefs par transactions DNS SRV
  * Nouvel écran de premier lancement
  * Nouvel écran de création de clef : auto-remplissage du nom et du courriel d'après vos coordonnées Android
  * Chiffrement des fichiers : nouvelle et superbe conception, prise en charge du chiffrement de fichiers multiples
  * Nouvelles icônes d'état des clefs (par Brennan Novak)
  * Correctif important de bogue : l'importation de grandes collections de clefs à partir d'un fichier est maintenant possible
  * Notification montrant les phrases de passe en cache
  * Les clefs sont connectées aux contacts d'Android

Cette version ne serait pas possible sans le travail de Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar

## 2.7

  * Violet ! (Dominik, Vincent)
  * Nouvelle présentation de la visualisation des clefs (Dominik, Vincent)
  * Nouveaux boutons Android plats (Dominik, Vincent)
  * Correctifs de l'API (Dominik)
  * Importation de Keybase.io (Tim Bray)


## 2.6.1

  * Quelques correctifs de bogues de régression


## 2.6

  * Certifications des clefs (merci à Vincent Breitmoser)
  * Prise en charge clefs secrètes partielles de GnuPG (merci à Vincent Breitmoser)
  * Nouvelle conception de la vérification de signatures
  * Longueur de clef personnalisée (merci à Greg Witczak)
  * Correctif - Fonctionnalités partagées d'autres applis


## 2.5

  * Correctif - Déchiffrement des messages/fichiers symétriques OpenPGP
  * Écran de modification des clefs remanié (merci à Ash Hughes)
  * Nouvelle conception moderne pour les écrans de chiffrement/déchiffrement
  * API OpenPGP version 3 (comptes multiples d'api, correctifs internes, recherche de clefs)


## 2.4
Merci à tous les participants de « Google Summer of Code 2014 » qui ont rendu cette version riche en fonctions et sans bogue !
À part plusieurs petits correctifs, un nombre notable de correctifs ont été apportés par les personnes suivantes (par ordre alphabétique) :
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  * Nouvelle liste de clefs unifiée
  * Empreintes de clefs colorées
  * Prise en charge des ports des serveurs de clefs
  * Désactiver la possibilité de générer des clefs faibles
  * Encore plus de travail interne dans l'API
  * Certifier les ID utilisateurs
  * Requêtes des serveurs de clefs basées sur des sorties assimilables par la machine
  * Verrouiller les tiroirs de navigation sur les tablettes
  * Suggestion de courriels à la création de clefs
  * Rechercher dans les listes de clefs publiques
  * Et bien plus d'améliorations et de correctifs


## 2.3.1

  * Correctif d'urgence pour le plantage lors de la mise à niveau à partir d'anciennes versions


## 2.3

  * Suppressions de l'exportation non nécessaire des clefs publiques lors de l'exportation de clefs secrètes (merci à Ash Hughes)
  * Correctif - Définition de la date de péremption des clefs (merci à Ash Hughes)
  * Plus de correctifs internes affectant la modifications des clefs (merci à Ash hughes)
  * Interrogation des serveurs de clefs directement de l'écran d'importation
  * Correctif - Mise en page et du style des fenêtres de dialogue sur Android 2.2-3.0
  * Correctif - Plantage pour les clefs avec des ID utilisateur vides
  * Correctif - Plantage et listes vides en revenant de l'écran de signature
  * Bouncy Castle (bibliothèque cryptographique) mise à jour de 1.47 à 1.50 et compilée de la source
  * Correctif - Téléversement d'une clef de l'écran de signature


## 2.2

  * Nouvelle conception avec tiroir de navigation
  * Nouvelle conception de la liste des clefs publics
  * Nouvelle vue des clefs publics
  * Correctif de bogues d'importation de clefs
  * Certification croisée des clefs (merci à Ash Hughes)
  * Bonne gestion des mots de passe UTF-8 (merci à Ash Hughes)
  * Première version avec de nouvelles langues (merci aux contributeurs sur Transifex)
  * Correctif et amélioration du partage de clefs par codes QR
  * Vérification de la signature des paquets pour l'API


## 2.1.1

  * Mise à jour de l'API, préparation à l'intégration à Courriel K-9 Mail


## 2.1

  * Beaucoup de bogues corrigés
  * Nouvelle API pour les développeurs
  * Correctif du blogue PRNG par Google


## 2.0

  * Conception complètement repensée
  * Partage de clefs publiques par codes QR, faisceau NFC
  * Signer les clefs
  * Téléverser les clefs vers le serveur
  * Corrige des problèmes d'importation
  * Nouvelle API AIDL


## 1.0.8

  * Prise en charge de base du serveur de clefs
  * App2sd
  * Plus de choix pour le cache de la phrase de passe : 1, 2, 4, 8 heures
  * Traductions : norvégien (merci Sander Danielsen), chinois (merci Zhang Fredrick)
  * Correctifs de bogues
  * Optimisations


## 1.0.7

  * Problème corrigé avec la vérification de la signature des textes se terminant par un retour à la ligne
  * Plus de choix pour la durée de vie de la phrase de passe (20, 40, 60 min)


## 1.0.6

  * Correctif - Plantage lors de l'ajout de compte sur Froyo
  * Suppression sécurisée de fichiers
  * Option de suppression du fichier de clef après l'importation
  * Chiffrement/déchiffrement de flux (galerie, etc.)
  * Nouvelles options (langue, forcer les signatures v3)
  * Changements dans l'interface
  * Correctifs de bogues


## 1.0.5

  * Traduction allemande et italienne
  * Paquet beaucoup plus petit grâce à des sources BC réduites
  * Nouvelle IUG pour les préférences
  * Ajustement de la mise en page pour les localisations
  * Correctif de bogue de signature


## 1.0.4

  * Correction d'un autre plantage causé par quelque bogue SDK avec le constructeur de requêtes


## 1.0.3

  * Corrections de plantages durant le chiffrement/la signature et possiblement l'exportation de clefs


## 1.0.2

  * Listes de clefs filtrables
  * Présélection plus intelligente des clefs de chiffrement
  * Nouvelle gestion des intentions pour VIEW et SEND, permet le chiffrement/déchiffrement des fichiers du gestionnaires de fichiers
  * Correctifs et fonctions additionnelles (présélection des clefs) pour Courriel K-9-Mail, nouvelle version bêta proposée


## 1.0.1

  * Le listage des comptes Gmail ne fonctionnait pas dans 1.0.0, maintenant corrigé


## 1.0.0

  * Intégration à K-9 Mail, APG prenant en charge la version bêta de Courriel K-9 Mail
  * Prise en charge de plus de gestionnaires de fichiers (incluant ASTRO)
  * Traduction slovène
  * Nouvelle base de données, bien plus rapide, utilisation de la mémoire moindre
  * Intentions définies et fournisseur de contenu pour d'autres applis
  * Correctifs de bogues