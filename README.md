README pour l'utilisation du projet de peer-to-peer

(by P. Chevallier, C. Bertin, M. Toy-Riont, C. Le Métayer)

Application pour le partage de fichiers en mode pair à pair.
Seul le dossier 'central' contient du code. /!\ Se placer dans le dossier 'central' /!\


## Configuration du tracker et des peers

```make configuration``` lance une interface graphique pour configurer les paramètres du tracker (fichier Config/config_tracker.ini)

Permet d'ajouter des peers et d'update leurs paramètres (fichier Config/config_peerXXXX.ini)

Permet aussi de supprimer tous les fichiers de configuration .ini



## Compilation et Exécution du Tracker

```make execTracker``` compile et exécute le tracker sur le port renseigné dans son fichier de config. Configurer le tracker préalablement



## Compilation et Exécution des Peers 

```make execPeer``` compile et exécute un peer lancé sur le port 8000 par défaut. Changeable à l'exécution et n'est valide que pour un port
préalablement configuré.

```make launchPeer``` pour seulement exécuter le peer



## Nettoyage du dossier 

```make clean``` pour supprimer les fichiers issus de la compilation dans le dossier 'build'

```make rm logs``` pour supprimer les fichiers de logs du dossier 'Logs' (générés automatiquement lors du fonctionnement du programme)
