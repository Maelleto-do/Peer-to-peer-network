#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <netinet/in.h>
#include <pthread.h>
#include <time.h>
#include <arpa/inet.h>

#include "uthash.h"
#include "hash_table.h"
#include "parsing.h"

#define BUFFER_SIZE 1024
#define POOL_SIZE 50
#define TRUE 1
#define FALSE 0
#define IPPORT_SIZE 50

void error(char *msg){
  perror(msg);
  exit(1);
}

/* #### WORKERS/THREADS ########################################################### */

typedef struct worker {
  pthread_t thread;
  int socket;
  int available;
  // char* add_port;
} worker_t;

worker_t w_pool[POOL_SIZE];
static char* corr[POOL_SIZE];
int portno;

// Initialise le tableau de workers, en les mettant tous disponibles
void initialize_workers(worker_t pool[]){
  for (size_t i = 0; i<POOL_SIZE; i++){
    pool[i].available = TRUE;
    corr[i] = malloc(sizeof(char)*IPPORT_SIZE);
    strcpy(corr[i], "empty");
  }
}

// Retourne l'indice du premier worker disponible, et -1 si aucun worker disponible
int first_free_worker(worker_t pool[]){
  for (size_t i = 0; i < POOL_SIZE; i++){
    if (pool[i].available == TRUE) return i;
  }
  return -1;
}

/* ##### LOGS ###################################################################### */

void writeRequestLog(int portTracker, char* request, char* address_port){
  char filename[16]; // Logs/logXXXX.txt
  time_t rawtime;
  struct tm * timeinfo;
  FILE *fptr;

  sprintf(filename,"Logs/log%d.txt",portTracker);
  time ( &rawtime );
  timeinfo = localtime ( &rawtime );

  char * time_cut = asctime(timeinfo);
  time_cut[strlen(time_cut)-1] = 0; //removes the \n
  char * req_cut = request;
  req_cut[strlen(req_cut)-1] = 0; //idem

  fptr = fopen(filename, "a"); // a for appending
  if(fptr == NULL) //if file does not exist, create it
  {
    fptr = fopen(filename, "w");
  }
  fprintf(fptr,"[%s] request %s received from %s\n", time_cut, req_cut, address_port);
  fclose(fptr);
}

void writeResponseLog(int portTracker, char* response, char* address_port){
  char filename[16]; // Logs/logXXXX.txt
  time_t rawtime;
  struct tm * timeinfo;
  FILE *fptr;
  sprintf(filename,"Logs/log%d.txt",portTracker);
  time ( &rawtime );
  timeinfo = localtime ( &rawtime );

  char * time_cut = asctime(timeinfo);
  time_cut[strlen(time_cut)-1] = 0; //removes the \n
  char res_cut[strlen(response)]; // sinon ça fait seg_fault :(
  strcpy(res_cut,response);
  res_cut[strlen(res_cut)-1] = 0; //idem

  fptr = fopen(filename, "a"); // a for appending
  if(fptr == NULL) //if file does not exist, create it
  {
    fptr = fopen(filename, "w");
  }
  fprintf(fptr,"[%s] response %s sent to %s\n", time_cut, res_cut, address_port);
  fclose(fptr);
}

/* #### REQUESTS ####################################################################### */

int getcommand(char* buf){ // Retourne int, car problèmes probables de mémoire avec char*
  // à mettre dans le switch case de socket_thread ?
  if(strncmp(buf,"announce",strlen("announce")-1) == 0){
    printf("announce received\n");
    return 1;
  }
  else if(strncmp(buf,"look",strlen("look")-1) == 0){
    printf("look received\n");
    return 2;
  }
  else if(strncmp(buf,"getfile",strlen("getfile")-1) == 0){
    printf("getfile received\n");
    return 3;
  }
  else if(strncmp(buf,"update",strlen("update")-1) == 0){
    printf("update received\n");
    return 7;
  }
  else if(strncmp(buf,"exit",strlen("exit")-1) == 0){
    printf("exit received\n");
    return 0;
  }
  else{
    return -1;
  }
}

/* #### MAIN ####################################################################### */
// Fonction d'affichage de la liste des peers connectés
void * prompt_list_peers(void * arg){
  while(1){
    sleep(30);
    char* intro = "Peers connectés : ";
    int len = strlen(intro);
    char* prompt = malloc(len*sizeof(char));
    strcpy(prompt, intro);

    for(int i = 0; i < POOL_SIZE; i++){
      if (strcmp(corr[i], "empty") != 0){
        len += strlen(corr[i])+3;
        prompt = realloc(prompt, len*sizeof(char));
        strcat(prompt, corr[i]);
        strcat(prompt, " | ");
      }
    }
    printf("%s\n", prompt);
    free(prompt);
  }
}

