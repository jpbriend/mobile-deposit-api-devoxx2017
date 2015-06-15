def dockerBuildTag = 'latest'
stage 'build'
node('docker') {
    docker.withServer('tcp://127.0.0.1:1234'){
        docker.withRegistry('https://registry.hub.docker.com/', 'docker-registry-kmadel-login') {
            def maven3 = docker.image('maven:3.3.3-jdk-8')
            maven3.pull()
            maven3.inside() {
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cloudbees/mobile-deposit-api.git']]])
                sh 'mvn clean package'

                stage 'integration-test'
                sh 'mvn verify'
                step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

                stage 'release'
                sh 'mvn -B release:prepare'
                sh 'mvn release:perform -Darguments="-Dmaven.deploy.skip=true"'
                archive 'target/*.jar, target/Dockerfile'
                def matcher = readFile('target/checkout/pom.xml') =~ '<version>(.+)</version>'
                if (matcher) {
                    dockerBuildTag = $ { matcher[0][1] }
                    echo "Releaed version ${dockerBuildTag}"
                }
                matcher = null
            }
        }
    }
    docker.withServer('tcp://54.165.201.3:2376','slave-docker-us-east-1-tls'){
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
}