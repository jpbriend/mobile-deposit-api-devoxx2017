// Mandatory environment variables:
// DOCKER_REGISTRY (without http://, no ending /)

// You need to create 1 Jenkins Credential
// * 1 type 'username with password' with ID 'test-registry'

def buildVersion = null
def short_commit = null

podTemplate(label: 'mypod',
            containers: [
              containerTemplate(name: 'maven', image: 'maven:3.3.9-jdk-8-alpine', ttyEnabled: true, command: 'cat'),
              containerTemplate(name: 'kubectl', image: 'jcorioland/devoxx2017attendee', ttyEnabled: true, command: 'cat'),
              containerTemplate(name: 'docker', image: 'docker:1.12.6', ttyEnabled: true, command: 'cat')
            ],
            volumes: [
              hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')]) {


  stage ('Build') {

    // Asking for an agent with label 'mypod'. Kubernetes plugin will provide a K8s pod as an agent
    node('mypod') {

      checkout scm

      // Let's retrieve the SHA-1 on the last commit (to identify the version we build)
      sh('git rev-parse HEAD > GIT_COMMIT')
      git_commit=readFile('GIT_COMMIT')
      short_commit=git_commit.take(7)

      // Let's build the application inside a Docker container
      container('maven') {

        sh "mvn -DGIT_COMMIT='${short_commit}' -DBUILD_NUMBER=${env.BUILD_NUMBER} -DBUILD_URL=${env.BUILD_URL} clean package"

        // Tell Jenkins to archive the results of the Unit tests
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'

        // Let's stash various files, mandatory of the pipeline
        stash name: 'jar-dockerfile', includes: '**/target/*.jar,**/target/Dockerfile'
        stash name: 'deployment.yml', includes:'deployment.yml'
      }
    }
  }

  node('mypod') {
    
    //unstash Spring Boot JAR and Dockerfile
    dir('target') {

      unstash 'jar-dockerfile'
      def dockerTag = "${env.BUILD_NUMBER}-${short_commit}"
      
      container('docker') {

        stage('Build Docker Image') {
          sh "docker build -t ${DOCKER_REGISTRY}/mobile-deposit-api:${dockerTag} target"
        }

        stage('Publish Docker Image') {
          withCredentials([[$class: 'UsernamePasswordMultiBinding',
                            credentialsId: 'test-registry',
                            usernameVariable: 'USERNAME',
                            passwordVariable: 'PASSWORD']]) {
              sh """
                docker login ${DOCKER_REGISTRY} --username ${USERNAME} --password ${PASSWORD}
                docker push ${DOCKER_REGISTRY}/mobile-deposit-api:${dockerTag}
              """
          }
        }

      }
    }
  }

  stage('Deploy to Kubernetes') {

    node('mypod') {

      container('kubectl') {
      
        unstash 'deployment.yml'

        // Execute this sh script
        sh """
          # Check if the Kubernetes credentials have been correctly installed
          kubectl version
          
          # Update the deployment.yml with the latest versions of the app
          sed -i 's/REGISTRY_NAME/${env.DOCKER_REGISTRY}/g' ./deployment.yml
          sed -i 's/IMAGE_TAG/${dockerTag}/g' ./deployment.yml

          # Deploy the application
          kubectl apply -f ./deployment.yml

          # Display the installed services (may also display the external IP if the service has been exposed)
          kubectl get services
          
          """
        
        currentBuild.result = "Success"
      }
    }
  }
}
