def dockerBuildTag = 'latest'
stage 'build'
node('docker') {
    docker.withServer('tcp://127.0.0.1:1234'){
            docker.image('kmadel/maven:3.3.3-jdk-8').inside('-v /data:/data') {
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cloudbees/mobile-deposit-api.git']]])
                sh 'mvn -s /data/mvn/settings.xml -Dmaven.repo.local=/data/mvn/repo clean package'

                stage 'integration-test'
                sh 'mvn -s /data/mvn/settings.xml -Dmaven.repo.local=/data/mvn/repo verify'
                step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

                stage 'release'
                archive 'target/*.jar, target/Dockerfile'
                sh 'sleep 10'
            }
    }
        unarchive mapping: ['target/*.jar' : '.', 'target/Dockerfile' : '.']
        stage 'build docker image'
        def mobileDepositApiImage = docker.build "kmadel/mobile-deposit-api:${dockerBuildTag}"

        stage 'deploy to production'
        sh "docker run -d --name mobile-deposit-api -p 8080:8080 mobile-deposit-api:${dockerBuildTag}"

        stage 'publish docker image'
        docker.withRegistry('https://registry.hub.docker.com/', 'docker-registry-kmadel-login') {
            //mobileDepositApiImage.push()
            mobileDepositApiImage.push "${dockerBuildTag}"
        }
}