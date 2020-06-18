#!/bin/bash

# while-menu-dialog: a menu driven system information program

DIALOG_CANCEL=1
DIALOG_ESC=255
HEIGHT=0
WIDTH=0

display_result() {
  ./dialog --title "$1" \
    --no-collapse \
    --form "\nDialog Sample Label and Values" 25 60 16 \
    "Form Label 1:" 1 1 "Value 1" 1 25 25 30 > /tmp/inputa.$$ \
    "Form Label 2:" 2 1 "Value 2" 2 25 25 30 > /tmp/inputb.$$ \
    "Form Label 3:" 3 1 "Value 3" 3 25 25 30 > /tmp/inputc.$$ \
    "Form Label 4:" 4 1 "Value 4" 4 25 25 30 > /tmp/inputd.$$ \
    2>&1 >/dev/tty
}

ask_tracker() {
  RESTART="0"
  NB=`cat Config/config_tracker.ini | wc -l`
  TRACKER_IP=`(ls Config/config_tracker.ini 2> /dev/null > /dev/null && grep "tracker-hostname = " Config/config_tracker.ini | sed 's/tracker-hostname = //') || echo "localhost"`
  TRACKER_PORT=`(ls Config/config_tracker.ini 2> /dev/null > /dev/null && grep "tracker-port = " Config/config_tracker.ini  | sed 's/tracker-port = //') || echo 9000`
  TRACKER_UPDATE=`(ls Config/config_tracker.ini 2> /dev/null > /dev/null && grep "tracker-update = " Config/config_tracker.ini  | sed 's/tracker-update = //') || echo 50`
  if [ -z $TRACKER_IP ]
  then
    TRACKER_IP="hostname"
  fi
  if [ -z $TRACKER_PORT ]
  then
    TRACKER_PORT="9000"
  fi
  if [ -z $TRACKER_UPDATE ]
  then
    TRACKER_UPDATE="50"
  fi
  ./dialog --title "Tracker's configuration" \
    --no-collapse \
    --form "" 25 60 16 \
    "tracker-hostname:" 1 1 $TRACKER_IP 1 25 25 30 \
    "Tracker-port:" 2 1 $TRACKER_PORT 2 25 25 30 \
    "Tracker-update:" 3 1 $TRACKER_UPDATE 3 25 25 30 > "/tmp/inputbox.tmp.tracker" \
    2>&1 >/dev/tty
  TMP=`head -1 /tmp/inputbox.tmp.tracker`
  if [ ! -z $TMP ]
  then
    echo "tracker-hostname = " `head -1 /tmp/inputbox.tmp.tracker` > "Config/config_tracker.ini"
  else
    echo "tracker-hostname = $TRACKER_IP"  > "Config/config_tracker.ini"
  fi
  TMP=`head -2 /tmp/inputbox.tmp.tracker | tail -1`
  if [ -z "${TMP//[0-9]}" ] && [ ! -z $TMP ] && [ $((TMP)) -gt 1024 ] && [ 65536 -gt $((TMP)) ]
  then
    echo "tracker-port = " `head -2 /tmp/inputbox.tmp.tracker | tail -1` >> "Config/config_tracker.ini"
  else
    echo "tracker-port = $TRACKER_PORT" >> "Config/config_tracker.ini"
    RESTART="1"
  fi
  TMP=`head -3 /tmp/inputbox.tmp.tracker | tail -1`
  if [ -z "${TMP//[0-9]}" ] && [ ! -z $TMP ]
  then
    echo "tracker-update = " `head -3 /tmp/inputbox.tmp.tracker | tail -1` >> "Config/config_tracker.ini"
  else
    echo "tracker-update = $TRACKER_UPDATE" >> "Config/config_tracker.ini"
    RESTART="1"
  fi
  if [ -z `cat /tmp/inputbox.tmp.tracker` ]
  then
    RESTART="0"
  fi
  test $RESTART == "1" && ask_tracker
}

