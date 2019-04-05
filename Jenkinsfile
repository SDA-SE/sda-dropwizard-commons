@Library('jenkins-library') _

pipeline {
  agent {
    node {
      label 'master'
    }
  }
  options {
    disableConcurrentBuilds()
  }
  stages {
    stage('Generate Semantic Release') {
      agent {
        docker {
          image 'quay.io/sdase/semantic-release:15.13.3'
        }
      }
      steps {
        script {
           env.SEMANTIC_VERSION = semanticRelease dryRun: "true"
        }
      }
    }

    stage('Build jar') {
      agent {
        docker {
          image 'quay.io/sdase/openjdk:8u191-alpine-3.8'
        }
      }
      steps {
        prepareGradleWorkspace secretId: 'sdabot-github-token'
        javaGradlew gradleCommand: 'assemble', clean: true
        stash includes: '**/build/libs/*', name: 'build'
      }
    }

    stage('Run Tests') {
      parallel {
        stage('Module test jar (Java 8)') {
          agent {
            docker {
              image 'quay.io/sdase/openjdk:8u191-bionic'
            }
          }
          steps {
            prepareGradleWorkspace secretId: 'sdabot-github-token'
            javaGradlew gradleCommand: 'test'
            script {
              def testResults = findFiles(glob: '**/build/test-results/test/*.xml')
              for(xml in testResults) {
                touch xml.getPath()
              }
            }
            stash includes: '**/build/**/*', name: 'moduletest'
          }
          post {
            always {
              junit '**/build/test-results/test/*.xml'
            }
          }
        }
        stage('Integration test service (Java 8)') {
          agent {
            docker {
              image 'quay.io/sdase/openjdk:8u191-bionic'
            }
          }
          steps {
            prepareGradleWorkspace secretId: 'sdabot-github-token'
            javaGradlew gradleCommand: 'integrationTest'
            script {
              def testResults = findFiles(glob: '**/build/integTest-results/*.xml')
              for(xml in testResults) {
                touch xml.getPath()
              }
            }
            stash includes: '**/build/**/*', name: 'integrationtest'
          }
          post {
            always {
              junit '**/build/integTest-results/*.xml'
            }
          }
        }
        stage('Module test jar (Java 11)') {
          agent {
            docker {
              image 'openjdk:11-jdk-slim'
            }
          }
          steps {
            prepareGradleWorkspace secretId: 'sdabot-github-token'
            javaGradlew gradleCommand: 'test'
            script {
              def testResults = findFiles(glob: '**/build/test-results/test/*.xml')
              for(xml in testResults) {
                touch xml.getPath()
              }
            }
            //stash includes: '**/build/**/*', name: 'moduletest'
          }
          post {
            always {
              junit '**/build/test-results/test/*.xml'
            }
          }
        }
        stage('Integration test service (Java 11)') {
          agent {
            docker {
              image 'openjdk:11-jdk-slim'
            }
          }
          steps {
            prepareGradleWorkspace secretId: 'sdabot-github-token'
            javaGradlew gradleCommand: 'integrationTest'
            script {
              def testResults = findFiles(glob: '**/build/integTest-results/*.xml')
              for(xml in testResults) {
                touch xml.getPath()
              }
            }
            //stash includes: '**/build/**/*', name: 'integrationtest'
          }
          post {
            always {
              junit '**/build/integTest-results/*.xml'
            }
          }
        }
      }
    }

    stage('Sonar Scan Sources (Publish to SonarQube)') {
      when {
        branch 'master'
      }
      steps {
        unstash 'moduletest'
        unstash 'integrationtest'
        sonarScanBranch project: 'org.sdase.commons', javaBaseDir: '.', exclusionPatterns: ['**main/generated/**']
      }
    }

    stage('Sonar Scan Sources (Annotate Pull Request)') {
      when {
        changeRequest()
      }
      steps {
        unstash 'moduletest'
        unstash 'integrationtest'
        sonarScanPullRequest project: 'org.sdase.commons', javaBaseDir: '.', exclusionPatterns: ['**main/generated/**']
      }
    }

    stage('Create Release') {
      when {
        branch 'master'
        not { environment name: 'SEMANTIC_VERSION', value: '' }
      }
      agent {
        docker {
          image 'quay.io/sdase/semantic-release:15.13.3'
        }
      }
      steps {
        semanticRelease()
      }
    }

    stage('Upload release') {
      when {
        branch 'master'
        not { environment name: 'SEMANTIC_VERSION', value: '' }
      }
      agent {
        docker {
          image 'quay.io/sdase/openjdk:8u191-alpine-3.8'
        }
      }
      steps {
        unstash 'build'
        prepareGradleWorkspace secretId: 'sdabot-github-token'
        javaGradlew gradleCommand: 'uploadArchives'
      }
    }
  }
}
