
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
        sh "./gradlew clean test --continue -i"

        stage 'Building AMP'
        sh "./gradlew :amp --continue -i"
        
        stage 'Building integrationJar'
        sh "./gradlew :integrationJar --continue -i"

        stage 'Publishing'
        sh "./gradlew :${publishAmpTask} :${publishJarTask} :${publishIntegrationJarTask} -PbuildNumber=${buildNr}  --continue -i"

    } catch (err) {
        currentBuild.result = "FAILED"
    } finally {
        step([$class: "JUnitResultArchiver", testResults: "**/build/**/TEST-*.xml"])
    }

}
