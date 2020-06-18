#ifndef _MY_STRUCT_
#define _MY_STRUCT_

#include "uthash.h"

struct my_struct {
  const char* id;                    /* key, file MD5*/
  char* file_name;
  char* file_size;
  char* pieces_size;
  char* access;              /* IP:Port ' ' delimite les adresses */
  UT_hash_handle hh;         /* makes this structure hashable */
};

extern struct my_struct *users;
extern struct my_struct *tmp;

#ifdef NO_DECLTYPE
#define USER_ITER(el) \                          \
for(((el)=(users)), ((*(char**)(&(tmp)))=(char*)((users!=NULL)?(head)->hh.next:NULL)); \
  (el) != NULL; ((el)=(tmp)), ((*(char**)(&(tmp)))=(char*)((tmp!=NULL)?(tmp)->hh.next:NULL)))
#else
#define USER_ITER(el) \
for(((el)=(users)), ((tmp)=DECLTYPE(el)((users!=NULL)?(users)->hh.next:NULL));      \
  (el) != NULL; ((el)=(tmp)), ((tmp)=DECLTYPE(el)((tmp!=NULL)?(tmp)->hh.next:NULL)))
#endif

void add_user(const char* user_id, char *access, char* file_name, char* file_size, char* pieces_size);
struct my_struct *find_user(const char* user_id);
void delete_user(struct my_struct *user);
char* add_file_response(char* msg, struct my_struct* file);
#endif
