<html>
<head>
 <title>Message</title>
 <link rel="stylesheet" type="text/css" href="contact.css">
</head>
<body>
<h1>Sending a message</h1>

<p>To send a message, please enter your e-mail address, then a subject and the content of your message. Then click on "Send".</p>

<form action="/cgi-bin/contact2.pl" type="POST">
<input type="hidden" name="recipient" value="<?php echo ( $_GET['recipient'] ? $_GET['recipient'] : 'thbz') ?>">
<input type="hidden" name="required" value="email,subject">
<input type="hidden" name="title" value="Merci d'avoir envoyé ce message">

<table border="0" class="formulaire">
 <tr>
   <td>Your e-mail address (mandatory):</td>
   <td><input type="text" width="70" name="email" value="" /></td>
 </tr>
 <tr>
   <td>Your name (not mandatory) :</td>
   <td><input type="text" width="70" name="realname" value="" /></td>
 </tr>
 <tr>
   <td>Subject :</td>
   <td><input type="text" width="70" name="subject" value="" /></td>
 </tr>
 <tr>
   <td>Content :</td>
   <td><textarea rows="10" cols="70" name="contenu">Enter here the contents of this message.</textarea></td>
 </tr>
 <tr>
  <td colspan="2" align="center">
   <input type="submit" value="Envoyer">
   <a href="<?php echo $_GET['retour'] ?>">Cancel</a>
  </td>
 </tr>
</table>
</form>

<p>Thank you for using this old-fashioned e-mail form! I will implement a more modern form one of these days (or maybe not).</p>

<div class="signature">Thierry Bézecourt</div>

</body>
</html>