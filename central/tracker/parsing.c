#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "parsing.h"

// Récupère l'indice de la sous-chaine 'ct' dans la chaine 'cs'
int str_istr (const char *cs, const char *ct){
   int index = -1;

   if (cs != NULL && ct != NULL){
      char *ptr_pos = NULL;

      ptr_pos = strstr (cs, ct);
      if (ptr_pos != NULL){
         index = ptr_pos - cs;
      }
   }
   return index;
}

// Récupère la sous-chaine de 's' d'indice de début 'start' et d'indice de fin 'end'
char *str_sub (const char *s, unsigned int start, unsigned int end){
   char* new_s = NULL;
   if (s != NULL && start < end){
      new_s = malloc(sizeof(char) * (end - start + 2));
      if (new_s != NULL){
         int i;
         for (i = start; i <= end; i++){
            new_s[i-start] = s[i];
         }
         new_s[i-start] = '\0';
      }else{
         fprintf (stderr, "Memoire insuffisante\n");
         exit (EXIT_FAILURE);
      }
   }
   return new_s;
}

// Compte le nombre d'espaces
int count_space(char *s){
  int res = 0;
  if (s != NULL){
    for(int i = 0; i < strlen(s); i++){
      if (s[i] == ' '){
        res += 1;
      }
    }
  }
  return res;
}

// Découpe la chaine 's' suivant le délimiteur 'ct'
char **str_split (char *s, const char *ct){
   char **tab = NULL;

   if(ct == NULL){
      tab = malloc(sizeof(*tab));
      tab[0] = s;
      tab[1] = NULL;
      return tab;
   }

   if (s != NULL && ct != NULL){
      int i;
      char *cs = NULL;
      size_t size = 1;

      for (i = 0; (cs = strtok (s, ct)); i++){
         if (size <= i + 1){
            void *tmp = NULL;
            size <<= 1;
            tmp = realloc (tab, sizeof (*tab) * size);
            if (tmp != NULL){
               tab = tmp;
            }else{
               fprintf (stderr, "Memoire insuffisante\n");
               free (tab);
               tab = NULL;
               exit (EXIT_FAILURE);
            }
         }
         tab[i] = cs;
         s = NULL;
      }
      tab[i] = NULL;
   }
   return tab;
}

// Récupère et découpe la chaine entre [] de 's' en fonction des espaces
char** parse_string(char* s, char* st, char* en, char* cut){
  char** tab;
  int start = str_istr(s, st);
  int end = str_istr(s, en);
  if (end-start > 2){
    char* files = str_sub(s, start+1, end-1);
    if(strstr(files, cut) == NULL) tab = str_split(files, NULL);
    else tab = str_split(files, cut);
  }else{
    tab = NULL;
  }
  return tab;
}
