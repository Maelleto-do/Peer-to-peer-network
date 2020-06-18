#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include "parsing.h"
#include "uthash.h"
#include "hash_table.h"

struct my_struct *users;    /* important! initialize to NULL */
struct my_struct *tmp;    /* important! initialize to NULL */

void add_user(const char* user_id, char *access, char* file_name, char* file_size, char* pieces_size) {
  struct my_struct *s;

  HASH_FIND_STR( users, user_id, s );  /* s: output pointer */
  if (s==NULL) {
    s = malloc(sizeof(struct my_struct));
    s->id = user_id;
    s->file_name = malloc(sizeof(char) * strlen(file_name));
    strcpy(s->file_name, file_name);
    s->file_size = malloc(sizeof(char) * strlen(file_size));
    strcpy(s->file_size, file_size);
    s->pieces_size = malloc(sizeof(char) * strlen(pieces_size));
    strcpy(s->pieces_size, pieces_size);
    s->access = malloc(sizeof(char) * (strlen(access)));
    strcpy(s->access, access);
    HASH_ADD_KEYPTR( hh, users, s->id, strlen(s->id), s );  /* id: name of key field */
  } else {
    char* tmp = malloc(sizeof(char)*strlen(s->access));
    strcpy(tmp, s->access);
    char ** current = str_split(s->access, " ");
    for(int i = 0; i <= count_space(s->access); i++){
      if (strcmp(current[i], access) == 0){
        s->access = tmp;
        return;
      }
    }
    s->access = tmp;
    s->access = realloc(s->access, sizeof(char) * (strlen(access) + strlen(s->access) + 1));
    strcat(s->access, " ");
    strcat(s->access, access);
  }
}

struct my_struct *find_user(const char* user_id) {
  struct my_struct *s;

  HASH_FIND_STR( users, user_id, s );  /* s: output pointer */
  return s;
}

void delete_user(struct my_struct *user) {
    HASH_DEL(users, user);  /* user: pointer to deletee */
    free(user);             /* optional; it's up to you! */
}
