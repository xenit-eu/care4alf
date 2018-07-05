
node {





    stage 'Checkout'
    checkout scm

    def gitBranch = env.BRANCH_NAME
    def buildNr = "SNAPSHOT"

    def publishAmpTask = "publishAmpPublicationToSnapshotRepository"
    def publishJarTask = "publishMavenJavaPublicationToSnapshotRepository"
    def publishIntegrationJarTask = "publishIntegrationJarPublicationToSnapshotRepository"
    if (gitBranch == "master") {
        buildNr = env.BUILD_NUMBER
        publishAmpTask = "publishAmpPublicationToReleaseRepository"
        publishJarTask = "publishMavenJavaPublicationToReleaseRepository"
        publishIntegrationJarTask = "publishIntegrationJarPublicationToReleaseRepository"
    }


    try {
        stage 'Testing'
        withCredentials([[$class: 'UsernamePasswordMultiBinding',
                                    credentialsId: 'jenkins-c4a.dev.xenit.eu/alfresco',
                                    usernameVariable: 'ALFRESCO_USERNAME',
                                    passwordVariable: 'ALFRESCO_PASSWORD']]) {

            // installBundle for dyn ext for test
            env.ORG_GRADLE_PROJECT_host="jenkins-c4a.dev.xenit.eu"
            env.ORG_GRADLE_PROJECT_protocol="https"
            env.ORG_GRADLE_PROJECT_port="443"

            sh "./gradlew clean :installBundle :test -PbuildNumber=${buildNr} -Phost=${ORG_GRADLE_PROJECT_host} -Pusername=${ALFRESCO_USERNAME} -Ppassword=${ALFRESCO_PASSWORD} -Pprotocol=${ORG_GRADLE_PROJECT_protocol} -i"
        }


        stage 'Building AMP'
        sh "./gradlew :ampde -PbuildNumber=${buildNr} --continue -i"

        def artifacts = [
           'build/libs/*.jar',
           'build/distributions/*.amp'
        ]

        archiveArtifacts artifacts: artifacts.join(','), excludes: '**/*-sources.jar'

        stage 'Building integrationJar'
        sh "./gradlew :integrationJar -PbuildNumber=${buildNr} --continue -i"

        stage 'Publishing'
        sh "./gradlew :${publishAmpTask} :${publishJarTask} :${publishIntegrationJarTask} -PbuildNumber=${buildNr}  --continue -i"

    } catch (err) {
        currentBuild.result = "FAILED"
    } finally {
        step([$class: "JUnitResultArchiver", testResults: "**/build/**/TEST-*.xml"])
    }

}
