#!/usr/bin/env groovy

pipeline {
	tools {
		maven 'maven-3'
		jdk 'JDK-8'
	}
	
	agent { 
		node {
			label 'put&&some&&labels'
		}
	}
  	options {
      	buildDiscarder(logRotator(artifactDaysToKeepStr: '2', artifactNumToKeepStr: '1', daysToKeepStr: '5', numToKeepStr: '5'))
    }
	
	stages {
		stage ("build maven && deploy artifactory") {
			steps {
				script {
			        def server = Artifactory.newServer url: ARTIFACTORY_URL, credentialsId: CREDENTIALS_IDENTIFIER
			        server.setBypassProxy true
			        def buildInfo = Artifactory.newBuildInfo()
			        buildInfo.env.capture = true
			        def rtMaven = Artifactory.newMavenBuild()
			        
			        rtMaven.tool = "maven-3"
			        rtMaven.deployer releaseRepo: 'brandapi', snapshotRepo: 'brand-api-snapshot', server: server
			        rtMaven.deployer.artifactDeploymentPatterns.addInclude("*.zip").addExclude("*.jar") //specify patterns
			        rtMaven.run pom: 'pom.xml', goals: 'clean package install -U', buildInfo: buildInfo
			        
			        buildInfo.retention maxBuilds: 10, maxDays: 7, deleteBuildArtifacts: true
			        server.publishBuildInfo buildInfo
				}
			}
		}
		
		stage ("post-cleanup") {
			steps {
				cleanWs()
			}
		}
	}
}