// Fonction exécutée par les threads (on passe l'index du worker en paramètre)
void * socket_thread(void* arg){
  int index = *((int *)arg);
  char buffer[BUFFER_SIZE];
  char* msg = NULL; int len = 0;
  int socket_running = TRUE;

  while(socket_running){
    // Recupere le msg client
    bzero(buffer, BUFFER_SIZE);
    recv(w_pool[index].socket, buffer, BUFFER_SIZE, 0);
    writeRequestLog(portno, buffer, corr[index]);
    int cmd = getcommand(buffer);
    switch (cmd) {
      case 0:
        socket_running = !socket_running;
        msg = malloc(sizeof(char)*4);
        strcpy(msg, "OK\n");
        break;

      case 1: ;// announce
        // Enregistrement du port
        char* tmp_port = malloc(sizeof(char) * 5);
        strncpy(tmp_port, buffer + 24, 5);
        if(tmp_port[4] == ' ') tmp_port[4]='\0';
        strcpy(corr[index], "localhost:");
        strcat(corr[index], tmp_port);
        char **files = parse_string(buffer, "[", "]", " ");
        if (files != NULL){
          for (int i = 0; files[i] != NULL ; i+=4){
            add_user(files[i+3], corr[index], files[i], files[i+1], files[i+2]);
          }
        }
        msg = malloc(sizeof(char)*4);
        strcpy(msg, "OK\n");
        break;

      case 2: ;// look (seul le critere d egalite sur les noms est implementé atm)
        char *name_criterion = parse_string(buffer, "[", "]", "\"")[1];
        struct my_struct* file;
        len = 9;
        msg = malloc(sizeof(char)*len);
        strcpy(msg, "list [");
        int not_empty = 0;
        USER_ITER(file){
          if (strcmp(file->file_name, name_criterion) == 0){
            if (not_empty){
              msg = realloc(msg, (++len)*sizeof(char));
              strcat(msg, " ");
            }
            not_empty = 1;
            len += strlen(file->file_name)+strlen(file->file_size)+strlen(file->pieces_size)+strlen(file->id)+3;
            msg = realloc(msg, len*sizeof(char));
            strcat(msg, file->file_name); strcat(msg, " ");
            strcat(msg, file->file_size); strcat(msg, " ");
            strcat(msg, file->pieces_size); strcat(msg, " ");
            strcat(msg, file->id);
          }
        }
        strcat(msg, "]\n");
        break;

      case 3: ;// getfile
        int start = str_istr(buffer, " ");
        char* key = str_sub(buffer, start+1, strlen(buffer));

        len = 11+strlen(key);
        msg = malloc(len*sizeof(char));
        strcpy(msg, "peers ");
        strcat(msg, key);
        strcat(msg, " [");
        char* peers = find_user(key)->access;
        len += strlen(peers);
        msg = realloc(msg, len*sizeof(char));
        strcat(msg, peers);
        strcat(msg, "]\n");
        break;

      case 7: ;// update
        msg = malloc(sizeof(char)*4);
        strcpy(msg, "OK\n");
        // seed
        char **keys = parse_string(buffer, "[", "]", " ");
        if(keys != NULL){
          for(int i = 0; keys[i] != NULL; i++){
            add_user(keys[i], corr[index], NULL, NULL, NULL);
          }
        }
        // leech
        char* leech_buffer = strchr(buffer, ']') + 2;
        keys = parse_string(leech_buffer, "[", "]", " ");
        if(keys != NULL){
          for(int i = 0; keys[i] != NULL; i++){
            add_user(keys[i], corr[index], NULL, NULL, NULL);
          }
        }
        break;

      default:
        printf("Unknown command %s\n",buffer);
        char* error = "Error command not recognized\n"; //idem, remplacer par un msg d'erreur
        msg = malloc(sizeof(char)*strlen(error));
        strcpy(msg, error);
        break;
    }
    send(w_pool[index].socket, msg, strlen(msg), 0);
    writeResponseLog(portno, msg, corr[index]);
    free(msg);
  }
  close(w_pool[index].socket);
  printf("Connexion fermée\n");
  w_pool[index].available = TRUE;
  strcpy(corr[index], "empty");
  pthread_exit(NULL);
}

int main(int argc, char *argv[]){
  int sockfd, newsockfd;
  socklen_t sock_size;
  struct sockaddr_in serv_addr, cli_addr; //Creation d'une structure d'adresse
  pthread_t prompt;
  initialize_workers(w_pool);

  int index_free_worker = -1; //-1 si 0 free thread, ou sinon compris entre 0 et POOL_SIZE

  if (argc < 2) {
    fprintf(stderr,"ERROR, no port provided\n");
    exit(1);
  }

  //Creation d'un nouveau socket
  sockfd = socket(AF_INET, SOCK_STREAM, 0);
  if (sockfd < 0) error("ERROR opening socket");
  if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &(int){1}, sizeof(int)) < 0)
    error("ERROR setsockopt(SO_REUSEADDR) failed");

  bzero((char *) &serv_addr, sizeof(serv_addr));
  portno = atoi(argv[1]);

  serv_addr.sin_family = AF_INET; //mise à jour de la famille
  serv_addr.sin_addr.s_addr = INADDR_ANY; //écoute n'importe quelle adresse dispo sur la machine
  serv_addr.sin_port = htons(portno); //déclaration du port

  if (bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0)
    error("ERROR on binding");

  listen(sockfd, 0); //Pas de liste d'attente atm
  sock_size = sizeof(cli_addr);

  if(pthread_create(&prompt, NULL, prompt_list_peers, NULL) != 0)
    error("ERROR on creating the thread printing connected peers");

  //acceptation de la connexion
  while ((newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &sock_size))){
    //le accept se lance lorsqu'un client se connecte
    if (newsockfd < 0){
      error("ERROR on accept");
    }else{
      printf("Connexion établie\n");
    }

    index_free_worker = -1;
    while (index_free_worker == -1){
      index_free_worker = first_free_worker(w_pool);
    } // Attendre qu'un thread soit disponible

    w_pool[index_free_worker].socket = newsockfd;
    w_pool[index_free_worker].available = FALSE;

    if(pthread_create(&w_pool[index_free_worker].thread, NULL, socket_thread, &index_free_worker) != 0)
      error("ERROR on creating new thread");

  }
  return 0;
}
