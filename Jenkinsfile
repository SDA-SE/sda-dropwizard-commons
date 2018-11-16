@Library('jenkins-library@v-0.30.0') _

pipeline {
   agent none
   stages {
      /*
       * Stage: Prepare Workspace
       *
       * Checks out scm, sets up gradle.properties and replaces placehodlers within
       * feature branch files
       *
       */
      stage('Prepare Workspace') {
         agent {
            label 'master'
          }
         steps {
            prepareGradleWorkspace secretId: 'sdabot-github-token', pathToFile: 'gradle.properties'
         }
      }
      /*
       * Stage: Tag Release
       *
       * Finds the current Tag and increments the version by one
       *
       */
      stage('Tag Release') {
         when {
            branch 'master'
         }
         agent {
            label 'master'
         }
         steps {
            javaGradlew gradleCommand: 'createRelease'
         }
      }
      /*
       * Stage: Gradle Java: Jar
       *
       * Calls the Gradle Wrapper and builds the Jar.
       *
       * Runs in its own Docker container
       */
      stage('Gradle Java: Jar') {
         agent {
            docker {
               image 'openjdk:8-jdk-alpine'
            }
         }
         steps {
            javaGradlew gradleCommand: 'assemble', clean: true
         }
      }
      /*
       * Stage: Java Gradle: Module Test Service
       *
       * Calls the Gradle wrapper with the "test" task in order
       * to module test the software.
       *
       * Publishes an HTML Report if tests are failing
       *
       * Runs in its own Docker container
       *
       */
      stage('Gradle Java: Module Test') {
         agent {
            docker {
               image 'openjdk:8-jdk-alpine'
            }
         }
         steps {
            javaGradlew gradleCommand: 'test'
         }
         post {
            always {
               junit '**/build/test-results/test/*.xml'
            }
         }
      }
      /*
        * Stage: Java Gradle: Integration Test Service
        *
        * Same as Module Test but calling gradlew 'iT'
        *
        */
      stage('Gradle Java: Integration Test') {
         agent {
            docker {
               image 'openjdk:8-jdk-alpine'
            }
         }
         steps {
            javaGradlew gradleCommand: 'integrationTest'
         }
         post {
            always {
               junit '**/build/integTest-results/*.xml'
            }
         }
      }
      /*
       * Run Sonar Scan on all sources for the default branch and publish the results to SonarQube
       */
      stage('Sonar Scan Sources (Publish to SonarQube)') {
         agent {
             label 'master'
         }
         when {
             branch 'master'
         }
         steps {
             sonarScanBranch project: 'org.sdase.commons', javaBaseDir: './'
         }
      }
      /*
       * Run Sonar Scan on all sources for a pull request and annotate the Pull Request on GitHub
       */
      stage('Sonar Scan Sources (Annotate Pull Request)') {
         agent {
             label 'master'
         }
         when {
             changeRequest()
         }
         steps {
             sonarScanPullRequest project: 'org.sdase.commons', javaBaseDir: './'
         }
      }
      /*
       * Stage: Publish release
       *
       * Pushes the Tag to Repository and Uploads the Archive to Nexus
       *
       */
      stage('Publish release') {
         when {
            branch 'master'
         }
         agent {
            label 'master'
         }
         steps {
            withCredentials([usernamePassword(credentialsId: 'sdabot-github-token', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
               sh('git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/SDA-SE/sda-commons --tags')
            }
            javaGradlew gradleCommand: 'uploadArchives'
         }
      }
   }
}