FROM eclipse-temurin:21-jammy AS build

# This docker file is used to test a specific regression from kafka-client:3.8.0 in a distroless
# container with readonly file system.
# See sda-commons-server-kafka/src/test/DockerTest for details.

COPY ./ /project

WORKDIR /project

RUN ls -al
RUN echo ''
RUN echo 'task download(type: Copy) {'            >> ./sda-commons-server-kafka/build.gradle
RUN echo '  from configurations.runtimeClasspath' >> ./sda-commons-server-kafka/build.gradle
RUN echo '  into "build/lib"'                     >> ./sda-commons-server-kafka/build.gradle
RUN echo '}'                                      >> ./sda-commons-server-kafka/build.gradle

RUN ./gradlew :sda-commons-server-kafka:compileTestJava :sda-commons-server-kafka:download

RUN mkdir "/build-result"
RUN mkdir "/build-result/cp"
RUN cp /project/sda-commons-server-kafka/build/classes/java/test/DockerTest.class /build-result/DockerTest.class
RUN cp /project/sda-commons-server-kafka/build/lib/*.jar /build-result/cp

FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /home
COPY --from=build --chown=65532:65532 /build-result ./nonroot
WORKDIR /home/nonroot
USER 65532:65532

ENTRYPOINT ["/usr/bin/java"]
CMD ["-cp", "/home/nonroot/cp/*:.", "DockerTest"]
