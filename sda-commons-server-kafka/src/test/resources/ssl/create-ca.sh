#!/bin/bash

# This script was used to create the keystore.* files. This is mostly for documentational purposes
# and doesn't need to be executed again.

# create CA
openssl req -new -x509 -keyout ca-key -out ca-cert -days 18000 -subj "/C=DE/ST=Hamburg/L=Hamburg/O=localhost/OU=localhost/CN=localhost" -passout pass:casecret
keytool -keystore kafka.client.truststore.jks -alias CARoot -importcert -file ca-cert -keypass client-password -storepass client-password -noprompt
keytool -keystore kafka.server.truststore.jks -alias CARoot -importcert -file ca-cert -keypass password -storepass password -noprompt

# create and sign server cert
keytool -keystore kafka.server.keystore.jks -alias localhost -validity 18000 -genkey -keyalg RSA -storepass password -keypass password -dname "CN=localhost, OU=localhost, O=localhost, L=localhost, ST=localhost, C=localhost"
keytool -keystore kafka.server.keystore.jks -alias localhost -certreq -file cert-file -keypass password -storepass password
openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days 18000 -CAcreateserial -passin pass:casecret
keytool -keystore kafka.server.keystore.jks -alias CARoot -importcert -file ca-cert -keypass password -storepass password -noprompt
keytool -keystore kafka.server.keystore.jks -alias localhost -import -file cert-signed -storepass password -keypass password

# create and sign client cert
keytool -keystore kafka.client.keystore.jks -alias localhost -validity 18000 -genkey -keyalg RSA -storepass client-password -keypass client-password -dname "CN=ssl-client-test, OU=localhost, O=localhost, L=localhost, ST=localhost, C=localhost"
keytool -keystore kafka.client.keystore.jks -alias localhost -certreq -file cert-file -keypass client-password -storepass client-password
openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days 18000 -CAcreateserial -passin pass:casecret
keytool -keystore kafka.client.keystore.jks -alias CARoot -importcert -file ca-cert -keypass client-password -storepass client-password -noprompt
keytool -keystore kafka.client.keystore.jks -alias localhost -import -file cert-signed -storepass client-password -keypass client-password

rm cert-signed cert-file ca-cert.srl ca-key
