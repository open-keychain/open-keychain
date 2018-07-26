[//]: # (NOTE : veuillez mettre chaque phrase sur sa propre ligne. Transifex met chaque ligne dans son propre champ de traduction !)

## 5.1
  * Prise en charge de Ledger Nano S
  * Prise en charge de la recherche dans l’annuaire de clés Web (WKD)
  * Correctif d’un problème de sécurité possible de l’API

## 5.0
  * Prise en charge améliorée d’Autocrypt

## 4.9

  * Prise en charge de Curve25519
  * Prise en charge améliorée des jetons de sécurité

## 4.8

  * Prise en charge améliorée des jetons USB : Gnuk, modèles Nitrokey, modèles YubiKey 4
  * Fonction pour trouver l’emplacement du lecteur CCP de l’appareil

## 4.7

  * Importation améliorée à partir du presse-papiers
  * Nouvel assistant de création de clé pour les jetons de sécurité
  * Suppression du paramètre de cache « durée de vie » des mots de passe


## 4.6

  * Importation de vos clés grâce à notre nouveau mécanisme de transfert Wi-Fi sécurisé


## 4.5

  * Description détaillée des problèmes de sécurité
  * Affiche l’état du serveur de clés par clé
  * Prise en charge d’EdDSA
  * Correctif - pgp.mit.edu (nouveau certificat)


## 4.4

  * Le nouvel état des clés affiche en détail pourquoi une clé est considérée comme non fiable ou défectueuse


## 4.3

  * Meilleure prise en charge des clé de grande taille
  * Correctif - Importation des fichiers Gpg4win avec des encodages brisés


## 4.2

  * Prise en charge expérimentale du chiffrement à courbe elliptique avec des jetons de sécurité
  * Nouvelle conception de l’écran d’importation des clés
  * Amélioration de la conception des listes de clés
  * Prise en charge des adresses « onion » de serveurs de clés


## 4.1

  * Meilleure détection des courriels et autres contenus à l’ouverture


## 4.0

  * Prise en charge expérimentale des jetons de sécurité par USB
  * Autoriser le changement de mot de passe des clés dépouillées


## 3.9

  * Détection et gestion de données texte
  * Améliorations des performances
  * Améliorations de l’IG pour la gestion des jetons de sécurité


## 3.8

  * Nouvelle conception de la modification des clés
  * Choisir les délais de mémorisation individuellement lors de la saisie des mots de passe
  * Importation de clé Facebook


## 3.7

  * Prise en charge améliorée d’Android 6 (autorisations, intégration dans la sélection de textes)
  * API : version 10


## 3.6

  * Sauvegardes chiffrées
  * Correctifs de sécurité suite à un audit externe de sécurité
  * Assistant de création de clés YubiKey NEO
  * Prise en charge interne MIME de base
  * Synchronisation automatique des clés
  * Fonction expérimentale : relier les clés aux comptes GitHub, Twitter
  * Fonction expérimentale : confirmation des clés par des phrases
  * Fonction expérimentale : thème foncé
  * API : version 9


## 3.5

  * révocation de la clé lors de la suppression de la clé
  * Vérifications améliorées à la recherche d’une cryptographie non fiable
  * Correctif - Ne pas fermer OpenKeychain après une réussite de l’assistant de première utilisation
  * API : version 8


## 3.4

  * Téléchargement anonyme de clés avec Tor
  * Prise en charge des serveurs mandataires
  * Meilleur gestion des erreurs YubiKey


## 3.3

  * Nouvel écran de déchiffrement
  * Déchiffrement simultané de plusieurs fichiers
  * Meilleure gestion des erreurs YubiKey


## 3.2

  * Première version avec prise en charge complète de la YubiKey, proposée dans l’interface utilisateur : modifier les clés, relier la YubiKey aux clés…
  * Conception matérielle
  * Intégration de la lecture de code QR (de nouvelles autorisations sont exigées)
  * Amélioration de l’assistant de création de clé
  * Correctif - Contacts manquants après la synchro
  * Android 4 exigé
  * Nouvelle conception de l’écran des clés
  * Simplification des préférences cryptographiques, meilleure sélection de codes de chiffrement sécurisés
  * API : signatures détachées, sélection libre de la clé de signature…
  * Correctif - Certaines clés valides apparaissaient comme révoquées ou expirées
  * Ne pas accepter de signatures par des sous-clés expirées ou révoquées
  * Prise en charge de keybase.io dans la vue avancée
  * Méthode pour mettre toutes les clés à jour en même temps


## 3.1.2

  * Correctif - Exportation des clés vers des fichiers (vraiment, maintenant)


## 3.1.1

  * Correctif - Exportation des clés vers des fichiers (elles n’étaient écrites que partiellement)
  * Correctif - Plantage sur Android 2.3


## 3.1

  * Correctif - Plantage sur Android 5
  * Nouvel écran de certification
  * Échange sécurisé directement de la liste des clés (bibliothèque SafeSlinger)
  * Nouveau flux de programme pour les codes QR
  * Écran de déchiffrement redessiné
  * Nouveaux agencement et couleurs d’icônes
  * Importation des clés secrètes corrigée de Symantec Encryption Desktop
  * Prise en charge expérimentale de la YubiKey : les ID de sous-clés sont maintenant vérifiés correctement


## 3.0.1

  * Meilleure gestion de l’importation de nombreuses clés
  * Sélection des sous-clés améliorée


## 3.0

  * Des applis compatibles installables sont proposées dans la liste des applis
  * Nouvelle conception pour les écrans de déchiffrement
  * Nombreux correctifs d’importation des clés, corrigent aussi les clés dépouillées
  * Accepter et afficher les drapeaux d’authentification des clés
  * Interface utilisateur pour générer des clés personnalisées
  * Corrigé - Certificats de révocation des ID utilisateurs
  * Nouvelle recherche nuagique (sur les serveurs de clés habituels et dans keybase.io)
  * Prise en charge du dépouillement des clés dans OpenKeychain
  * Prise en charge expérimentale de la YubiKey : prise en charge de la génération de signature et le déchiffrement


## 2.9.2

  * Correctif - Clés brisées dans 2.9.1
  * Prise en charge expérimentale de la YubiKey : le déchiffrement fonctionne maintenant avec l’API


## 2.9.1

  * Partage de l’écran de chiffrement en deux
  * Correctif - Gestion des drapeaux de clés (prend maintenant en charge les clés Mailvelope 0.7)
  * Gestion des phrases de passe améliorée
  * Partage de clés par SafeSlinger
  * Prise en charge expérimentale de la YubiKey : préférence pour permettre d’autres NIP, seule la signature par l’API OpenPGP fonctionne actuellement, mais pas dans OpenKeychain
  * Correctif - Utilisation de clés dépouillées
  * SHA256 par défaut pour la compatibilité
  * L’API des intentions a changé, voir https://github.com/open-keychain/open-keychain/wiki/Intent-API
  * L’API d’OpenPGP gère maintenant les clés révoquées/expirées et retourne tous les ID utilisateurs


## 2.9

  * Correction des plantages présents dans v2.8
  * Prise en charge expérimentale CCE
  * Prise en charge expérimentale de la YubiKey : signature seulement avec les clés importées


## 2.8

  * Tellement de bogues ont été réglés dans cette version que nous nous concentrons sur les nouvelles caractéristiques principales.
  * Modification des clés : nouvelle et superbe conception, révocations des clés
  * Importation des clés : nouvelle et superbe conception, connexion sécurisé aux serveurs de clés par hkps, résolution des serveurs de clés par transactions DNS SRV
  * Nouvel écran de premier lancement
  * Nouvel écran de création de clé : auto-remplissage du nom et du courriel d’après vos coordonnées Android
  * Chiffrement des fichiers : nouvelle et superbe conception, prise en charge du chiffrement de fichiers multiples
  * Nouvelles icônes d’état des clés (par Brennan Novak)
  * Correctif important de bogue : l’importation de grandes collections de clés à partir d’un fichier est maintenant possible
  * Notification montrant les phrases de passe en cache
  * Les clés sont connectées aux contacts d’Android

Cette version ne serait pas possible sans le travail de Vincent Breitmoser (GSoC 2014), mar-v-in (GSoC 2014), Daniel Albert, Art O Cathain, Daniel Haß, Tim Bray, Thialfihar

## 2.7

  * Violet ! (Dominik, Vincent)
  * Nouvelle présentation de la visualisation des clés (Dominik, Vincent)
  * Nouveaux boutons Android plats (Dominik, Vincent)
  * Correctifs de l’API (Dominik)
  * Importation de Keybase.io (Tim Bray)


## 2.6.1

  * Quelques correctifs de bogues de régression


## 2.6

  * Certifications des clés (merci à Vincent Breitmoser)
  * Prise en charge clés secrètes partielles de GnuPG (merci à Vincent Breitmoser)
  * Nouvelle conception de la vérification de signatures
  * Longueur de clé personnalisée (merci à Greg Witczak)
  * Correctif - Fonctionnalités partagées d’autres applis


## 2.5

  * Correctif - Déchiffrement des messages/fichiers symétriques OpenPGP
  * Écran de modification des clés remanié (merci à Ash Hughes)
  * Nouvelle conception moderne pour les écrans de chiffrement/déchiffrement
  * API OpenPGP version 3 (comptes multiples d’api, correctifs internes, recherche de clés)


## 2.4
Merci à tous les participants de « Google Summer of Code 2014 » qui ont rendu cette version riche en fonctions et sans bogue !
À part plusieurs petits correctifs, un nombre notable de correctifs ont été apportés par les personnes suivantes (par ordre alphabétique) :
Daniel Hammann, Daniel Haß, Greg Witczak, Miroojin Bakshi, Nikhil Peter Raj, Paul Sarbinowski, Sreeram Boyapati, Vincent Breitmoser.

  * Nouvelle liste de clés unifiée
  * Empreintes de clés colorées
  * Prise en charge des ports des serveurs de clés
  * Désactiver la possibilité de générer des clés faibles
  * Encore plus de travail interne dans l’API
  * Certifier les ID utilisateurs
  * Requêtes des serveurs de clés basées sur des sorties assimilables par la machine
  * Verrouiller les tiroirs de navigation sur les tablettes
  * Suggestion de courriels à la création de clés
  * Rechercher dans les listes de clés publiques
  * Et bien plus d’améliorations et de correctifs


## 2.3.1

  * Correctif d’urgence pour le plantage lors de la mise à niveau à partir d’anciennes versions


## 2.3

  * Suppressions de l’exportation non nécessaire des clés publiques lors de l’exportation de clés secrètes (merci à Ash Hughes)
  * Correctif - Définition de la date de péremption des clés (merci à Ash Hughes)
  * Plus de correctifs internes affectant la modifications des clés (merci à Ash hughes)
  * Interrogation des serveurs de clés directement de l’écran d’importation
  * Correctif - Mise en page et du style des fenêtres de dialogue sur Android 2.2-3.0
  * Correctif - Plantage pour les clés avec des ID utilisateur vides
  * Correctif - Plantage et listes vides en revenant de l’écran de signature
  * Bouncy Castle (bibliothèque cryptographique) mise à jour de 1.47 à 1.50 et compilée de la source
  * Correctif - Téléversement d’une clé de l’écran de signature


## 2.2

  * Nouvelle conception avec tiroir de navigation
  * Nouvelle conception de la liste des clés publics
  * Nouvelle vue des clés publics
  * Correctif de bogues d’importation de clés
  * Certification croisée des clés (merci à Ash Hughes)
  * Bonne gestion des mots de passe UTF-8 (merci à Ash Hughes)
  * Première version avec de nouvelles langues (merci aux contributeurs sur Transifex)
  * Correctif et amélioration du partage de clés par codes QR
  * Vérification de la signature des paquets pour l’API


## 2.1.1

  * Mise à jour de l’API, préparation à l’intégration à Courriel K-9 Mail


## 2.1

  * Beaucoup de bogues corrigés
  * Nouvelle API pour les développeurs
  * Correctif du blogue PRNG par Google


## 2.0

  * Conception complètement repensée
  * Partage de clés publiques par codes QR, faisceau CCP
  * Signer les clés
  * Téléverser les clés vers le serveur
  * Corrige des problèmes d’importation
  * Nouvelle API AIDL


## 1.0.8

  * Prise en charge de base du serveur de clés
  * App2sd
  * Plus de choix pour le cache de la phrase de passe : 1, 2, 4, 8 heures
  * Traduction : Bokmål (merci à Sander Danielsen), Chinois (merci à Zhang Fredrick)
  * Correctifs de bogues
  * Optimisations


## 1.0.7

  * Problème corrigé avec la vérification de la signature des textes se terminant par un retour à la ligne
  * Plus de choix pour la durée de vie de la phrase de passe (20, 40, 60 min)


## 1.0.6

  * Correctif - Plantage lors de l’ajout de compte sur Froyo
  * Suppression sécurisée de fichiers
  * Option de suppression du fichier clé après importation
  * Chiffrement/déchiffrement de flux (galerie, etc.)
  * Nouvelles options (langue, forcer les signatures v3)
  * Changements dans l’interface
  * Correctifs de bogues


## 1.0.5

  * Traduction allemande et italienne
  * Paquet beaucoup plus petit grâce à des sources BC réduites
  * Nouvelle IUG pour les préférences
  * Ajustement de la mise en page pour les localisations
  * Correctif de bogue de signature


## 1.0.4

  * Correction d’un autre plantage causé par quelque bogue SDK avec le constructeur de requêtes


## 1.0.3

  * Corrections de plantages durant le chiffrement/la signature et possiblement l’exportation de clés


## 1.0.2

  * Listes de clés filtrables
  * Présélection plus intelligente des clés de chiffrement
  * Nouvelle gestion des intentions pour VIEW et SEND, permet le chiffrement/déchiffrement des fichiers du gestionnaires de fichiers
  * Correctifs et fonctions additionnelles (présélection des clés) pour Courriel K-9 Mail, nouvelle version bêta proposée


## 1.0.1

  * Le listage des comptes Gmail ne fonctionnait pas dans 1.0.0, maintenant corrigé


## 1.0.0

  * Intégration à Courriel K-9 Mail, APG prenant en charge la version bêta de Courriel K-9 Mail
  * Prise en charge de plus de gestionnaires de fichiers (incluant ASTRO)
  * Traduction slovène
  * Nouvelle base de données, bien plus rapide, utilisation de la mémoire moindre
  * Intentions définies et fournisseur de contenu pour d’autres applis
  * Correctifs de bogues