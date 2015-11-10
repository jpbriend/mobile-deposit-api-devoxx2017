import java.util.Random
def Random rand = new Random()
def int max = 10

def buildVersion = null
stage 'Build'
node('docker-cloud') {
    //docker.withServer('tcp://127.0.0.1:1234') {
        docker.image('kmadel/maven:3.3.3-jdk-8').inside('-v /data:/data') {
            checkout([$class: 'GitSCM', branches: [[name: '*/master']], clean: true, doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cloudbees/mobile-deposit-api.git']]])
            sh 'mvn -Dmaven.repo.local=/data/mvn/repo -Dsonar.jdbc.username=NULL -Dsonar.jdbc.password=NULL clean package'
        }
    //}
    archive 'pom.xml, src/, target/'
//}

checkpoint 'Build Complete'

stage 'Quality Analysis'
//node('docker-cloud') {
    unarchive mapping: ['pom.xml' : '.', 'src/' : '.']
    waitUntil {
        try {
            readFile 'src/main/java/com/cloudbees/example/mobile/deposit/api/DepositEndpoint.java'
            return true
        } catch (FileNotFoundException _) {
            return false
        }
    }
    //docker.withServer('tcp://127.0.0.1:1234') {
        //test in paralell
        parallel(
            integrationTests: {
              docker.image('kmadel/maven:3.3.3-jdk-8').inside('-v /data:/data') {
                  sh 'mvn -Dmaven.repo.local=/data/mvn/repo -Dsonar.jdbc.username=NULL -Dsonar.jdbc.password=NULL verify'
              }
            }, sonarAnalysis: {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'sonar.beedemo',
                    usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                    echo 'run sonar tests'
                  //docker.image('kmadel/maven:3.3.3-jdk-8').inside('-v /data:/data') {
                    //sh 'mvn -Dmaven.repo.local=/data/mvn/repo -Dsonar.scm.disabled=True -Dsonar.jdbc.username=$USERNAME -Dsonar.jdbc.password=$PASSWORD sonar:sonar'
                  //}
                }
            }, failFast: true
        )
    //}
//}

checkpoint 'Quality Analysis Complete'
stage 'Version Release'
//node('docker-cloud') {
    unarchive mapping: ['pom.xml' : '.', 'target/' : '.']

    def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
    if (matcher) {
        buildVersion = matcher[0][1]
        echo "Release version: ${buildVersion}"
    }
    matcher = null
    
    docker.withServer('tcp://52.26.31.52:3376', 'beedemo-swarm-cert'){

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
        //docker traceability rest call
        container = mobileDepositApiImage.run("--name mobile-deposit-api -p 8080:8080")
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'webhook-login',
            usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
          sh "curl http://$USERNAME:$PASSWORD@jenkins.beedemo.net/api-team/docker-traceability/submitContainerStatus \
            --data-urlencode status=deployed \
            --data-urlencode hostName=prod-server-1 \
            --data-urlencode hostName=prod \
            --data-urlencode imageName=cloudbees/mobile-deposit-api \
            --data-urlencode inspectData=\"\$(docker inspect $container.id)\""
        }
        
        stage 'Publish Docker Image'
        sh "docker -v"
        //need to make sure we are logged in to docker hub registry
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'docker-registry-kmadel-login',
            usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            sh 'docker login --username=$USERNAME --password=$PASSWORD --email=kmadel@mac.com'
        }
        mobileDepositApiImage.push()
   }
}
