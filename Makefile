GITSITE_DEPLOY=net.kozelka.maven:gitsite-maven-plugin:0.1-SNAPSHOT:deploy

build:
	mvn clean install site

site:
	mvn clean site

site-deploy-first:
	mvn $(GITSITE_DEPLOY) -Dgitsite.inputDirectory=$(PWD)/target/site -Dgitsite.branch=gh-pages -Dgitsite.keepHistory=false

site-deploy:
	mvn $(GITSITE_DEPLOY) -Dgitsite.inputDirectory=$(PWD)/target/site -Dgitsite.branch=gh-pages
