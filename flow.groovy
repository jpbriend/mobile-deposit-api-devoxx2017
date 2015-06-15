def dockerBuildTag = 'latest'
def buildVersion = null
stage 'build'
node('docker') {
    docker.withServer('tcp://127.0.0.1:1234'){
            docker.image('kmadel/maven:3.3.3-jdk-8').inside('-v /data:/data') {
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cloudbees/mobile-deposit-api.git']]])
                sh 'mvn -s /data/mvn/settings.xml -Dmaven.repo.local=/data/mvn/repo clean package'

                stage 'integration-test'
                //sh 'mvn -s /data/mvn/settings.xml -Dmaven.repo.local=/data/mvn/repo verify'
                //step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

                stage 'release'
                def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
                if (matcher) {
                    buildVersion = matcher[0][1]
                    echo "Releaed version ${buildVersion}"
                }
                matcher = null
                archive "target/mobile-deposit-api.jar, target/Dockerfile"
            }

  docker.withServer('tcp://54.173.235.97:2375'){

        unarchive mapping: ['target/mobile-deposit-api.jar' : '.', 'target/Dockerfile' : '.']
        stage 'build docker image'
        docker.build "kmadel/mobile-deposit-api:${dockerBuildTag}"

        stage 'deploy to production'
        try{
          sh "docker stop mobile-deposit-api"
          sh "docker rm mobile-deposit-api"
        } catch (Exception _) {
           echo "no container to stop"        
        }
        sh "docker run -d --name mobile-deposit-api -p 8080:8080 kmadel/mobile-deposit-api:${dockerBuildTag}"
        //hack to kick off ui job
        //sh 'curl "http://webhook:13461862c863d7df39e63435eb17deb9@jenkins-latest.beedemo.net/mobile-team/job/mobile-deposit-ui-workflow/build?token=llNSDXpfTim4Bm2SIIoQezwwQOHmEMYgSeHSUnL"'
   }
        stage 'publish docker image'
        docker.withServer('tcp://127.0.0.1:1234') {
            docker.withRegistry('https://registry.hub.docker.com/', 'docker-registry-kmadel-login') {
                unarchive mapping: ['target/mobile-deposit-api.jar' : '.', 'target/Dockerfile' : '.']
                def mobileDepositApiImagePush = docker.build "kmadel/mobile-bank-api:latest"
                mobileDepositApiImagePush.push()
            }
        }

  }
}