ask_new_peer() {
  RESTART="0"
  COPY="0"
  PEER_PORT=`(ls /tmp/new_peer_error 2> /dev/null > /dev/null && grep "peer-port = " /tmp/new_peer_error | sed 's/peer-port = //') || echo 7000`
  PEER_MAX_CONNEXION=`(ls /tmp/new_peer_error 2> /dev/null > /dev/null && grep "peer-max-connexion = " /tmp/new_peer_error | sed 's/peer-max-connexion = //') || echo 10`
  PEER_MESSAGE_MAX_SIZE=`(ls /tmp/new_peer_error 2> /dev/null > /dev/null && grep "peer-message-max-size = " /tmp/new_peer_error | sed 's/peer-message-max-size = //') || echo 8192`
  PEER_UPDATE=`(ls /tmp/new_peer_error 2> /dev/null > /dev/null && grep "peer-update = " /tmp/new_peer_error | sed 's/peer-update = //') || echo 50`
  TRACKER_IP=`(ls /tmp/new_peer_error 2> /dev/null > /dev/null && grep "tracker-hostname = " /tmp/new_peer_error | sed 's/tracker-hostname = //') || echo localhost`
  TRACKER_PORT=`(ls /tmp/new_peer_error 2> /dev/null > /dev/null && grep "tracker-port = " /tmp/new_peer_error | sed 's/tracker-port =  //') || echo 8080`
  PIECE_SIZE=`(ls /tmp/new_peer_error 2> /dev/null > /dev/null && grep "piece-size = " /tmp/new_peer_error | sed 's/piece-size =  //') || echo 2048`
  FILE="write_config.sh"
  echo "0" > /tmp/inputbox.tmp.peer
  if [ -z $PEER_MAX_CONNEXION ]
  then
    PEER_MAX_CONNEXION="10"
  fi
  if [ -z $PEER_MESSAGE_MAX_SIZE ]
  then
    PEER_MESSAGE_MAX_SIZE="8192"
  fi
  if [ -z $PEER_UPDATE ]
  then
    PEER_UPDATE="50"
  fi
  if [ -z $TRACKER_IP ]
  then
    TRACKER_IP="localhost"
  fi
  if [ -z $TRACKER_PORT ]
  then
    TRACKER_PORT="8080"
  fi
  if [ -z $PIECE_SIZE ]
  then
    PIECE_SIZE="2048"
  fi
  ./dialog --title "New peer's configuration" \
    --no-collapse \
    --form "" 25 60 16 \
    "Peer-port:" 1 1 $PEER_PORT 1 25 25 30 \
    "Peer-max-connexion:" 2 1 $PEER_MAX_CONNEXION 2 25 25 30 \
    "Peer-message-max-size:" 3 1 $PEER_MESSAGE_MAX_SIZE 3 25 25 30 \
    "Peer-update:" 4 1 $PEER_UPDATE 4 25 25 30 \
    "Tracker-hostname:" 5 1 $TRACKER_IP 5 25 25 30 \
    "Tracker-port:" 6 1 $TRACKER_PORT 6 25 25 30 \
    "Piece-size:" 7 1 $PIECE_SIZE 7 25 25 30 > "/tmp/inputbox.tmp.peer" \
    2>&1 >/dev/tty
  TMP=`head -1 /tmp/inputbox.tmp.peer | tail -1`
  if [ -n "${TMP//[0-9]}" ] || [ `cat /tmp/inputbox.tmp.peer | head -1` -gt 0 ]
  then
    TMP=`head -1 /tmp/inputbox.tmp.peer | tail -1`
    if [ -z "${TMP//[0-9]}" ] && [ -n $TMP ] && [ $((TMP)) -gt 1024 ] && [ 11024 -gt $((TMP)) ]
    then
      TMP=`head -1 /tmp/inputbox.tmp.peer | tail -1`
      FILE="Config/config_peer$TMP.ini"
      if [ -e $FILE ]
      then
        RESTART="1"
        ./dialog --title "Error" \
          --msgbox "You can't use this port." 10 30
        FILE="/tmp/new_peer_error"
        echo "peer-port = 7000" > $FILE
      else
        echo "peer-port = $TMP" > $FILE
      fi
    else
      RESTART="1"
      FILE="/tmp/new_peer_error"
      echo "peer-port = $PEER_PORT" > $FILE
    fi
    TMP=`head -2 /tmp/inputbox.tmp.peer | tail -1`
    if [ -z "${TMP//[0-9]}" ] && [ -n $TMP ]
    then
      echo "peer-max-connexion = $TMP" >> $FILE
    else
      RESTART="1"
      COPY="1"
      echo "peer-max-connexion = $PEER_MAX_CONNEXION" >> $FILE
    fi
    TMP=`head -3 /tmp/inputbox.tmp.peer | tail -1`
    if [ -z "${TMP//[0-9]}" ] && [ -n $TMP ] && [ 8193 -gt $((TMP)) ]
    then
      echo "peer-message-max-size = $TMP" >> $FILE
    else
      COPY="1"
      echo "peer-message-max-size = $PEER_MESSAGE_MAX_SIZE" >> $FILE
    fi
    TMP=`head -4 /tmp/inputbox.tmp.peer | tail -1`
    if [ -z "${TMP//[0-9]}" ] && [ -n $TMP ]
    then
      echo "peer-update = $TMP" >> $FILE
    else
      RESTART="1"
      COPY="1"
      echo "peer-update = $PEER_UPDATE" >> $FILE
    fi
    TMP=`head -5 /tmp/inputbox.tmp.peer | tail -1`
    if [ ! -z $TMP ]
    then
      echo "tracker-hostname = " `head -5 /tmp/inputbox.tmp.peer | tail -1` >> $FILE
    else
      echo "tracker-hostname = $TRACKER_IP"  >> $FILE
      RESTART="1"
    fi
    TMP=`head -6 /tmp/inputbox.tmp.peer | tail -1`
    if [ -z "${TMP//[0-9]}" ] && [ ! -z $TMP ] && [ $((TMP)) -gt 1024 ] && [ 11024 -gt $((TMP)) ]
    then
      echo "tracker-port = " `head -6 /tmp/inputbox.tmp.peer | tail -1` >> $FILE
    else
      echo "tracker-port = $TRACKER_PORT" >> $FILE
      RESTART="1"
    fi
    TMP=`head -7 /tmp/inputbox.tmp.peer | tail -1`
    if [ -z "${TMP//[0-9]}" ] && [ ! -z $TMP ]
    then
      echo "piece-size = " `head -7 /tmp/inputbox.tmp.peer | tail -1` >> $FILE
    else
      echo "piece-size = $PIECE_SIZE" >> $FILE
      RESTART="1"
    fi
    if [ $COPY == "1" ] && [ $FILE != "/tmp/new_peer_error" ]
    then
      cp $FILE /tmp/new_peer_error
    fi
    test $RESTART == "1" && ask_new_peer
    rm -f /tmp/new_peer_error
  fi
}

