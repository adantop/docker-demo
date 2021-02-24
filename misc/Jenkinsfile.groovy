node {
   def commit_id
   
   stage('setup') {
      checkout scm
      sh 'git rev-parse --short HEAD > .git/commit-id'
      commit_id = readFile('.git/commit-id').trim()
   }
   
   stage('unit-test') {
      docker.withServer('tcp://192.168.0.146:2376', 'localDocker') {
         docker.withRegistry('', 'dockerhub') {
            
            docker.image('node:4.6').inside {
               sh 'npm install --only=dev'
               sh 'npm test'
            }
         }
      }
   }
   
   stage('integration-test') {
      docker.withServer('tcp://192.168.0.146:2376', 'localDocker') {
         docker.withRegistry('', 'dockerhub') {
            
            docker.image('mysql').withRun("-e MYSQL_ALLOW_EMPTY_PASSWORD=yes") { mysql ->
               // using linking, mysql will be available at host: mysql, port: 3306
               docker.image('node:4.6').inside("--link ${mysql.id}:mysql") {
                   sh 'npm install --only=dev' 
                   sh 'npm test'                     
               }
            }
         }
      }
   }
   
   /*
   stage('docker build/push') {
      docker.withServer('tcp://192.168.0.146:2376', 'localDocker') {
         docker.withRegistry('', 'dockerhub') {
            def app = docker.build("adantop/docker-nodejs-demo:${commit_id}", '.').push()
         }
      }
   }*/
}
