[//] : # (NOTE : veuillez mettre chaque phrase dans sa propre ligne. Transifex met chaque ligne dans son propre champ de traduction !)

## Confirmation de clef
Sans confirmation, vous ne pouvez pas être certain que la clef appartient à une personne déterminée.
La façon la plus simple de confirmer une clef est en balayant le code QR ou en l'échangeant par NFC.
Pour confirmer des clefs entre plus de deux personnes, nous suggérons d'utiliser la méthode d'échange de clef proposée pour vos clefs.

## État de la clef

<img src="status_signature_verified_cutout_24dp"/>  
Confirmée : vous avez déjà confirmé cette clef, p. ex. en balayant le code QR.  
<img src="status_signature_unverified_cutout_24dp"/>  
Non confirmée : cette clef n'a pas encore été confirmée. Vous ne pouvez pas être certain que la clef appartient à une personne déterminée.  
<img src="status_signature_expired_cutout_24dp"/>  
Expirée : cette clef n'est plus valide. Seul le propriétaire peut prolonger sa validité.  
<img src="status_signature_revoked_cutout_24dp"/>  
Révoquée : cette n'est plus valide. Elle a été révoquée par son propriétaire.

## Informations avancées
Avec OpenKeychain une « confirmation de clef » est effectuée en créant une certification d'après la norme OpenPGP.
Cette certification est une [« certification générique » (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) décrite ainsi dans la norme :
« L'émetteur de cette certification n'affirme aucunement que le certificateur a bien vérifié que le propriétaire de la clef est bel et bien la personne décrite par l'ID utilisateur »

Habituellement, les certifications (il en est de même avec les niveaux supérieurs de certification, tels que le « certifications positives » (0x13)) sont organisées dans la toile de confiance d'OpenPGP.
Notre modèle de confirmation de clef est un concept bien plus simple pour éviter les problèmes habituels de convivialité associés à cette toile de confiance.
Nous assumons que les clefs sont vérifiées seulement jusqu'à un certain degré qui est quand même assez utilisable pour être exécuté « à la volée ». 
Nous ne mettons pas non plus en place des signatures de confiance (potentiellement transitives) ou une base de données « ownertrust » comme dans GnuPG.
De plus, les clefs contenant au moins un ID utilisateur certifié par une clef de confiance seront marquées « confirmée » dans les listages de clefs.