node {
   def commit_id
   
   stage('setup') {
      checkout scm
      sh 'git rev-parse --short HEAD > .git/commit-id'
      commit_id = readFile('.git/commit-id').trim()
   }
   
   stage('unit-test') {
      docker.withServer('tcp://192.168.0.146:2376', 'localDocker') {
         docker.withRegistry('https://index.docker.io/v1/', 'dockerhub') {
            
            docker.image('node:4.6').inside {
               sh 'npm install --only=dev'
               sh 'npm test'
            }
         }
      }
   }
   
   stage('integration-test') {
      docker.withServer('tcp://192.168.0.146:2376', 'localDocker') {
         docker.withRegistry('https://index.docker.io/v1/', 'dockerhub') {
            def mysql = docker.image('mysql').run("-e MYSQL_ALLOW_EMPTY_PASSWORD=yes") 
            def myTestContainer = docker.image('node:4.6')
            myTestContainer.pull()
            myTestContainer.inside("--link ${mysql.id}:mysql") { // using linking, mysql will be available at host: mysql, port: 3306
                sh 'npm install --only=dev' 
                sh 'npm test'                     
            }                                   
            mysql.stop()
         }
      }
   }
   
   stage('docker build/push') {
      docker.withServer('tcp://192.168.0.146:2376', 'localDocker') {
         docker.withRegistry('https://index.docker.io/v1/', 'dockerhub') {
            def app = docker.build("adantop/docker-nodejs-demo:${commit_id}", '.').push()
         }
      }
   }
}
