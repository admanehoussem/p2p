# MiniP P2P Chat (Java)

Petit projet de chat pair-à-pair (P2P) en Java — console.

Fonctionnalités minimales:
- Écoute sur un port spécifié
- Connexion à un pair via `/connect <host> <port>`
- Envoi de messages texte à tous les pairs connectés
- Commande `/exit` pour quitter

Compilation (PowerShell):

```powershell
# depuis le répertoire du projet
javac -d out src/main/java/minip/p2pchat/ChatPeer.java
java -cp out minip.p2pchat.ChatPeer 5000
```

Exemples d'utilisation:
- Ouvrir 2 terminaux.
- Dans le premier: `java -cp out minip.p2pchat.ChatPeer 5000`
- Dans le deuxième: `java -cp out minip.p2pchat.ChatPeer 5001` puis ` /connect 127.0.0.1 5000`
- Tapez des messages simples; ils seront relayés entre pairs.

Notes:
- Cette implémentation est volontairement simple (pas de chiffrement, pas de découverte auto, un seul socket par connexion). Elle sert de base pédagogique.
