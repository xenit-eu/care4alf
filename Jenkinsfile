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
        stage ('Unit Tests') {
            sh "./gradlew clean :c4a-impl:test -PbuildNumber=${buildNr} -i"
        }

        stage('Testing 5.x') {
            sh "./gradlew clean :c4a-test:test-5x:integrationTest -PbuildNumber=${buildNr} -i"
        }

        stage('Testing 6.0') {
            sh "./gradlew clean :c4a-test:test-6x:integrationTest -PbuildNumber=${buildNr} -i"
        }

        stage('Building AMP') {
            sh "./gradlew :c4a-impl:care4alf-5x:amp -PbuildNumber=${buildNr} --continue -i"
            sh "./gradlew :c4a-impl:care4alf-6x:amp -PbuildNumber=${buildNr} --continue -i"
        }

        stage('Publishing') {
            sh "./gradlew :c4a-impl:care4alf-5x:${publishMavenJavaTask} -PbuildNumber=${buildNr}  --continue -i"
            sh "./gradlew :c4a-impl:care4alf-6x:${publishMavenJavaTask} -PbuildNumber=${buildNr}  --continue -i"
        }

    } catch (err) {
        echo "Exception: ${err.getMessage()}"
        currentBuild.result = "FAILED"
    } finally {
        junit '**/build/**/TEST-*.xml'
        sh "./gradlew :c4a-test:test-5x:composeDownForced -i"
        sh "./gradlew :c4a-test:test-6x:composeDownForced -i"
        cleanWs()
    }
}
