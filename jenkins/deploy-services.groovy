pipeline {
    agent any
    parameters{
        booleanParam(name: 'CREATE_INFAR', defaultValue: false, description: 'Flag to trigger create infrastructure')
        string(name: 'BRANCH_BUILD', defaultValue: 'staging', description: 'The branch of git')
        string(name: 'BUILD_SERVICES', defaultValue: '', description: 'List of build services')
    }
    stages {
        stage('Remote to k8s cluster') {
            when{
                expression {
                    return Boolean.valueOf(CREATE_INFAR)
                }
            }
            steps {
                sh 'aws eks --region us-east-1 update-kubeconfig --name Cap-Pro-Eks-Cluster'
                sh 'kubectl create secret docker-registry ecr-secret \
                    --docker-server=248155485793.dkr.ecr.us-east-1.amazonaws.com \
                    --docker-username=AWS \
                    --docker-password=$(aws ecr get-login-password)'
            }
        }
        stage('Deploy Front End Service') {
            when{
                expression {
                    return "${BUILD_SERVICES}".contains("frontend-service")
                }
            }
            steps {
                sh 'kubectl get nodes -o wide'
                sh 'kubectl apply -f kubenetes/frontend-service.yml'
            }
        }
        stage('Deployment status') {
            steps {
                sh 'kubectl get nodes'
                sh 'kubectl get pods'
            }
        }
    }
    post {
        always {
            deleteDir() /* clean up our workspace */
        }
    }
}