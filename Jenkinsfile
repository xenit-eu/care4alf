def sendEmailNotifications(boolean isMasterOrRelease) {
    def color = 'purple'
    switch (currentBuild.currentResult) {
        case 'FAILURE': case 'UNSTABLE':
            color = 'red'
            break
        case 'SUCCESS':
            color = 'green'
            break
    }
    subject = "C4A build ${env.JOB_NAME} #${env.BUILD_NUMBER}: ${currentBuild.currentResult}"
    body = """<html><body>
        <p>
            <b>C4A build in Job ${env.JOB_NAME} #${env.BUILD_NUMBER}: <span style="color: ${color};">${currentBuild.currentResult}</span></b>
        </p>
        <p>
            Check console output at <a href='${env.BUILD_URL}'>${env.JOB_NAME}</a>
        </p></body></html>"""
    if (isMasterOrRelease) {
        emailext(
                subject: subject,
                body: body,
                mimeType: 'text/html',
                to: 'team-coolguys@xenit.eu',
                recipientProviders: [requestor(), developers(), culprits(), brokenBuildSuspects()]
        )
    } else {
        emailext(
                subject: subject,
                body: body,
                mimeType: 'text/html',
                recipientProviders: [requestor(), developers(), culprits(), brokenBuildSuspects()]
        )
    }
}

def isJobStartedByCronJob() {
    return currentBuild.getBuildCauses('hudson.triggers.TimerTrigger$TimerTriggerCause').size() != 0
}

if (isJobStartedByCronJob() && "${env.BRANCH_NAME}" != "master") {
    echo "Aborting build because build was triggered by cron trigger and this is not the master branch"
    currentBuild.result = 'ABORTED'
    return
}

properties(
        [
                pipelineTriggers([cron('H H(0-6) 28 * *')])
        ]
)

node {
    def buildNr = "SNAPSHOT"

    stage('Checkout') {
        checkout scm

        if (env.BRANCH_NAME == "release") {
            buildNr = env.BUILD_NUMBER
        }
    }

    try {
        stage('Unit Tests') {
            sh "./gradlew clean :c4a-impl:test -PbuildNumber=${buildNr} -i"
        }

        stage('Integration Tests') {
            parallel(
                    'Testing 5.x': {
                        sh "./gradlew :c4a-test:test-5x:integrationTest -PbuildNumber=${buildNr} -i"
                    },
                    'Testing 6.x': {
                        sh "./gradlew :c4a-test:test-6x:integrationTest -PbuildNumber=${buildNr} -i"
                    }
            )
        }

        stage('Building AMP') {
            sh "./gradlew :c4a-impl:care4alf-5x:amp -PbuildNumber=${buildNr} --continue -i"
            sh "./gradlew :c4a-impl:care4alf-6x:amp -PbuildNumber=${buildNr} --continue -i"
        }

        stage('Publishing') {
            if (env.BRANCH_NAME == "release") {
                def sonatypeCredentials = usernamePassword(
                        credentialsId: 'sonatype',
                        passwordVariable: 'sonatypePassword',
                        usernameVariable: 'sonatypeUsername'
                );
                def gpgCredentials = string(credentialsId: 'gpgpassphrase', variable: 'gpgPassPhrase');
                withCredentials([sonatypeCredentials, gpgCredentials]) {
                    for (project in ['care4alf-5x', 'care4alf-6x']) {
                        sh """./gradlew :c4a-impl:${project}:publishMavenJavaPublicationToSonatypeRepository -i \
                            -PbuildNumber=${buildNr} \
                            -Ppublish_username=${sonatypeUsername} \
                            -Ppublish_password=${sonatypePassword} \
                            -PkeyId=DF8285F0 \
                            -Ppassword=${gpgPassPhrase} \
                            -PsecretKeyRingFile=/var/jenkins_home/secring.gpg"""
                    }
                }
            }
        }

    } catch (err) {
        echo "Exception: ${err.getMessage()}"
        currentBuild.result = "FAILED"
    } finally {
        junit '**/build/**/TEST-*.xml'
        sh "./gradlew :c4a-test:test-5x:composeDownForced -i"
        sh "./gradlew :c4a-test:test-6x:composeDownForced -i"
        boolean isMasterOrRelease = env.BRANCH_NAME == "release" || env.BRANCH_NAME == "master"
        sendEmailNotifications(isMasterOrRelease)
        cleanWs()
    }
}
