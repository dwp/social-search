FROM openjdk:8

ARG MC_API_KEY
ARG ES_HOSTNAME=localhost
ARG ES_PORT=9200

ENV SBT_VERSION=0.13.12
ENV PATH=${PATH}:/usr/local/sbt/bin

# Install SBT
RUN wget "https://dl.bintray.com/sbt/native-packages/sbt/${SBT_VERSION}/sbt-${SBT_VERSION}.tgz"
RUN tar -xz -f sbt-${SBT_VERSION}.tgz -C /usr/local

COPY . /opt/socialsearch/src
WORKDIR /opt/socialsearch/src

# Set the API key for meaning cloud
RUN sed -i -e "s/<api_key>/${MC_API_KEY}/" src/main/resources/application.conf

# Set the hostname for Elastic Search
RUN sed -i -e "s/localhost:9200/${ES_HOSTNAME}:${ES_PORT}/" src/main/resources/application.conf

# Compile the code, packaged as a jar, then create a script to run it.
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
