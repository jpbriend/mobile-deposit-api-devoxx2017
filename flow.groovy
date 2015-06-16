def dockerBuildTag = 'latest'
def buildVersion = null
stage 'build'
node('docker') {
    docker.withServer('tcp://127.0.0.1:1234'){
            docker.image('kmadel/maven:3.3.3-jdk-8').inside('-v /data:/data') {
                sh 'rm -rf *'
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], clean: true, doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/cloudbees/mobile-deposit-api.git']]])
                sh 'git checkout master'
                sh 'git config user.email "kmadel@mac.com"'
                sh 'git config user.name "kmadel"'
                sh 'git remote set-url origin git@github.com:cloudbees/mobile-deposit-api.git'
                sh 'mvn -s /data/mvn/settings.xml -Dmaven.repo.local=/data/mvn/repo clean package'

                stage 'integration-test'
                //sh 'mvn -s /data/mvn/settings.xml -Dmaven.repo.local=/data/mvn/repo verify'
                //step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])

                stage 'prepare release'
                def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
                if (matcher) {
                    buildVersion = matcher[0][1]
                    echo "Releaed version ${buildVersion}"
                }
                matcher = null
                archive 'target', includes: ['*.jar', 'Dockerfile']
            }

  docker.withServer('tcp://54.173.235.97:2375'){

        unarchive mapping: ['target/': '.']
        input 'Were the file unarchived properly?'
        stage 'build docker image'
        docker.build "kmadel/mobile-deposit-api:${dockerBuildTag}"

        stage 'deploy to production'
        try{
          sh "docker stop mobile-deposit-api"
          sh "docker rm mobile-deposit-api"
        } catch (Exception _) {
           echo "no container to stop"        
        }
        sh "docker run -d --name mobile-deposit-api -p 8080:8080 kmadel/mobile-deposit-api:${buildVersion}"
        //hack to kick off ui job
        //sh 'curl "http://webhook:13461862c863d7df39e63435eb17deb9@jenkins-latest.beedemo.net/mobile-team/job/mobile-deposit-ui-workflow/build?token=llNSDXpfTim4Bm2SIIoQezwwQOHmEMYgSeHSUnL"'
        stage 'publish docker image'
        //sh 'curl -H "Content-Type: application/json" -X POST -d \'{"push_data": {"pushed_at": 1434386606, "images": null, "pusher": "kmadel"}, "callback_url": "https://registry.hub.docker.com/u/kmadel/mobile-bank-api/hook/21a0ic0dje2ff4hg3f3hbg23b5220454b/", "repository": {"status": "Active", "description": "", "is_trusted": false, "full_description": "", "repo_url": "https://registry.hub.docker.com/u/kmadel/mobile-bank-api/", "owner": "kmadel", "is_official": false, "is_private": false, "name": "mobile-bank-api", "namespace": "kmadel", "star_count": 0, "comment_count": 0, "date_created": 1434385021, "repo_name": "kmadel/mobile-bank-api"}}\' http://webhook:13461862c863d7df39e63435eb17deb9@jenkins-latest.beedemo.net/mobile-team/dockerhub-webhook/notify'
   }
  }
}