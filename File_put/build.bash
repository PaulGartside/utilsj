#!/bin/bash

#set -o xtrace
#set -o noexec

NAME=File_put

CLASS_DIR=classes

FILES='File_put'

# Running without arguments will make without cleaning
clean=false
make=true

function usage
{
  echo "usage: $0 [clean] [make]"

  clean=false
  make=false
}

function run_cmd
{
  echo "$*"
  $*
  es=$?
}

function do_clean
{
  run_cmd \rm -rf $CLASS_DIR
  run_cmd \rm -rf *.jar
}

function do_make
{
  if [ ! -d $CLASS_DIR ]; then
    run_cmd mkdir -p $CLASS_DIR
    if [ $es -ne 0 ]; then
      return
    fi
  fi

  recompiled_file=false

  for file in $FILES; do
    if [ ! -e $CLASS_DIR/$file.class ] || [ $file.java -nt $CLASS_DIR/$file.class ]
    then
      run_cmd javac -cp . -d $CLASS_DIR $file.java
      if [ $es -ne 0 ]; then
        return
      fi
      recompiled_file=true
    fi
  done

  if [ ! -e $NAME.jar ] || [ $recompiled_file = true ]; then
    run_cmd jar -cvfe $NAME.jar $NAME -C $CLASS_DIR .
    if [ $es -ne 0 ]; then
      return
    fi
  fi

  echo "Done making $NAME.jar"
}

if [ $# -gt 2 ]; then
  # Too many arguments
  usage
elif [ $# -eq 1 ]; then
  # Checkout for clean or make
  if [ $1 = clean ]; then
    clean=true
    make=false
  elif [ $1 = make ]; then
    clean=false
    make=true
  else
    usage
  fi
elif [ $# -eq 2 ]; then
  if [ $1 = clean ] && [ $2 = make ]; then
    clean=true
    make=true
  elif [ $1 = make ] && [ $2 = clean ]; then
    clean=true
    make=true
  else
    usage
  fi
fi

if [ $clean = true ]; then
  do_clean
fi

if [ $make = true ]; then
  do_make
fi

