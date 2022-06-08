@NonCPS
def getChangeString() {
    MAX_MSG_LEN = 100
    def changeString = ""
    echo "Gathering SCM changes"
    def changeLogSets = currentBuild.changeSets
    for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = 0; j < entries.length; j++) {
            def entry = entries[j]
            truncated_msg = entry.msg.take(MAX_MSG_LEN)
            changeString += " - ${truncated_msg} [${entry.author}]\n"
        }
    }

    if (!changeString) {
        changeString = " - No new changes"
    }
    return changeString
}
// Changes func
properties([
    buildDiscarder(logRotator(daysToKeepStr: '', numToKeepStr: '3')),
])
timestamps {
    node {
        try {
            stage('Checkout SCM') {
                checkout([
                    $class                           : 'GitSCM',
                    branches                         : [[name: "${env.BRANCH_NAME}"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions                       : [],
                    submoduleCfg                     : [],
                    userRemoteConfigs                : [[credentialsId: 'awx.integrations', url: "git@gitlab.citeck.ru:citeck-projects/ecos-notifications-lib.git"]]
                ])
            }

            def project_version = readMavenPom().getProperties().getProperty("revision")

            mattermostSend endpoint: 'https://mm.citeck.ru/hooks/9ytch3uox3retkfypuq7xi3yyr', channel: "build_notifications", color: 'good', message: " :arrow_forward: **Build project ecos-notifications-lib:**\n**Branch:** ${env.BRANCH_NAME}\n**Version:** ${project_version}\n**Build id:** ${env.BUILD_NUMBER}\n**Build url:** ${env.BUILD_URL}\n**Changes:**\n" + getChangeString()

            if (!(env.BRANCH_NAME ==~ /master(-\d)?/) && (!project_version.contains('SNAPSHOT'))) {
                def tag = ""
                try {
                    tag = sh(script: "git describe --exact-match --tags", returnStdout: true).trim()
                } catch (Exception e) {
                    // no tag
                }
                def buildStopMsg = ""
                if (tag == "") {
                    buildStopMsg = "You should add tag with version to build release from non-master branch. Version: " + project_version
                } else if (tag != project_version) {
                    buildStopMsg = "Release tag doesn't match version. Tag: " + tag + " Version: " + project_version
                }
                if (buildStopMsg != "") {
                    echo buildStopMsg
                    buildTools.notifyBuildWarning(repoUrl, buildStopMsg, env)
                    currentBuild.result = 'NOT_BUILT'
                    return
                }
            }

            stage('Assembling and publishing project artifacts') {
                withMaven(mavenLocalRepo: '/opt/jenkins/.m2/repository', tempBinDir: '') {
                    sh "mvn clean deploy"
                }
            }
        }
        catch (Exception e) {
            currentBuild.result = 'FAILURE'
            error_message = e.getMessage()
            echo error_message
        }
        script {
            if (currentBuild.result != 'FAILURE') {
                mattermostSend endpoint: 'https://mm.citeck.ru/hooks/9ytch3uox3retkfypuq7xi3yyr', channel: "build_notifications", color: 'good', message: " :white_check_mark: **Build project with ID ${env.BUILD_NUMBER} complete!**"
            } else {
                mattermostSend endpoint: 'https://mm.citeck.ru/hooks/9ytch3uox3retkfypuq7xi3yyr', channel: "build_notifications", color: 'danger', message: " @channel :exclamation: **Build project with ID ${env.BUILD_NUMBER} failure with message:**\n```${error_message}```"
            }
        }
    }
}
