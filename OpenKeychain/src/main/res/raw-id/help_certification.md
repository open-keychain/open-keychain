[//]: # (NOTE: Please put every sentence in its own line, Transifex puts every line in its own translation field!)

## Konfirmasi Kunci
Tanpa konfirmasi, anda tidak bisa yakin bahwa suatu kunci memang benar kepunyaan seseorang.
Cara paling mudah untuk mengkonfirmasi kunci adalah dengan memindai kode QR atau bertukar kunci melalui NFC.
To confirm keys between more than two persons, we suggest using the key exchange method available for your keys.

## Status Kunci

<img src="status_signature_verified_cutout_24dp"/>  
Terkonfirmasi : Anda telah mengkonfirmasi kunci ini, contoh, seperti melalui kode QR.  
<img src="status_signature_unverified_cutout_24dp"/>  
Tidak terkonfirmasi: Kunci ini belum dikonfirmasi. Anda tidak bisa yakin bahwa kunci ini memang benar kepunyaan seseorang.  
<img src="status_signature_expired_cutout_24dp"/>  
Tidak berlaku: Kunci ini sudah tidak lagi berlaku. Hanya pemilik yang mampu memperpanjang masa berlakunya.  
<img src="status_signature_revoked_cutout_24dp"/>  
Dibatalkan: Kunci ini sudah tidak berlaku lagi, telah dibatalkan oleh pemilik.

## Informasi Tambahan
"Konfirmasi kunci" di OpenKeychain diimplementasikan dengan membuat sertifikat sesuai dengan standar OpenPGP
Sertifikasi ini adalah ["generic certification (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) dideskripsikan dengan:
"Sang pengeluar sertifikat tidak tahu seberapa baik sang pemeriksa telah memeriksa bahwa sang pemilik kunci memang sesuai deskripsi yang ada."

Umumnya, sertifikasi (termasuk sertifikasi tingkat tinggi, seperti "sertifikasi positif" (0x13)) diorganisir di Web of Trust OpenPGP.
Model konfirmasi kunci kami lebih mudah untuk menghindari masalah masalah dengan Web of Trust ini.
Kami berasumsi bahwa kunci tersebut telah diverifikasi hanya menurut standar tertentu yang cukup untuk dipakai.
Kami juga tidak mengimplementasikan tanda kepercayaan atau database kepercayaan seperti di GnuPG.
Lebih lanjut lagi, kunci yang memiliki setidaknya satu identitas yang dikonfirmasi oleh kunci yang terpecaya akan ditandai sebagai "terkonfirmasi" di daftar.