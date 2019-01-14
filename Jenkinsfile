node {
    def buildNr = "SNAPSHOT"

    def publishAmpTask = "publishAmpPublicationToSnapshotRepository"
    def publishJarTask = "publishMavenJavaPublicationToSnapshotRepository"
    def publishIntegrationJarTask = "publishIntegrationJarPublicationToSnapshotRepository"

    stage('Checkout') {
        checkout scm

        if (env.BRANCH_NAME == "master") {
            buildNr = env.BUILD_NUMBER
            publishAmpTask = "publishAmpPublicationToReleaseRepository"
            publishJarTask = "publishMavenJavaPublicationToReleaseRepository"
            publishIntegrationJarTask = "publishIntegrationJarPublicationToReleaseRepository"
        }
    }

    try {
         stage('Testing 4.2') {
            sh "./gradlew clean :c4a-42-integration-testing:integrationTest -PbuildNumber=${buildNr} -i"
        }

        stage('Testing 5.x') {
            sh "./gradlew clean :c4a-integration-testing:integrationTest -PbuildNumber=${buildNr} -i"
        }

        stage('Building AMP') {
            sh "./gradlew :care4alf:ampde -PbuildNumber=${buildNr} --continue -i"

            def artifacts = [
                    'care4alf/build/libs/*.jar',
                    'care4alf/build/distributions/*.amp'
            ]

            archiveArtifacts artifacts: artifacts.join(','), excludes: '**/*-sources.jar'
        }

        stage('Publishing') {
            sh "./gradlew :care4alf:${publishAmpTask} :care4alf:${publishJarTask} -PbuildNumber=${buildNr}  --continue -i"
        }

    } catch (err) {
        echo "Exception: ${err.getMessage()}"
        currentBuild.result = "FAILED"
    } finally {
        junit '**/build/**/TEST-*.xml'
        cleanWs()
    }
}
