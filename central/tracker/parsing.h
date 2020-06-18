#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// Récupère l'indice de la sous-chaine 'ct' dans la chaine 'cs'
int str_istr (const char *cs, const char *ct);

// Récupère la sous-chaine de 's' d'indice de début 'start' et d'indice de fin 'end'
char *str_sub (const char *s, unsigned int start, unsigned int end);

// Compte le nombre d'espaces
int count_space(char* s);

// Découpe la chaine 's' suivant le délimiteur 'ct'
char **str_split (char *s, const char *ct);

// Récupère et découpe la chaine entre [] de 's' en fonction des espaces
char **parse_string (char *s, char* st, char* en, char* cut);
