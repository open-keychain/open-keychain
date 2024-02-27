[//]: # (หมายเหตุ: กรุณาใส่ทุกประโยคลงในบรรทัดของตัวเอง Transifex จะใส่ทุกบรรทัดลงในช่องสำหรับแปลของมันเอง!)

## การยืนยันกุญแจ
หากไม่มีการยืนยัน คุณก็ไม่สามารถแน่ใจว่ากุญแจนั้นจะเป็นกุญแจสำหรับบุคคลคนหนึ่งจริงๆ อย่างที่ถูกอ้าง
วิธีที่ง่ายที่สุดในการยืนยันกุญแจคือการสแกนรหัส QR หรือแลกเปลี่ยนมันผ่าน NFC
ในการยืนยันกุญแจระหว่างคนมากกว่าสองคน เราแนะนำให้ใช้วิธีการแลกเปลี่ยนกุญแจที่กุญแจของคุณสามารถใช้ได้

## สถานะกุญแจ

<img src="status_signature_verified_cutout_24dp"/>  
ยืนยันแล้ว: คุณได้ยืนยันกุญแจนี้แล้ว เช่น โดยการสแกนรหัส QR  
<img src="status_signature_unverified_cutout_24dp"/>  
Unconfirmed (ไม่ยืนยัน): กุญแจนี้ยังไม่ถูกยืนยัน คุณไม่สามารถแน่ใจได้ว่ากุญแจดอกนี้จะเป็นกุญแจสำหรับบุคคลคนหนึ่งจริงๆ  
<img src="status_signature_expired_cutout_24dp"/>  
Expired (หมดอายุ): กุญแจนี้ใช้ไม่ได้อีกต่อไปแล้ว มีเฉพาะเจ้าของของมันที่จะขยายอายุมันได้  
<img src="status_signature_revoked_cutout_24dp"/>  
Revoked (ถูกเพิกถอน): กุญแจนี้ใช้ไม่ได้อีกต่อไปแล้ว มันถูกเพิกถอนโดยเจ้าของของมัน

## ข้อมูลขั้นสูง
"การยืนยันกุญแจ" ใน OpenKeychain นั้นทำโดยการสร้างใบรับรองตามมาตรฐาน OpenPGP
ใบรับรองดังกล่าวคือ ["generic certification (0x10)"](http://tools.ietf.org/html/rfc4880#section-5.2.1) ซึ่งในถูกอธิบายในมาตรฐานดังนี้:
"ผู้ออกใบรับรองนี้ไม่ได้อ้างยืนยันอย่างเฉพาะเจาะจง ว่าผู้รับรองได้ตรวจสอบอย่างไรว่า เจ้าของกุญแจนั้นเป็นบุคคลตามที่อธิบายใน User ID จริง"

Traditionally, certifications (also with higher certification levels, such as "positive certifications" (0x13)) are organized in OpenPGP's Web of Trust.
Our model of key confirmation is a much simpler concept to avoid common usability problems related to this Web of Trust.
We assume that keys are verified only to a certain degree that is still usable enough to be executed "on the go".
We also do not implement (potentially transitive) trust signatures or an ownertrust database like in GnuPG.
Furthermore, keys which contain at least one user ID certified by a trusted key will be marked as "confirmed" in the key listings.