pipeline {
    agent any
    parameters {
        booleanParam(name: 'CREATE_INFARSTRUCTURE', defaultValue: false, description: 'Flag to trigger create infrastructure')
        choice(
            name: 'BRANCH_BUILD',
            choices: ['master', 'main', 'production'],
            description: 'Branch build from git'
        )
        checkboxParameter(name: 'BUILD_SERVICES', format: 'JSON',
            pipelineSubmitContent: '{"CheckboxParameter": [{"key": "Frontend Service","value": "frontend-service"}]}', description: '')
    }
    stages {
        stage ("Lint Dockerfile") {
            steps {
                sh 'hadolint Dockerfile'
            }
        }
        stage('BUILD_FRONTEND_SERVICE') {
            steps {
                build(job: 'BUILD_FRONTEND_SERVICE', parameters: [
                    string(name: 'BRANCH_BUILD', value: String.valueOf(BRANCH_BUILD)),
                    string(name: 'BUILD_SERVICES', value: String.valueOf(BUILD_SERVICES))
                ])
            }
        }

        stage('CREATE_INFRASTRUCTURE') {
            when{
                expression {
                    return Boolean.valueOf(CREATE_INFARSTRUCTURE)
                }
            }
            steps {
                sh 'cd cloudformation && aws cloudformation create-stack --stack-name hatn5-project5-stack --template-body file://infrastructure.yml  --parameters file://parameters.json --capabilities "CAPABILITY_IAM" "CAPABILITY_NAMED_IAM" --region=us-east-1'
                sh 'aws cloudformation wait stack-create-complete --region us-east-1 --stack-name project5-stack'
            }
        }

        stage('DEPLOY_SERVERS') {
            steps {
                build(job: 'DEPLOYMENT', parameters: [
                    booleanParam(name: 'CREATE_INFAR', value: Boolean.valueOf(CREATE_INFAR)),
                    string(name: 'BRANCH_BUILD', value: String.valueOf(BRANCH_BUILD)),
                    string(name: 'BUILD_SERVICES', value: String.valueOf(BUILD_SERVICES))
                ])
            }
        }
    }
    post {
        always {
            deleteDir() /* clean up our workspace */
        }
    }
}