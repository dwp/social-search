FROM openjdk:8

# Install SBT
ENV SBT_VERSION=0.13.12
ENV PATH=${PATH}:/usr/local/sbt/bin
RUN wget "https://dl.bintray.com/sbt/native-packages/sbt/${SBT_VERSION}/sbt-${SBT_VERSION}.tgz"
RUN tar -xz -f sbt-${SBT_VERSION}.tgz -C /usr/local

# Compile and produce a fat JAR
COPY . /opt/socialsearch/src
WORKDIR /opt/socialsearch/src
RUN sbt package
RUN mv target/scala-2.11/*.jar /opt/socialsearch/socialsearch.jar
RUN echo '#!/bin/bash' > /opt/socialsearch/start.sh
RUN echo "DEPENDENCIES=$(cat target/streams/compile/dependencyClasspath/\$global/streams/export)" >> /opt/socialsearch/start.sh
RUN echo 'java -cp /opt/socialsearch/socialsearch.jar:${DEPENDENCIES} SocialSearch' >> /opt/socialsearch/start.sh
RUN chmod u+x /opt/socialsearch/start.sh
RUN rm -rf /opt/socialsearch/src

# Executing the container
EXPOSE 8080
CMD /opt/socialsearch/start.sh
