pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
    }

    parameters {
        booleanParam(name: 'RUN_DEPLOY', defaultValue: false, description: 'Run local deploy stage after package')
    }

    environment {
        MAVEN_VERSION = '3.9.9'
        MAVEN_OPTS = '-Xmx2g -Djava.awt.headless=true'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build And Test') {
            steps {
                sh 'bash scripts/ci/mvnw-local.sh -T 1C -DskipITs clean test'
            }
        }

        stage('Package') {
            steps {
                sh 'bash scripts/ci/mvnw-local.sh -T 1C -DskipTests package'
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true, allowEmptyArchive: false
            }
        }

        stage('Deploy Local') {
            when {
                expression { return params.RUN_DEPLOY }
            }
            steps {
                sh 'bash scripts/ci/deploy-local.sh'
            }
        }
    }

    post {
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: '.tmp/acceptance/startup.csv,.tmp/acceptance/logs/*.log', allowEmptyArchive: true
        }
    }
}