ask_change_peer() {
  RESTART="0"
  NB=`ls -1 Config | grep "config_peer*" | sed 's/config_peer//' | sed 's/.ini//' | head -$1 | tail -1`
  FILE="Config/config_peer$NB.ini"
  PEER_MAX_CONNEXION=`(ls $FILE 2> /dev/null > /dev/null && grep "peer-max-connexion = " $FILE | sed 's/peer-max-connexion = //') || echo 10`
  PEER_MESSAGE_MAX_SIZE=`(ls $FILE 2> /dev/null > /dev/null && grep "peer-message-max-size = " $FILE | sed 's/peer-message-max-size = //') || echo 8192`
  PEER_UPDATE=`(ls $FILE 2> /dev/null > /dev/null && grep "peer-update = " $FILE | sed 's/peer-update = //') || echo 50`
  TRACKER_IP=`(ls $FILE 2> /dev/null > /dev/null && grep "tracker-hostname = " $FILE | sed 's/tracker-hostname = //') || echo localhost`
  TRACKER_PORT=`(ls $FILE 2> /dev/null > /dev/null && grep "tracker-port = " $FILE | sed 's/tracker-port =  //') || echo 8080`
  PIECE_SIZE=`(ls $FILE 2> /dev/null > /dev/null && grep "piece-size = " $FILE | sed 's/piece-size =  //') || echo 2048`
  echo "0" > /tmp/inputbox.tmp.peer
  if [ -z $PEER_MAX_CONNEXION ]
  then
    PEER_MAX_CONNEXION="10"
  fi
  if [ -z $PEER_MESSAGE_MAX_SIZE ]
  then
    PEER_MESSAGE_MAX_SIZE="8192"
  fi
  if [ -z $PEER_UPDATE ]
  then
    PEER_UPDATE="50"
  fi
  if [ -z $TRACKER_IP ]
  then
    TRACKER_IP="localhost"
  fi
  if [ -z $TRACKER_PORT ]
  then
    TRACKER_PORT="8080"
  fi
  if [ -z $PIECE_SIZE ]
  then
    PIECE_SIZE="2048"
  fi
  ./dialog --title "Configuration of peer $NB" \
    --no-collapse \
    --form "" 25 60 16 \
    "Peer-max-connexion:" 1 1 $PEER_MAX_CONNEXION 1 25 25 30 \
    "Peer-message-max-size:" 2 1 $PEER_MESSAGE_MAX_SIZE 2 25 25 30 \
    "Peer-update:" 3 1 $PEER_UPDATE 3 25 25 30 \
    "Tracker-hostname:" 4 1 $TRACKER_IP 4 25 25 30 \
    "Tracker-port:" 5 1 $TRACKER_PORT 5 25 25 30 \
    "Piece-size:" 6 1 $PIECE_SIZE 6 25 25 30 > "/tmp/inputbox.tmp.peer" \
    2>&1 >/dev/tty
  echo "peer-port = $NB" > $FILE
  TMP=`head -1 /tmp/inputbox.tmp.peer | tail -1`
  if [ -n "${TMP//[0-9]}" ] || [ `cat /tmp/inputbox.tmp.peer | head -1` -gt 0 ]
  then
    TMP=`head -1 /tmp/inputbox.tmp.peer | tail -1`
    if [ -z "${TMP//[0-9]}" ] && [ -n $TMP ]
    then
      TMP=`head -1 /tmp/inputbox.tmp.peer | tail -1`
      echo "peer-max_connexion = $TMP" >> $FILE
    else
      RESTART="1"
      echo "peer-max_connexion = $PEER_MESSAGE_MAX_SIZE" >> $FILE
    fi
    TMP=`head -2 /tmp/inputbox.tmp.peer | tail -1`
    if [ -z "${TMP//[0-9]}" ] && [ -n $TMP ] && [ 8193 -gt $((TMP)) ]
    then
      echo "peer-message-max-size = $TMP" >> $FILE
    else
      RESTART="1"
      echo "peer-message-max-size = $PEER_MESSAGE_MAX_SIZE" >> $FILE
    fi
    TMP=`head -3 /tmp/inputbox.tmp.peer | tail -1`
    if [ -z "${TMP//[0-9]}" ] && [ -n $TMP ]
    then
      echo "peer-update = $TMP" >> $FILE
    else
      RESTART="1"
      echo "peer-update = $PEER_UPDATE" >> $FILE
    fi
    TMP=`head -4 /tmp/inputbox.tmp.peer | tail -1`
    if [ ! -z $TMP ]
    then
      echo "tracker-hostname = " `head -4 /tmp/inputbox.tmp.peer | tail -1` >> $FILE
    else
      echo "tracker-hostname = $TRACKER_IP"  >> $FILE
      RESTART="1"
    fi
    TMP=`head -5 /tmp/inputbox.tmp.peer | tail -1`
    if [ -z "${TMP//[0-9]}" ] && [ ! -z $TMP ] && [ $((TMP)) -gt 1024 ] && [ 65536 -gt $((TMP)) ]
    then
      echo "tracker-port = " `head -5 /tmp/inputbox.tmp.peer | tail -1` >> $FILE
    else
      echo "tracker-port = $TRACKER_PORT" >> $FILE
      RESTART="1"
    fi
    TMP=`head -6 /tmp/inputbox.tmp.peer | tail -1`
    if [ -z "${TMP//[0-9]}" ] && [ ! -z $TMP ]
    then
      echo "piece-size = " `head -6 /tmp/inputbox.tmp.peer | tail -1` >> $FILE
    else
      echo "piece-size = $PIECE_SIZE" >> $FILE
      RESTART="1"
    fi
    test $RESTART == "1" && ask_change_peer $1
  fi
}

