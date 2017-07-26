[//]: # (NOTA: Si prega di mettere ogni frase in una propria linea, Transifex mette ogni riga nel proprio campo di traduzione!)

## Conferma chiave
Senza conferma, non puoi essere sicuro la chiave veramente corrisponda a una persona specifica.
Il modo più semplice per confermare una chiave è la scansione del codice QR o lo scambio via NFC.
Per confermare le chiavi tra più di due persone, consigliamo di usare il metodo di scambio chiavi disponibile per le tue chiavi.

## Stato chiave

<img src="status_signature_verified_cutout_24dp"/>  
Confermato: Hai già confermato questa chiave, ad esempio, attraverso la scansione del codice QR.  
<img src="status_signature_unverified_cutout_24dp"/>  
Non confermato: Questa chiave non è ancora stata confermata. Non si può essere sicuri se la chiave corrisponde davvero a una persona specifica.  
<img src="status_signature_expired_cutout_24dp"/>  
Scaduta: Questa chiave non è più valida. Solo il proprietario può estendere la sua validità.  
<img src="status_signature_revoked_cutout_24dp"/>  
Revoca: Questa chiave non è più valida. È stata revocata dal suo proprietario.

## Informazioni avanzate
Una "conferma chiave" in OpenKeychain è attuata mediante la creazione di una certificazione secondo lo standard OpenPGP.
Questa certificazione è un ["generic certification (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) indicata nella norma per:
"L'emittente di tale certificazione non fa alcuna particolare asserzione da quanto accurato il certificatore ha verificato che il proprietario della chiave è infatti la persona descritta dal ID utente."

Tradizionalmente, certificazioni (anche con livelli di certificazione più elevati, come "certificazioni positivi" (0x13)) sono organizzate su OpenPGP nel web di confidenza ("Web of trust").
Il nostro modello di conferma chiave è un concetto molto più semplice per evitare problemi di usabilità comuni relativi a questo Web of Trust.
Assumiamo che le chiavi vengono verificate solo fino ad un certo grado che è ancora abbastanza utilizzabile da eseguire "in movimento".
Inoltre, non implementiamo (potenzialmente transitive) firme fiduciarie o un database ownertrust come in GnuPG.
Inoltre, chiavi che contengono almeno un ID utente certificata da una chiave di fiducia saranno contrassegnati come "confermate" negli elenchi principali.