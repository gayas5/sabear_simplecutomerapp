pipeline {
    agent any

    tools {
        maven "maven"
    }

    stages {
        
        stage('Checkout') {
            steps {
                git branch: 'master',
                    url: 'https://github.com/gayas5/sabear_simplecutomerapp.git'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean install -f pom.xml'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    withSonarQubeEnv('sonar') {
                        sh 'mvn sonar:sonar'
                    }
                }
            }
        }

        stage('Nexus Upload') {
            steps {
                script {
                    nexusArtifactUploader(
                        nexusVersion: 'nexus3',
                        protocol: 'http',
                        nexusUrl: '44.203.98.235:8081',
                        repository: 'SimpleCustomerApp',    // use snapshot/release accordingly
                        credentialsId: 'nexus',
                        groupId: 'com.javatpoint',
                        version: "${BUILD_NUMBER}-SNAPSHOT",
                        artifacts: [[
                            artifactId: 'SimpleCustomerApp',
                            classifier: '',
                            type: 'war',
                            file: "target/SimpleCustomerApp-${BUILD_NUMBER}-SNAPSHOT.war"
                        ]]
                    )
                }
            }
        }

        stage('Deploy to Tomcat') {
            steps {
                script {
                    deploy adapters: [
                        tomcat9(
                            credentialsId: 'tomcat',
                            path: '',
                            url: 'http://13.53.218.134:8080'
                        )
                    ],
                    war: '**/*.war'
                }
            }
        }
    }

    post {
        success {
            slackSend(
                channel: '#jenkins-integration',
                color: '#36a64f',
                message: "Deployment completed successfully for build #${BUILD_NUMBER}",
                tokenCredentialId: 'slack'
            )
        }

        failure {
            slackSend(
                channel: '#jenkins-integration',
                color: '#ff0000',
                message: "peline failed for build #${BUILD_NUMBER}. Check Jenkins logs.",
                tokenCredentialId: 'slack'
            )
        }
    }
}
