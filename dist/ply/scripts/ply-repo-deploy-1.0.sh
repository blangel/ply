#!/bin/bash

localrepo=${ply_depmngr_localRepo/\~/$HOME}
artifactname=$ply_project_artifact_name
namespace=$ply_project_namespace
name=$ply_project_name
version=$ply_project_version
scope=$ply_ply_scope
packaging=$ply_project_packaging
artifactslabel=$ply_project_artifacts_label
artifactsourcesname="$name-$version-sources.$packaging"
gitrepo=$ply_deploy_git
gitpush=$ply_deploy_git_push
gitremotename=$ply_deploy_git_remote_name
gitrefspec=$ply_deploy_git_refspec

if [[ -z "$gitrepo" ]]; then
    echo "Local git repository not set: ply set git='your repo' in deploy"
    exit 1
fi 

if [[ ! -e "$gitrepo" || ! -d "$gitrepo" ]]; then
    echo "Local git repository is not a directory [ $gitrepo ]."
    exit 1
fi

mkdir -p $gitrepo/$namespace/$name/$version

cp $localrepo/$namespace/$name/$version/$artifactname $gitrepo/$namespace/$name/$version/

if [ $? != 0 ]; then
    echo "Could not copy artifact [ $localrepo/$namespace/$name/$version/$artifactname ] into local git repository [ $gitrepo/$namespace/$name/$version ]."
    exit 1
fi

# copy dependencies file
depfile="dependencies.properties"
if [[ ! -z "$artifactslabel" ]]; then
    depfile="dependencies.$artifactslabel.properties"
fi
cp $localrepo/$namespace/$name/$version/$depfile $gitrepo/$namespace/$name/$version/

# no need to check for success, dependencies file may not exist and that's ok

# copy checksum file
checksumfile="checksum.properties"
if [[ ! -z "$artifactslabel" ]]; then
    checksumfile="checksum.$artifactslabel.properties"
fi

checksumfilepath="$localrepo/$namespace/$name/$version/$checksumfile"
if [[ -e  "$checksumfilepath" ]]; then
    cp "$checksumfilepath" $gitrepo/$namespace/$name/$version/
fi

# copy artifacts-label file
artifactslabelfilepath="$localrepo/$namespace/$name/$version/$artifactname.$artifactslabel"
if [[ -e  "$artifactslabelfilepath" ]]; then
    cp "$artifactslabelfilepath" $gitrepo/$namespace/$name/$version/
fi

cp $localrepo/$namespace/$name/$version/$artifactsourcesname $gitrepo/$namespace/$name/$version/

# no need to check for success, source file may not exist and that's ok

cd $gitrepo
git add ./

if [ $? != 0 ]; then
    echo "Could not add files into local git repository [ $gitrepo ]."
    exit 1
fi

git commit -m "* deploying $artifactname"

if [ $? != 0 ]; then
    echo "Could not commit files into local git repository [ $gitrepo ]."
    exit 1
fi

if [[ -z "$gitpush" || "$gitpush" == "true" || "$gitpush" == "True" || "$gitpush" == "TRUE" ]]; then
    command=""
    if [[ -z "$gitremotename" && -z "$gitrefspec" ]]; then
        command="git push -u origin master"
        git push -u origin master
    elif [[ -z "$gitremotename" ]]; then
        command="git push -u origin $gitrefspec"
        git push -u origin $gitrefspec
    elif [[ -z "$gitrefspec" ]]; then
        command="git push -u $gitremotename master"
        git push -u $gitremotename master
    else
        command="git push -u $gitremotename $gitrefspec"
        git push -u $gitremotename $gitrefspec
    fi
    if [ $? != 0 ]; then
        echo "Could not push into remote repository [ $command ]."
        exit 1
    fi
fi