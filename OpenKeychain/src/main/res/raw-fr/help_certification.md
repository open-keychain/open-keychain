[//]: # (NOTE : veuillez mettre chaque phrase sur sa propre ligne. Transifex met chaque ligne dans son propre champ de traduction !)

## Confirmation de clé
Sans confirmation, vous ne pouvez pas être certain qu’une clé correspond à une personne déterminée.
La façon la plus simple de confirmer une clé est en lisant le code QR ou en l’échangeant par CCP.
Pour confirmer des clés entre plus de deux personnes, nous suggérons d’utiliser la méthode d’échange de clés proposée pour vos clés.

## État de la clé

<img src="status_signature_verified_cutout_24dp"/>  
Confirmée : vous avez déjà confirmé cette clé, p. ex. en lisant le code QR.  
<img src="status_signature_unverified_cutout_24dp"/>  
Non confirmée : cette clé n’a pas encore été confirmée. Vous ne pouvez pas être certain que la clé correspond à une personne déterminée.  
<img src="status_signature_expired_cutout_24dp"/>  
Expirée : cette clé n’est plus valide. Seul le propriétaire peut prolonger sa validité.  
<img src="status_signature_revoked_cutout_24dp"/>  
Révoquée : cette n’est plus valide. Elle a été révoquée par son propriétaire.

## Informations avancées
Avec OpenKeychain, une « confirmation de clé » est effectuée en créant une certification d’après la norme OpenPGP.
Cette certification est une [« certification générique » (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) décrite ainsi dans la norme :
« L’émetteur de cette certification n’affirme aucunement que le certificateur a bien vérifié que le propriétaire de la clé est bel et bien la personne décrite par l’ID utilisateur »

Habituellement, les certifications (il en est de même avec les niveaux supérieurs de certification, tels que les « certifications positives » [0x13]) sont organisées dans la toile de confiance d’OpenPGP.
Notre modèle de confirmation de clé est un concept bien plus simple pour éviter les problèmes habituels de convivialité associés à cette toile de confiance.
Nous supposons que les clés sont vérifiées à concurrence d’un certain degré qui est quand même assez utilisable pour être exécuté « à la volée ».
Nous ne mettons pas non plus en place des signatures de confiance (potentiellement transitives) ou une base de données « ownertrust » comme dans GnuPG.
De plus, les clés contenant au moins un ID utilisateur certifié par une clé de confiance seront marquées « confirmée » dans les listages de clés.