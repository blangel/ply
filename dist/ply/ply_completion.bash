_ply_completion() {
    local cur prev tasks defaultaliases projectaliases aliases configtasks projectdir
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"

    projectdir="./"
    while [ -d ${projectdir} ]; do
	local result=$(find ${projectdir} -maxdepth 1 -type d -name ".ply")
	if [[ ${result} == *.ply* ]]; then
	    projectdir="${result}"
	    break
	fi
	projectdir="${projectdir}../"
    done
    if [ ! -d ${projectdir} ]; then
	projectdir="./.ply" # default to current
    fi

    defaultaliases=""
    if [ -e $PLY_HOME/config/aliases.properties ]; then
	defaultaliases=$(less $PLY_HOME/config/aliases.properties | sed 's/^#.*//' | grep -v '^$' | sed 's/\(.*\)=.*/\1/')
    fi
    projectaliases=""
    if [ -e ${projectdir}/config/aliases.properties ]; then
	projectaliases=$(less ${projectdir}/config/aliases.properties | sed 's/^#.*//' | grep -v '^$' | sed 's/\(.*\)=.*/\1/')
    fi
    aliases="${defaultaliases} ${projectaliases}"
    tasks="init config ${aliases}"
    configtasks="get get-all set append prepend remove"
    
    case "${prev}" in 
	init)
	    COMPREPLY=();;
	config)
	    local defaultcontexts=$(find $PLY_HOME/config/ -type f -name "*.properties" -printf "%f\n" | \
		sed 's/\(.*\)\.properties/--\1/')
	    local projectcontexts=$(find ${projectdir}/config/ -type f -name "*.properties" -printf "%f\n" | \
		sed 's/\(.*\)\.properties/--\1/')
	    COMPREPLY=( $(compgen -W "${configtasks} ${defaultcontexts} ${projectcontexts}" -- ${cur}) );;
	dep)
	    local deptasks="add remove repo-add repo-remove list tree"
	    COMPREPLY=( $(compgen -W "${deptasks}" -- ${cur}) );;
	ply)
	    COMPREPLY=( $(compgen -W "${tasks}" -- ${cur}) );;
	*)
	    # if '-P' is start of cur, print the contexts after the -P
	    if [[ ${cur} == -P* ]]; then
	        # the start of the -P
		if [[ ${cur} != *.* ]]; then
		    local defaultcontexts=$(find $PLY_HOME/config/ -type f -name "*.properties" -printf "%f\n" | \
			sed 's/\(.*\)\.properties/-P\1/' | sed 's/\./#/')
		    local projectcontexts=$(find ${projectdir}/config/ -type f -name "*.properties" -printf "%f\n" | \
			sed 's/\(.*\)\.properties/-P\1/' | sed 's/\./#/')
		    COMPREPLY=( $(compgen -W "${defaultcontexts} ${projectcontexts}" -- ${cur}) )
		# the -P has a complete context, complete with the context's property-names
		elif [[ ${cur} == *.* ]]; then
		    local index=`expr index "$cur" "."`
		    local len=$(($index - 2))
		    local curcontext=${cur:0:$index}
		    local context=${cur:2:$len}
		    local nonscopedcontext=""
		    if [[ ${context} == *#* ]]; then
			index=`expr index "$context" "#"`
			len=$(($index - 1))
			nonscopedcontext=${context:0:$len}
		    fi
		    context=${context/\#/\.}
		    local defaultprops=""
		    if [ -e $PLY_HOME/config/${context}properties ]; then
			defaultprops=$(less $PLY_HOME/config/${context}properties | sed 's/^#.*//' | grep -v '^$' \
			    | sed "s/\(.*\)=.*/$curcontext\1/")
		    fi
		    if [[ (${#nonscopedcontext} -gt 0) && (-e $PLY_HOME/config/${nonscopedcontext}.properties) ]]; then
			local nonscopeddefaultprops=$(less $PLY_HOME/config/${nonscopedcontext}.properties | sed 's/^#.*//' | grep -v '^$' \
			    | sed "s/\(.*\)=.*/$curcontext\1/")
			defaultprops="${defaultprops} ${nonscopeddefaultprops}"
		    fi
		    local projectprops=""
		    if [ -e ${projectdir}/config/${context}properties ]; then
			projectprops=$(less ${projectdir}/config/${context}properties | sed 's/^#.*//' | grep -v '^$' \
			    | sed "s/\(.*\)=.*/$curcontext\1/")
		    fi
		    if [[ (${#nonscopedcontext} -gt 0) && (-e ${projectdir}/config/${nonscopedcontext}.properties) ]]; then
			local nonscopedprojectprops=$(less ${projectdir}/config/${nonscopedcontext}.properties | sed 's/^#.*//' \
			    | grep -v '^$' | sed "s/\(.*\)=.*/$curcontext\1/")
			projectprops="${projectprops} ${nonscopedprojectprops}"
		    fi
		    COMPREPLY=( $(compgen -W "${defaultprops} ${projectprops}" -- ${cur}) )
		fi
	    # if aliases was previous, print again as aliases can be duplicated and chained
	    elif [[ ${aliases} == *${prev}* ]]; then
		COMPREPLY=( $(compgen -W "${aliases}" -- ${cur}) )
	    # if config was the first (after ply) and config is not the previous, check for aliases/config-tasks/property-names
	    elif [ "${COMP_WORDS[1]}" == "config" ]; then 
		# if config-tasks was not prev and we have not already printed the config-tasks before, print them
		if [[ (${configtasks} != *${prev}*) && (${COMP_CWORD} -lt 5) ]]; then
		    COMPREPLY=( $(compgen -W "${configtasks}" -- ${cur}) )
		# if config-tasks was prev and there is a context specified (i.e., '--xxxxx') print the context's property names
	        elif [[ (${configtasks} == *${prev}*) && (${COMP_WORDS[2]} == --*) ]]; then
		    local nonscopedcontext=""
		    if [[ ${COMP_WORDS[2]} == *.* ]]; then
			local index=`expr index "${COMP_WORDS[2]}" "."`
			local len=$(($index - 1))
			nonscopedcontext=${COMP_WORDS[2]:0:$len}
		    fi
		    local defaultprops=""
		    if [ -e $PLY_HOME/config/${COMP_WORDS[2]:2}.properties ]; then
			defaultprops=$(less $PLY_HOME/config/${COMP_WORDS[2]:2}.properties | sed 's/^#.*//' | grep -v '^$' \
			    | sed 's/\(.*\)=.*/\1/')
		    fi		    
		    local nonscopeddefaultprops=""
		    if [[ (${#nonscopedcontext} -gt 0) && (-e $PLY_HOME/config/${nonscopedcontext:2}.properties) ]]; then
			nonscopeddefaultprops=$(less $PLY_HOME/config/${nonscopedcontext:2}.properties | sed 's/^#.*//' | grep -v '^$' \
			    | sed 's/\(.*\)=.*/\1/')			
		    fi
		    local projectprops=""
		    if [ -e ${projectdir}/config/${COMP_WORDS[2]:2}.properties ]; then
			projectprops=$(less ${projectdir}/config/${COMP_WORDS[2]:2}.properties | sed 's/^#.*//' | grep -v '^$' \
			    | sed 's/\(.*\)=.*/\1/')
		    fi
		    local nonscopedprojectprops=""
		    if [[ (${#nonscopedcontext} -gt 0) && (-e ${projectdir}/config/${nonscopedcontext:2}.properties) ]]; then
			nonscopedprojectprops=$(less ${projectdir}/config/${nonscopedcontext:2}.properties | sed 's/^#.*//' | grep -v '^$' \
			    | sed 's/\(.*\)=.*/\1/')			
		    fi
		    # if get/remove only print the projectprops
		    # for set/append/prepend need to also print non-scoped if in scoped context
		    case "${prev}" in
			get | remove)
			    COMPREPLY=( $(compgen -W "${projectprops}" -- ${cur}) );;
			get-all)
			    COMPREPLY=( $(compgen -W "${defaultprops} ${projectprops}" -- ${cur}) );;
			set | append | prepend)
			    COMPREPLY=( $(compgen -W "${defaultprops} ${nonscopeddefaultprops} ${projectprops} ${nonscopedprojectprops}" -- ${cur}) );;
		    esac
		fi
	    fi
	    ;;
    esac
    	   
    return 0;
}
complete -F _ply_completion ply