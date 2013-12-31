handle_get_set() {
    local nonscopedPrev=$1
    local projectdir=$2
    local prjContexts=$3
    local dflContexts=$4
    local has_compopt=$5
    local allprops=""

    for context in $prjContexts
    do
        if [ -e $projectdir/config/${context}.properties ]; then
            local prop=`less $projectdir/config/${context}.properties | sed 's/^#.*//' | grep -v '^$' \
                | sed "s/^\([^=]*\)=.*/\1/"`
            allprops=`echo "$allprops $prop"`
        fi
    done
    case "${nonscopedPrev}" in
    rm | get)
        ;;
    *)
        # need to get system props as well
        for context in $dflContexts
        do
            if [ -e $PLY_HOME/config/${context}.properties ]; then
                local prop=`less $PLY_HOME/config/${context}.properties | sed 's/^#.*//' | grep -v '^$' \
                    | sed "s/^\([^=]*\)=.*/\1/"`
                allprops=`echo "$allprops $prop"`
            fi
        done
    esac
    case "${nonscopedPrev}" in
    rm)
        COMPREPLY=( $(compgen -W "${allprops}" -- ${cur}) );;
    set)
        if [ "$has_compopt" == "builtin" ]; then
            compopt -o nospace
        fi
        COMPREPLY=( $(compgen -S '=' -W "${allprops}" -- ${cur}) );;
    *)
        COMPREPLY=( $(compgen -W "${allprops} from --unfiltered" -- ${cur}) );;
    esac
}
_ply_completion() {
    local has_compopt=`type -t compopt`
    local cur prev tasks defaultaliases projectaliases aliases configtasks projectdir defaultcontexts projectcontexts
    local deptasks="add rm exclude list tree"
    local repotasks="add rm auth auth-local"
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    projectdir="./"
    while [[ (-d ${projectdir}) && (${projectdir} != "/")]]; do
	local result=$(find ${projectdir} -maxdepth 1 -type d -name ".ply")
	if [[ ${result} == *.ply* ]]; then
	    projectdir="${result}"
	    break
	fi
	projectdir=`readlink "${projectdir}../"`
    done
    if [ ! -d ${projectdir} ]; then
	projectdir="./.ply" # default to current
    fi

    defaultaliases=""
    if [ -e $PLY_HOME/config/aliases.properties ]; then
	defaultaliases=$(less $PLY_HOME/config/aliases.properties | sed 's/^#.*//' | grep -v '^$' | sed 's/^\([^=]*\)=.*/\1/')
    fi
    projectaliases=""
    if [ -e ${projectdir}/config/aliases.properties ]; then
	    projectaliases=$(less ${projectdir}/config/aliases.properties | sed 's/^#.*//' | grep -v '^$' | sed 's/^\([^=]*\)=.*/\1/')
    fi
    aliases="${defaultaliases} ${projectaliases}"
    configtasks="get get-all set append prepend rm"
    tasks="init --version --usage --help update ${configtasks} ${aliases}"
    defaultcontexts=$(find $PLY_HOME/config/ -type f -name "*.properties" | \
    sed 's/\(\/.*\/\)\(.*\)\.properties/-P\2/' | sed 's/\./#/')
    if [ -d ${projectdir}/config/ ]; then
        projectcontexts=$(find ${projectdir}/config/ -type f -name "*.properties" | \
        sed 's/\(\/.*\/\)\(.*\)\.properties/-P\2/' | sed 's/\./#/')
    else
        projectcontexts=""
    fi
    local prjContexts=""
    for prjContext in $projectcontexts
    do
        if [[ ${prjContext:3} != *.* ]]; then
            prjContexts=`echo "$prjContexts ${prjContext:3}"`
        fi
    done
    local dflContexts=""
    for dflContext in $defaultcontexts
    do
        if [[ ${dflContext} != *#* ]]; then
            dflContexts=`echo "$dflContexts ${dflContext:2}"`
        fi
    done

    # if '-P' is start of cur, print the contexts after the -P
    if [[ ${cur} == -P* ]]; then
	if [ "$has_compopt" == "builtin" ]; then
	    compopt -o nospace
	fi
	# the start of the -P
	if [[ ${cur} != *.* ]]; then
	    COMPREPLY=( $(compgen -W "${defaultcontexts} ${projectcontexts}" -- ${cur}) )
	# the -P has a complete context, complete with the context's property-names
	elif [[ ${cur} == *.* ]]; then
	    local index=`echo "$cur" | sed -n 's/[\.].*//p' | wc -c`
	    local len=$(($index - 2))
	    local curcontext=${cur:0:$index}
	    local context=${cur:2:$len}
	    local nonscopedcontext=""
	    if [[ ${context} == *#* ]]; then
		index=`echo "$context" | sed -n 's/[#].*//p' | wc -c`
		len=$(($index - 1))
		nonscopedcontext=${context:0:$len}
	    fi
	    context=${context/\#/\.}
	    local defaultprops=""
	    if [ -e $PLY_HOME/config/${context}properties ]; then
		defaultprops=$(less $PLY_HOME/config/${context}properties | sed 's/^#.*//' | grep -v '^$' \
		    | sed "s/^\([^=]*\)=.*/$curcontext\1/")
	    fi
	    if [[ (${#nonscopedcontext} -gt 0) && (-e $PLY_HOME/config/${nonscopedcontext}.properties) ]]; then
		local nonscopeddefaultprops=$(less $PLY_HOME/config/${nonscopedcontext}.properties | sed 's/^#.*//' | grep -v '^$' \
		    | sed "s/^\([^=]*\)=.*/$curcontext\1/")
		defaultprops="${defaultprops} ${nonscopeddefaultprops}"
	    fi
	    local projectprops=""
	    if [ -e ${projectdir}/config/${context}properties ]; then
		projectprops=$(less ${projectdir}/config/${context}properties | sed 's/^#.*//' | grep -v '^$' \
		    | sed "s/^\([^=]*\)=.*/$curcontext\1/")
	    fi
	    if [[ (${#nonscopedcontext} -gt 0) && (-e ${projectdir}/config/${nonscopedcontext}.properties) ]]; then
		local nonscopedprojectprops=$(less ${projectdir}/config/${nonscopedcontext}.properties | sed 's/^#.*//' \
		    | grep -v '^$' | sed "s/^\([^=]*\)=.*/$curcontext\1/")
		projectprops="${projectprops} ${nonscopedprojectprops}"
	    fi
	    COMPREPLY=( $(compgen -S "=" -W "${defaultprops} ${projectprops}" -- ${cur}) )
	fi
	return 0;
    fi

    # pull out the scope, if any
    local nonscopedPrev=$prev
    local scope=""
    local prevIndex=1
    if [[ ${cur} == :* ]]; then
        scope=$prev
        if [[ COMP_CWORD > 2 ]]; then
            prevIndex=2
            nonscopedPrev="${COMP_WORDS[COMP_CWORD-2]}"
        else
            nonscopedPrev=""
        fi
    elif [[ ${prev} == : ]]; then
        if [[ COMP_CWORD > 3 ]]; then
            prevIndex=3
            scope="${COMP_WORDS[COMP_CWORD-2]}"
            nonscopedPrev="${COMP_WORDS[COMP_CWORD-3]}"
        elif [[ COMP_CWORD > 2 ]]; then
            scope="${COMP_WORDS[COMP_CWORD-2]}"
            nonscopedPrev=""
        else
            nonscopedPrev=""
        fi
    fi

    case "${nonscopedPrev}" in
	init)
	    if [ "$has_compopt" == "builtin" ]; then
		compopt -o nospace
	    fi
	    COMPREPLY=( $(compgen -S '=' -W "--from-pom" -- ${cur}) );;
	--version | --usage | --help)
	    ;;
	rm)
	    # rm could be a sub-task for the 'dep' / 'repo' aliases
	    if [[ ($nonscopedPrev == "rm") && ("${COMP_WORDS[COMP_CWORD-prevIndex-1]}" == "dep") ]]; then
	        local depFile="dependencies.properties"
            if [[ -n $scope ]]; then
                depFile=`echo "dependencies.$scope.properties"`
            fi
            if [ -e $projectdir/config/$depFile ]; then
                local prop=`less $projectdir/config/$depFile | sed 's/^#.*//' | grep -v '^$' \
                            | sed "s/^\([^=]*\)=.*/\1/"`
                COMPREPLY=( $(compgen -W "${prop}" -- ${cur}) )
            fi
        elif [[ ($nonscopedPrev == "rm") && ("${COMP_WORDS[COMP_CWORD-prevIndex-1]}" == "repo") ]]; then
            local repoFile="repositories.properties"
            if [[ -n $scope ]]; then
                repoFile=`echo "repositories.$scope.properties"`
            fi
            if [ -e $projectdir/config/$repoFile ]; then
                local prop=`less $projectdir/config/$repoFile | sed 's/^#.*//' | grep -v '^$' \
                            | sed "s/^\([^=]*\)=.*/\1/"`
                COMPREPLY=( $(compgen -W "${prop}" -- ${cur}) )
            fi
        else
            handle_get_set "$nonscopedPrev" "$projectdir" "$prjContexts" "$dflContexts" "$has_compopt"
        fi
        ;;
	get | get-all | set)
        handle_get_set "$nonscopedPrev" "$projectdir" "$prjContexts" "$dflContexts" "$has_compopt"
        ;;
	append | prepend)
	    ;;
	dep)
	    if [[ $cur == : ]]; then
	        COMPREPLY=( $(compgen -W "${aliases}" -- '') )
	    else
	        COMPREPLY=( $(compgen -W "${deptasks} ${aliases}" -- ${cur}) )
	    fi
	    ;;
	repo)
	    if [[ $cur == : ]]; then
	        COMPREPLY=( $(compgen -W "${aliases}" -- '') )
	    else
	        COMPREPLY=( $(compgen -W "${repotasks} ${aliases}" -- ${cur}) )
	    fi
	    ;;
	*)
	    # handle ply case
	    if [[ ($COMP_CWORD == 1) || (${configtasks} != *${COMP_WORDS[1]}*) ]]; then
	        if [[ $cur == : ]]; then
                COMPREPLY=( $(compgen -W "${aliases} ${configtasks}" -- '') )
            else
                COMPREPLY=( $(compgen -W "${tasks}" -- ${cur}) )
            fi
            return 0;
	    fi
	    # handle the next dep case
	    if [[ (${deptasks} == *${nonscopedPrev}*) && ("${COMP_WORDS[COMP_CWORD - prevIndex - 1]}" == "dep") ]]; then
	        case "${nonscopedPrev}" in
	        add)
	            # TODO - augment with values within localRepo/...
	            ;;
	        rm)
	            # handled by 'rm' case above (as maybe the 'rm' is a config-task)
	            ;;
	        esac
	    # handle the next repo case
	    elif [[ (${repotasks} == *${nonscopedPrev}*) && ("${COMP_WORDS[COMP_CWORD - prevIndex - 1]}" == "repo") ]]; then
            case "${nonscopedPrev}" in
            add)
                # TODO - augment with known remote repos (like maven central, etc)...
                ;;
            auth)
                # TODO - username
                ;;
            auth-local)
                # TODO - username
                ;;
            rm)
                # handled by 'rm' case above (as maybe the 'rm' is a config-task)
                ;;
            esac
	    # handle the next config-task (get/get-all/set/append/prepend/rm) case
	    elif [[ (${configtasks} == *${COMP_WORDS[COMP_CWORD - prevIndex - 1]}*) ]]; then
	        local configTask=${COMP_WORDS[COMP_CWORD - prevIndex - 1]}

	        case "${configTask}" in
	        get)
	            if [[ ${nonscopedPrev} == from ]]; then
	                COMPREPLY=( $(compgen -W "${prjContexts}" -- ${cur}) )
	            elif [[ ${nonscopedPrev} != --unfiltered ]]; then
	                COMPREPLY=( $(compgen -W "from --unfiltered" -- ${cur}) )
                fi
                ;;
            get-all)
                if [[ ${nonscopedPrev} == from ]]; then
	                COMPREPLY=( $(compgen -W "${prjContexts} ${dflContexts}" -- ${cur}) )
	            elif [[ ${nonscopedPrev} != --unfiltered ]]; then
	                COMPREPLY=( $(compgen -W "from --unfiltered" -- ${cur}) )
                fi
                ;;
            rm)
                COMPREPLY=( $(compgen -W "from" -- '') );;
            append | prepend)
                COMPREPLY=( $(compgen -W "to" -- '') );;
            esac
        # handle the tertiary case for config-tasks
        elif [[ (${configtasks} == *${COMP_WORDS[COMP_CWORD - prevIndex - 2]}*) ]]; then
            local configTask=${COMP_WORDS[COMP_CWORD - prevIndex - 2]}
	        case "${configTask}" in
	        get | get-all | rm)
	            if [[ ${nonscopedPrev} == from ]]; then
	                local propName=${COMP_WORDS[COMP_CWORD - prevIndex - 1]}
                    local contexts=${prjContexts}
                    if [[ ${configTask} == get-all ]]; then
                        contexts=`echo "$contexts $dflContexts"`
                    fi
                    local propContexts=""
                    for context in $contexts
                    do
                        if [ -e $projectdir/config/${context}.properties ]; then
                            local prop=`less $projectdir/config/${context}.properties | sed 's/^#.*//' | grep -v '^$' \
                                | sed "s/^\([^=]*\)=.*/\1/"`
                            if [[ $prop == *$propName* ]]; then
                                propContexts=`echo "$propContexts $context"`
                            fi
                        fi
                        if [ -e $PLY_HOME/config/${context}.properties ]; then
                            local prop=`less $PLY_HOME/config/${context}.properties | sed 's/^#.*//' | grep -v '^$' \
                                | sed "s/^\([^=]*\)=.*/\1/"`
                            if [[ $prop == *$propName* ]]; then
                                propContexts=`echo "$propContexts $context"`
                            fi
                        fi
                    done
	                COMPREPLY=( $(compgen -W "${propContexts}" -- ${cur}) )
                fi
                ;;
            append | prepend)
                if [[ ${nonscopedPrev} == to ]]; then
                    local allprops=""
                    for context in $prjContexts
                    do
                        if [ -e $projectdir/config/${context}.properties ]; then
                            local prop=`less $projectdir/config/${context}.properties | sed 's/^#.*//' | grep -v '^$' \
                                | sed "s/^\([^=]*\)=.*/\1/"`
                            allprops=`echo "$allprops $prop"`
                        fi
                    done
                    for context in $dflContexts
                    do
                        if [ -e $PLY_HOME/config/${context}.properties ]; then
                            local prop=`less $PLY_HOME/config/${context}.properties | sed 's/^#.*//' | grep -v '^$' \
                                | sed "s/^\([^=]*\)=.*/\1/"`
                            allprops=`echo "$allprops $prop"`
                        fi
                    done
                    COMPREPLY=( $(compgen -W "${allprops}" -- ${cur}) )
                fi
                ;;
            esac
        # handle the forth case for config-tasks
        elif [[ (${configtasks} == *${COMP_WORDS[COMP_CWORD - prevIndex - 3]}*) ]]; then
            local configTask=${COMP_WORDS[COMP_CWORD - prevIndex - 3]}
	        case "${configTask}" in
	        get | get-all)
                COMPREPLY=( $(compgen -W "--unfiltered" -- '') );;
            append | prepend | set)
                COMPREPLY=( $(compgen -W "in" -- '') );;
            esac
        # handle the fifth case for config-tasks
        elif [[ (${configtasks} == *${COMP_WORDS[COMP_CWORD - prevIndex - 4]}*) ]]; then
            local configTask=${COMP_WORDS[COMP_CWORD - prevIndex - 4]}
	        case "${configTask}" in
            append | prepend | set)
                local propName=${COMP_WORDS[COMP_CWORD - prevIndex - 1]}
                if [[ ${configTask} == set ]]; then
                    propName=${COMP_WORDS[COMP_CWORD - prevIndex - 3]}
                fi
                local contexts=`echo "$prjContexts $dflContexts"`
                local propContexts=""
                for context in $contexts
                do
                    if [ -e $projectdir/config/${context}.properties ]; then
                        local prop=`less $projectdir/config/${context}.properties | sed 's/^#.*//' | grep -v '^$' \
                            | sed "s/^\([^=]*\)=.*/\1/"`
                        if [[ $prop == *$propName* ]]; then
                            propContexts=`echo "$propContexts $context"`
                        fi
                    fi
                    if [ -e $PLY_HOME/config/${context}.properties ]; then
                        local prop=`less $PLY_HOME/config/${context}.properties | sed 's/^#.*//' | grep -v '^$' \
                            | sed "s/^\([^=]*\)=.*/\1/"`
                        if [[ $prop == *$propName* ]]; then
                            propContexts=`echo "$propContexts $context"`
                        fi
                    fi
                done
                COMPREPLY=( $(compgen -W "${propContexts}" -- ${cur}) )
            esac
        # handle the sixth case for config-tasks - which is nothing
        elif [[ (${configtasks} == *${COMP_WORDS[COMP_CWORD - prevIndex - 5]}*) ]]; then
            return 0;
        # handle the case just after scope addition 'test:'
	    elif [[ $cur == : ]]; then
	        COMPREPLY=( $(compgen -W "${aliases}" -- '') )
	    else
	        COMPREPLY=( $(compgen -W "${aliases}" -- ${cur}) )
	    fi
    esac
    	   
    return 0;
}
complete -F _ply_completion ply