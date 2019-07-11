@Library('jenkins-library') _

pipeline {
  agent none

  options {
    disableConcurrentBuilds()
    timeout time: 1, unit: 'HOURS'
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
          when {
            branch 'master'
            not { environment name: 'SEMANTIC_VERSION', value: '' }
          }

          stages {
            stage('Create Release') {
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
              agent {
                docker {
                  image 'quay.io/sdase/openjdk-development:8.212-hotspot'
                }
              }

              steps {
                prepareGradleWorkspace secretId: 'sdabot-github-token'
                javaGradlew gradleCommand: 'uploadArchives'
              }
            }
          }
        }

        stage("Snapshot") {
          when {
            changeRequest()
          }

          stages {
            stage('Upload Release') {
              agent {
                docker {
                  image 'quay.io/sdase/openjdk-development:8.212-hotspot'
                }
              }

              steps {
                script {
                  env.SEMANTIC_VERSION = "${BRANCH_NAME}-SNAPSHOT"
                }

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
