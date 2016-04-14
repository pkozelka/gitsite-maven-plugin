build:
	mvn clean install site

site:
	mvn clean site

site-deploy-first:
	mvn net.kozelka.maven:gitsite-maven-plugin:1.0-SNAPSHOT:gitsite-deploy -Dgitsite.inputDirectory=$(PWD)/target/site -Dgitsite.keepHistory=false

site-deploy:
	mvn net.kozelka.maven:gitsite-maven-plugin:1.0-SNAPSHOT:gitsite-deploy -Dgitsite.inputDirectory=$(PWD)/target/site

