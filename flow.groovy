import java.util.Random
def Random rand = new Random()
def int max = 10

def buildVersion = null
stage 'Build'
node('docker') {
    docker.withServer('tcp://127.0.0.1:1234') {
        docker.image('kmadel/maven:3.3.3-jdk-8').inside('-v /data:/data') {
            sh 'rm -rf *'
            checkout([$class: 'GitSCM', branches: [[name: '*/master']], clean: true, doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cloudbees/mobile-deposit-api.git']]])
            sh 'git checkout master'
            sh 'git config user.email "kmadel@mac.com"'
            sh 'git config user.name "kmadel"'
            sh 'git remote set-url origin git@github.com:cloudbees/mobile-deposit-api.git'
            sh 'mvn -s /data/mvn/settings.xml -Dmaven.repo.local=/data/mvn/repo clean package'
        }
    }
    archive 'pom.xml, src/, target/'
}

checkpoint 'Build Complete'

stage 'Quality Analysis'
node('docker') {
    unarchive mapping: ['pom.xml' : '.', 'src/' : '.']
    docker.withServer('tcp://127.0.0.1:1234') {
        //test in paralell
        parallel(
            integrationTests: {
              docker.image('kmadel/maven:3.3.3-jdk-8').inside('-v /data:/data') {
                  sh 'mvn -s /data/mvn/settings.xml -Dmaven.repo.local=/data/mvn/repo verify'
              }
            }, sonarAnalysis: {
                docker.image('kmadel/maven:3.3.3-jdk-8').inside('-v /data:/data') {
                  sh 'mvn -s /data/mvn/settings.xml -Dmaven.repo.local=/data/mvn/repo -Dsonar.scm.disabled=True sonar:sonar'
                }
            }, failFast: true
        )
    }
}

checkpoint 'Quality Analysis Complete'
node('docker') {
    //allows randome testing of above checkpoint
    def failInt = rand.nextInt(max+1)
    if(failInt>6){
        error 'error to allow testing checkpoint'
    } 
    
    unarchive mapping: ['pom.xml' : '.', 'target/' : '.']

    stage 'Version Release'
    def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
    if (matcher) {
        buildVersion = matcher[0][1]
        echo "Release version: ${buildVersion}"
    }
    matcher = null
    
    docker.withServer('tcp://54.165.201.3:2376', 'slave-docker-us-east-1-tls'){

        stage 'Build Docker Image'
        def mobileDepositApiImage
        dir('target') {
            mobileDepositApiImage = docker.build "kmadel/mobile-deposit-api:${buildVersion}"
        }

        stage 'Deploy to Prod'
        try{
          sh "docker stop mobile-deposit-api"
          sh "docker rm mobile-deposit-api"
        } catch (Exception _) {
           echo "no container to stop"        
        }
        mobileDepositApiImage.run("--name mobile-deposit-api -p 8080:8080")
        sh 'curl http://webhook:336838a2daad1ea4ed0d18734ff6a9fb@jenkins.beedemo.net/api-team/docker-traceability/submitContainerStatus --data-urlencode inspectData="$(docker inspect mobile-deposit-api)" --data-urlencode hostName=prod-server-1'
        
        stage 'Publish Docker Image'
        docker.withRegistry('https://registry.hub.docker.com/', 'docker-registry-kmadel-login') {
            mobileDepositApiImage.push()
        }
   }
}
