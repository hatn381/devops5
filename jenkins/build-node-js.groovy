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
    parameters {
        string(name: 'BRANCH_BUILD', defaultValue: 'staging', description: 'The branch of git')
        string(name: 'BUILD_SERVICES', defaultValue: '', description: 'List of build services')
    }
    stages{
        stage('Checkout'){
            steps{
                checkout([   $class: 'GitSCM',
                branches: [[name: "${BRANCH_BUILD}"]],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'CleanBeforeCheckout'],
                            [$class: 'SubmoduleOption',
                            disableSubmodules: false,
                            parentCredentials: true,
                            recursiveSubmodules: true,
                            reference: '',
                            trackingSubmodules: false],
                            [$class: 'RelativeTargetDirectory',
                            relativeTargetDir: "${WORKSPACE}"]],
                submoduleCfg: [],
                userRemoteConfigs: [[credentialsId: 'hatn5', url: "${GIT_URL}"]]
                ])
            }
        }
        stage('Login into AWS ECR') {
            steps {
                sh "pwd"
                sh "aws configure list"
                sh "aws ecr get-login-password --region ${AWS_DEFAULT_REGION} | sudo docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"  
            }
        }
        stage('Build Cloud Config Server'){
            when{
                expression {
                    return "${BUILD_SERVICES}".contains("frontend-service")
                }
            }
            environment {
                IMAGE_REPO_NAME="frontend-repo"
            }
            steps{
                // Build docker image
                sh "cd ${WORKSPACE} && sudo docker build -t ${IMAGE_REPO_NAME}:${BRANCH_BUILD}_${IMAGE_TAG} ."
                // Tag docker image
                sh "sudo docker tag ${IMAGE_REPO_NAME}:${BRANCH_BUILD}_${IMAGE_TAG} ${REPOSITORY_URI}/${IMAGE_REPO_NAME}:${BRANCH_BUILD}_${IMAGE_TAG}"
                // Push image to ECR repository
                sh "sudo docker push ${REPOSITORY_URI}/${IMAGE_REPO_NAME}:${BRANCH_BUILD}_${IMAGE_TAG}"
            }
        }

    }
    post {
        always {
            deleteDir() /* clean up our workspace */
        }
    }
}