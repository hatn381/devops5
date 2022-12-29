pipeline {
    agent any
    environment {
        GIT_URL = 'https://github.com/hatn381/devops5.git'
        WORKSPACE = 'SOURCE_CODE'
        AWS_ACCOUNT_ID = "523411581086"
        AWS_DEFAULT_REGION = "us-east-1" 
        IMAGE_TAG = "latest"
        REPOSITORY_URI = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"
    }
    parameters{
        string(name: 'BRANCH_BUILD', defaultValue: 'master', description: 'The branch of git')
        string(name: 'BUILD_SERVICES', defaultValue: 'frontend-service', description: 'List of build services')
    }
    stages {
        stage('Remote to k8s cluster') {
            steps {
                sh 'aws eks --region us-east-1 update-kubeconfig --name HaTN5-Eks-Cluster'
                sh "aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | sudo docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"
                sh 'kubectl create secret docker-registry ecr-secret \
                    --docker-server=523411581086.dkr.ecr.us-east-1.amazonaws.com \
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