while true; do
  exec 3>&1
  selection=$(./dialog \
    --backtitle "Configuration" \
    --title "Menu" \
    --clear \
    --cancel-label "Exit" \
    --menu "Please select:" $HEIGHT $WIDTH 4 \
    "1" "Tracker's configuration" \
    "2" "Add new peer's configuration" \
    "3" "Update peer's configuration" \
    "4" "Clean config" \
    2>&1 1>&3)
  exit_status=$?
  exec 3>&-
  case $exit_status in
    $DIALOG_CANCEL)
      clear
      echo "Program terminated."
      exit
      ;;
    $DIALOG_ESC)
      clear
      echo "Program aborted." >&2
      exit 1
      ;;
  esac
  case $selection in
    0 )
      clear
      echo "Program terminated."
      ;;
    1 )
      ask_tracker
      ;;
    2 )
      ask_new_peer
      ;;
    3 )
      let i=0
      W=()
      while read -r line; do
          let i=$i+1
          W+=($i "$line")
      done < <( ls -1 Config | grep "config_peer" | sed 's/config_peer//' | sed 's/.ini//' )
      N=$(./dialog --title "List file of directory /home" --menu "Chose one" 24 80 17 "${W[@]}" 3>&2 2>&1 1>&3)
      ask_change_peer $N
      ;;
    4 )
      rm -f Config/config_*.ini
      ;;
  esac
done
