#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags and push to GitHub.
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    def gitRepoUrl = config.gitRepoUrl ?: 'https://github.com/oliversims/tws-e-commerce-app_hackathon.git'
    def dockerImageName = config.dockerImageName ?: 'simsoliver/easyshop-app'
    def dockerMigrationImageName = config.dockerMigrationImageName ?: 'simsoliver/easyshop-migration'

    // github.com/owner/repo.git -> owner/repo
    def gitRepoPath = gitRepoUrl
        .replaceFirst('^https://github.com/', '')
        .replaceFirst('\\.git$', '')

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    echo "Pushing manifest changes to: ${gitRepoPath}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"

            sed -i "s|image: .*easyshop-app.*|image: ${dockerImageName}:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml

            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: .*easyshop-migration.*|image: ${dockerMigrationImageName}:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} [ci skip]"
                git push "https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/${gitRepoPath}.git" HEAD:\${GIT_BRANCH}
            fi
        """
    }
}
