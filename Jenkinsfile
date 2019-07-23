node {
    def buildNr = "SNAPSHOT"

    def publishMavenJavaTask = "publishMavenJavaPublicationToSnapshotRepository"
    def publishIntegrationJarTask = "publishIntegrationJarPublicationToSnapshotRepository"

    stage('Checkout') {
        checkout scm

        if (env.BRANCH_NAME == "release") {
            buildNr = env.BUILD_NUMBER
            publishMavenJavaTask = "publishMavenJavaPublicationToReleaseRepository"
            publishIntegrationJarTask = "publishIntegrationJarPublicationToReleaseRepository"
        }
    }

    try {

        stage('Testing 5.x') {
            sh "./gradlew clean :c4a-test:test-5x:integrationTest -PbuildNumber=${buildNr} -i"
        }

        stage('Testing 6.0') {
            sh "./gradlew clean :c4a-test:test-60:integrationTest -PbuildNumber=${buildNr} -i"
        }

        stage('Building AMP') {
            sh "./gradlew :c4a-impl:care4alf-5x:amp -PbuildNumber=${buildNr} --continue -i"
            sh "./gradlew :c4a-impl:care4alf-60:amp -PbuildNumber=${buildNr} --continue -i"

            def artifacts = [
                    'c4a-impl/5x/build/libs/*.jar',
                    'c4a-impl/60/build/libs/*.jar',
                    'c4a-impl/5x/build/distributions/*.amp',
                    'c4a-impl/60/build/distributions/*.amp'
            ]

            archiveArtifacts artifacts: artifacts.join(','), excludes: '**/*-sources.jar'
        }

        stage('Publishing') {
            sh "./gradlew :c4a-impl:care4alf-5x:${publishMavenJavaTask} -PbuildNumber=${buildNr}  --continue -i"
            sh "./gradlew :c4a-impl:care4alf-60:${publishMavenJavaTask} -PbuildNumber=${buildNr}  --continue -i"
        }

    } catch (err) {
        echo "Exception: ${err.getMessage()}"
        currentBuild.result = "FAILED"
    } finally {
        junit '**/build/**/TEST-*.xml'
        cleanWs()
    }
}
