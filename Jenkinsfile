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
          image 'quay.io/sdase/openjdk-development:8.212-hotspot'
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
              image 'quay.io/sdase/openjdk-development:8.212-hotspot'
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
              image 'quay.io/sdase/openjdk-development:8.212-hotspot'
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
              image 'quay.io/sdase/openjdk-development:11.0-hotspot'
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
              image 'quay.io/sdase/openjdk-development:11.0-hotspot'
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
        stage('License analysis') {
          agent {
            docker {
              image 'quay.io/sdase/openjdk-development:8.212-hotspot'
            }
          }
          steps {
            prepareGradleWorkspace secretId: 'sdabot-github-token'
            fossaLicenseAnalysis project: 'sda-commons', apiKey: 'api-key-service'
          }
        }
      }
    }

    stage('Sonar Scan') {
      agent {
        node {
          label 'master'
        }
      }

      when {
        anyOf {
          branch 'develop'
          changeRequest()
        }
      }

      steps {
        unstash 'moduletest'
        unstash 'integrationtest'
        script {
          if (env.BRANCH_NAME == 'develop') {
            sonarScanBranch project: 'org.sdase.commons', javaBaseDir: '.', exclusionPatterns: ['**main/generated/**']
          } else {
            sonarScanPullRequest project: 'org.sdase.commons', javaBaseDir: '.', exclusionPatterns: ['**main/generated/**']
          }
        }
      }
    }

    stage('Releases') {
      parallel {
        stage('Master') {
          stages {
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

            stage('Upload Release') {
              when {
                branch 'master'
                not { environment name: 'SEMANTIC_VERSION', value: '' }
              }

              agent {
                docker {
                  image 'quay.io/sdase/openjdk-development:8.212-hotspot'
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

        stage("Snapshot") {
          stages {
            stage('Upload Release') {
              when {
                changeRequest()
              }

              agent {
                docker {
                  image 'quay.io/sdase/openjdk-development:8.212-hotspot'
                }
              }

              steps {
                script {
                  env.SEMANTIC_VERSION = "${BRANCH_NAME}-SNAPSHOT"
                }

                unstash 'build'
                prepareGradleWorkspace secretId: 'sdabot-github-token'
                javaGradlew gradleCommand: 'uploadArchives'
              }
            }
          }
        }
      }
    }
  }
}
