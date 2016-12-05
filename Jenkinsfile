
node {

    // installBundle for dyn ext  for test
    env.ORG_GRADLE_PROJECT_host="gio.dev.xenit.eu"
    env.ORG_GRADLE_PROJECT_protocol="https"
    env.ORG_GRADLE_PROJECT_port="443"
    env.ORG_GRADLE_PROJECT_username="admin"
    env.ORG_GRADLE_PROJECT_password="admin"

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
        sh "./gradlew clean :installBundle :test --continue -i"

        stage 'Building AMP'
        sh "./gradlew :ampde --continue -i"

        def artifacts = [
           'build/libs/*.jar',
           'build/distributions/*.amp'
        ]

        archiveArtifacts artifacts: artifacts.join(','), excludes: '**/*-sources.jar'

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
