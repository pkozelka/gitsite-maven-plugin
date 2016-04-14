GITSITE_DEPLOY=net.kozelka.maven:gitsite-maven-plugin:1.0-SNAPSHOT:deploy

build:
	mvn clean install site

site:
	mvn clean site

site-deploy-first:
	mvn $(GITSITE_DEPLOY) -Dgitsite.inputDirectory=$(PWD)/target/site -Dgitsite.keepHistory=false

site-deploy:
	mvn $(GITSITE_DEPLOY) -Dgitsite.inputDirectory=$(PWD)/target/site
