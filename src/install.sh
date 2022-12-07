mvn install:install-file -Dfile=target/hl7_utils-1.1.0-SNAPSHOT.jar -DgroupId=gov.cdc.ncezid.eip -DartifactId=hl7_utils -Dversion=1.1.0-SNAPSHOT -Dpackaging=jar -DpomFile=target/hl7_utils-1.1.0-SNAPSHOT.pom


export VERSION=1.2.7.1 && \
mvn deploy:deploy-file -DrepositoryId=nexus -Durl=https://imagehub.cdc.gov/repository/maven-ede/ -Dfile=target/scala-2.13/hl7-pet_2.13-$VERSION.jar -DgroupId=gov.cdc.hl7 -DartifactId=hl7-pet_2.13 -Dversion=$VERSION -Dpackaging=jar -DpomFile=target/scala-2.13/hl7-pet_2.13-$VERSION.pom



mvn deploy:deploy-file -DrepositoryId=nexus -Durl=https://imagehub.cdc.gov/repository/maven-ede/ -Dfile=phinVadsClient-2.0.0.jar -DgroupId=gov.cdc.vocab -DartifactId=phinvads-client -Dversion=2.0.0 -Dpackaging=jar
