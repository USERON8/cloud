pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
    }

    parameters {
        booleanParam(name: 'RUN_FRONTEND', defaultValue: true, description: 'Build frontend (my-shop-web)')
        booleanParam(name: 'RUN_DEPLOY', defaultValue: false, description: 'Run local deployment after build')
        booleanParam(name: 'RUN_SMOKE', defaultValue: true, description: 'Run smoke test after deployment')
    }

    environment {
        MAVEN_VERSION = '3.9.9'
        MAVEN_OPTS = '-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=20 -XX:+UseStringDeduplication -Djava.awt.headless=true -Dfile.encoding=UTF-8'
        JAVA_TOOL_OPTIONS = '-XX:+UseG1GC -XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=20 -XX:+UseStringDeduplication -Dfile.encoding=UTF-8'
        NODE_OPTIONS = '--max-old-space-size=2048'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Environment Check') {
            steps {
                sh 'bash --version | head -1'
                sh 'java -version'
                sh 'docker --version'
                sh 'docker version --format \'{{.Server.Version}}\''
            }
        }

        stage('Backend Unit Test') {
            steps {
                sh 'bash scripts/ci/mvnw-local.sh -T 1C -DskipITs clean test'
            }
        }

        stage('Backend Package') {
            steps {
                sh 'bash scripts/ci/mvnw-local.sh -T 1C -DskipTests package'
            }
        }

        stage('Frontend Build') {
            when {
                expression { return params.RUN_FRONTEND }
            }
            steps {
                dir('my-shop-web') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true, allowEmptyArchive: false
                archiveArtifacts artifacts: 'my-shop-web/dist/**', allowEmptyArchive: true
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

        stage('Smoke Test') {
            when {
                expression { return params.RUN_DEPLOY && params.RUN_SMOKE }
            }
            steps {
                sh 'bash scripts/ci/smoke-local.sh'
            }
        }
    }

    post {
        failure {
            script {
                if (params.RUN_DEPLOY) {
                    sh 'bash scripts/ci/rollback-local.sh || true'
                }
            }
        }
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            archiveArtifacts artifacts: '.tmp/acceptance/startup.csv,.tmp/acceptance/logs/*.log', allowEmptyArchive: true
        }
    }
}
