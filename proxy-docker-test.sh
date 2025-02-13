#!/usr/bin/env bash

if [[ "$1" == "" ]]; then
  docker run -v "${PWD}:/project" -w /project eclipse-temurin:17-focal /bin/sh -c "cd /project && /project/proxy-docker-test.sh test"
  docker run -v "${PWD}:/project" -w /project eclipse-temurin:17-focal /bin/sh -c "cd /project && /project/proxy-docker-test.sh test proxy"
  docker run -v "${PWD}:/project" -w /project eclipse-temurin:17-focal /bin/sh -c "cd /project && /project/proxy-docker-test.sh test noproxy"
  exit
fi

if [[ "$1" != "test" ]]; then
    echo "Start me with ./proxy-docker-test.sh";
    exit 1
fi

echo ""

# set required hosts
echo "127.0.0.1 dummy.opa.test" >> "/etc/hosts"
echo "127.0.0.1 dummy.server.test" >> "/etc/hosts"

if [[ "$2" == "" ]]; then
  echo "Executing without system proxy configuration"
fi

if [[ "$2" == "proxy" ]]; then
  echo "Executing with system proxy configuration without exceptions"
  export "http_proxy=http://nowhere.example.com"
  export "HTTP_PROXY=http://nowhere.example.com"
fi

if [[ "$2" == "noproxy" ]]; then
  echo "Executing with system proxy configuration with no proxy settings"
  export "http_proxy=http://nowhere.example.com"
  export "no_proxy=dummy.opa.test,dummy.server.test"
  export "HTTP_PROXY=http://nowhere.example.com"
  export "NO_PROXY=dummy.opa.test,dummy.server.test"
fi

echo ""

# run only proxy tests
./gradlew :sda-commons-client-jersey:test --tests 'Proxy*Test' --rerun-tasks :sda-commons-server-auth:test --tests 'Proxy*Test' --rerun-tasks 2>&1 | grep "Proxy" | grep "should" | grep "()"

echo ""
