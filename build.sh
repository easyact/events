#!/bin/bash
set -e
set -o pipefail

instruction()
{
  echo "usage: ./build.sh deploy <env>"
  echo ""
  echo "env: eg. int, staging, prod, ..."
  echo ""
  echo "for example: ./deploy.sh int"
}

if [ $# -eq 0 ]; then
  instruction
  exit 1
elif [ "$1" = "int" ] && [ $# -eq 1 ]; then
  sbt cucumber
elif [ "$1" = "at" ] && [ $# -eq 1 ]; then
  sbt it
elif [ "$1" = "deploy" ] && [ $# -eq 2 ]; then
  STAGE=$2

  sbt assembly
#  npm install
#  'node_modules/.bin/sls' deploy -s $STAGE
  sls deploy -s $STAGE
else
  instruction
  exit 1
fi