#!/bin/sh

if [[ -z "$3" ]]; then
    echo "Local git repository not set: ply set git='your repo' in deploy"
    exit 1
fi 

cp $1/$2 $3

if [ $? != 0 ]; then
    echo "Could not copy artifact [ $1/$2 ] into local git repository [ $3 ]."
    exit 1
fi

cd $3
git add ./

if [ $? != 0 ]; then
    echo "Could not add files into local git repository [ $3 ]."
    exit 1
fi

git commit -m "* deploying $2"

if [ $? != 0 ]; then
    echo "Could not commit files into local git repository [ $3 ]."
    exit 1
fi

if [[ -z "$4" || "$4" == "true" || "$4" == "True" || "$4" == "TRUE" ]]; then
    command=""
    if [[ -z "$5" && -z "$6" ]]; then
        command="git push -u origin master"
        git push -u origin master
    elif [[ -z "$5" ]]; then
        command="git push -u origin $6"
        git push -u origin $6
    elif [[ -z "$6" ]]; then
        command="git push -u $5 master"
        git push -u $5 master
    else
        command="git push -u $5 $6"
        git push -u $5 $6
    fi
    if [ $? != 0 ]; then
        echo "Could not push into remote repository [ $command ]."
        exit 1
    fi
fi