#!/bin/bash

BASE_DIR=`cd $(dirname $0)/..; pwd`
PROFILE="dev"
export SERVER="nacos-server"
export DEFAULT_SEARCH_LOCATIONS="classpath:/,classpath:/config/,file:./,file:./config/"
export CUSTOM_SEARCH_LOCATIONS=${DEFAULT_SEARCH_LOCATIONS},file:${BASE_DIR}/conf/

MAX_MEMORY=
AUTH=
AUTH_TOKEN=
ACCESS_KEY=
SECRET=
DB_URL=
DB_USER=
DB_PASSWORD=
params=$(getopt -o "P:a:t:k:s" -l "max-memory:,profile:,auth:,token:,access-key:,secret:,db-url:,db-user:,db-password:" -- "$@")
echo $0 $params
eval set -- "$params"
while true; do
    case "$1" in
        --max-memory)
            MAX_MEMORY=$2
            shift 2
            ;;
        -a | --auth)
            AUTH=$2
            shift 2
            ;;
        -P | --profile)
            PROFILE=$2
            shift 2
            ;;
        -t | --token)
            AUTH_TOKEN=$2
            shift 2
            ;;
        -k | --access-key)
            ACCESS_KEY=$2
            shift 2
            ;;
        -s | --secret)
            SECRET=$2
            shift 2
            ;;
        --db-url)
            DB_URL=$2
            shift 2
            ;;
        --db-user)
            DB_USER=$2
            shift 2
            ;;
        --db-password)
            DB_PASSWORD=$2
            shift 2
            ;;
        --)
            break
            ;;
    esac
done

JAVA_OPT="-Xms128m"
if [[ $MAX_MEMORY != "" ]]; then
    JAVA_OPT="$JAVA_OPT -Xmx$MAX_MEMORY"
else
    JAVA_OPT="$JAVA_OPT -Xmx384m"
fi
JAVA_OPT="$JAVA_OPT -XX:-OmitStackTraceInFastThrow -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${BASE_DIR}/logs/java_heapdump.hprof"
JAVA_OPT="$JAVA_OPT -Dnacos.home=${BASE_DIR}"
if [[ $PROFILE != "" ]]; then
    JAVA_OPT="$JAVA_OPT -Dspring.profiles.active=$PROFILE"
fi

if [[ $AUTH != "" ]]; then
    JAVA_OPT="$JAVA_OPT -Dnacos.core.auth.enabled=$AUTH"
fi
if [[ $AUTH_TOKEN != "" ]]; then
    JAVA_OPT="$JAVA_OPT -Dnacos.core.auth.default.token.secret.key=$AUTH_TOKEN"
fi
if [[ $ACCESS_KEY != "" ]]; then
    JAVA_OPT="$JAVA_OPT -Dnacos.core.auth.server.identity.key=$ACCESS_KEY"
fi
if [[ $SECRET != "" ]]; then
    JAVA_OPT="$JAVA_OPT -Dnacos.core.auth.server.identity.value=$SECRET"
fi
if [[ $DB_URL != "" ]]; then
    JAVA_OPT="$JAVA_OPT -Ddb.url.0=$DB_URL"
fi
if [[ $DB_USER != "" ]]; then
    JAVA_OPT="$JAVA_OPT -Ddb.user.0=$DB_USER"
fi
if [[ $DB_PASSWORD != "" ]]; then
    JAVA_OPT="$JAVA_OPT -Ddb.password.0=$DB_PASSWORD"
fi

JAVA_OPT="$JAVA_OPT -jar ${BASE_DIR}/lib/$SERVER.jar"
JAVA_OPT="$JAVA_OPT --spring.config.location=${CUSTOM_SEARCH_LOCATIONS}"
JAVA_OPT="$JAVA_OPT --logging.config=${BASE_DIR}/conf/nacos-logback.xml"
JAVA_OPT="$JAVA_OPT --server.max-http-header-size=524288"

echo "nohup java ${JAVA_OPT} >> /dev/null 2>&1 &"

java ${JAVA_OPT}
