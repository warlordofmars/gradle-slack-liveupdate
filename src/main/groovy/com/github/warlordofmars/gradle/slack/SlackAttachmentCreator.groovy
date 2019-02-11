package com.github.warlordofmars.gradle.slack.model

import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;

import org.gradle.api.Task
import org.gradle.api.tasks.TaskState


class SlackAttachmentCreator {
    private static final String COLOR_PASSED = 'good'
    private static final String COLOR_FAILED = 'danger'
    private static final String TASK_TITLE = 'task'
    private static final String FAILURE_TITLE = 'failure reason'
    private static final String DESCRIPTION_TITLE = 'description'
    private static final String BRANCH_TITLE = 'branch'
    private static final String AUTHOR_TITLE = 'author'
    private static final String COMMIT_TITLE = 'commit'
    private static final String TASKS_TITLE = 'tasks'
    private static final String TESTS_TITLE = 'tests'
    private static final String RUNTIME_TITLE = 'runtime'
    private static final String RUNNING_STATUS = 'running :woman-running:'
    private static final String COMPLETED_STATUS = 'completed :white_check_mark:'
    private static final String FAILURE_STATUS = 'failed :fire:'

    static Attachment buildSlackAttachment(Task task, TaskState state, String taskLog) {
     
        Throwable failure = state.getFailure()
        boolean success = failure == null

        def rootProject = task.getProject().rootProject
        def gradle = rootProject.getGradle()
        def completedTasks = rootProject.completedTasks.size()
        def startingTasks = gradle.startParameter.taskNames.collect { "`${it}`" }.join(", ")
        def totalTasks = gradle.getTaskGraph().getAllTasks().size()

        def completed = completedTasks == totalTasks

        def fields = []

        if(rootProject.ext.has('getRuntime')) {
            def runtime = rootProject.getRuntime()

            Field runtimeField = Field.builder()
                .title(RUNTIME_TITLE)
                .value("${runtime} s")
                .valueShortEnough(true)
                .build()
            
            fields << runtimeField
        }

        Field numberOfTasksField = Field.builder()
            .title(TASKS_TITLE)
            .value("${completedTasks} of ${totalTasks}")
            .valueShortEnough(true)
            .build()
        
        fields << numberOfTasksField
        
        if(!completed || failure) {

            Field taskField = Field.builder()
                .title(TASK_TITLE)
                .value("${task.getProject().name} : ${task.getName()}")
                .valueShortEnough(true)
                .build()
            
            fields << taskField

            if(task.getDescription() != null) {

                Field descriptionField = Field.builder()
                    .title(DESCRIPTION_TITLE)
                    .value(task.getDescription())
                    .valueShortEnough(false)
                    .build()
                
                fields << descriptionField

            }

        }

        if(rootProject.ext.has('getTestCounts')) {
            def testCounts = rootProject.getTestCounts()

            Field testsField = Field.builder()
                .title(TESTS_TITLE)
                .value("${testCounts['total']} total / ${testCounts['success']} successful / ${testCounts['failed']} failed / ${testCounts['skipped']} skipped")
                .valueShortEnough(false)
                .build()
            
            fields << testsField
        }
        
        if(rootProject.hasProperty('git')) {

            Field branchField = Field.builder()
                .title(BRANCH_TITLE)
                .value()
                .valueShortEnough(true)
                .build()

            def branch = rootProject.git.branch.current.name
            
            if(rootProject.group.startsWith('com.github') && rootProject.group.split('\\.').size() > 2) {
                def organization = rootProject.group.split('\\.')[2]
                def gitHubUrl = "https://github.com/${organization}/${rootProject.name}/tree/${branch}"
                branchField.setValue("<${gitHubUrl}|${branch}>")
            } else {
                branchField.setValue(branch)
            }
            
            fields << branchField

            def lastCommmit = rootProject.git.head()

            Field authorField = Field.builder()
                .title(AUTHOR_TITLE)
                .value("<mailto:${lastCommmit.author.email}|${lastCommmit.author.name}>")
                .valueShortEnough(true)
                .build()
            
            fields << authorField

            Field commitField = Field.builder()
                .title(COMMIT_TITLE)
                .value("```${lastCommmit.fullMessage}```")
                .valueShortEnough(false)
                .build()
            
            fields << commitField
        }

        if (!success && failure != null && failure.getCause() != null) {

            Field failureField = Field.builder()
                .title(FAILURE_TITLE)
                .value("```${failure.getCause()}```")
                .valueShortEnough(false)
                .build()
            
            fields << failureField

        }
        
        String status = failure ? FAILURE_STATUS : completed ? COMPLETED_STATUS : RUNNING_STATUS
        
        Attachment attachment = Attachment.builder()
            .fields(fields)
            .color(success ? COLOR_PASSED : COLOR_FAILED)
            .text("started with tasks ${startingTasks} at _${rootProject.buildStartTime}_")
            .build()

        if(System.env.containsKey('BUILD_NUMBER')) {
            if(rootProject.hasProperty('git')) {
                attachment.title = "${task.getProject().rootProject.name} build #${System.env.BUILD_NUMBER} on ${rootProject.git.branch.current.name} ${status}"
                attachment.footer = "${task.getProject().rootProject.name} ${rootProject.git.branch.current.name} #${System.env.BUILD_NUMBER}"
            } else {
                attachment.title = "${task.getProject().rootProject.name} build #${System.env.BUILD_NUMBER} ${status}"
                attachment.footer = "${task.getProject().rootProject.name} #${System.env.BUILD_NUMBER}"
            }
        } else {
            if(rootProject.hasProperty('git')) {
                attachment.title = "${task.getProject().rootProject.name} build on ${rootProject.git.branch.current.name} ${status}"
                attachment.footer = "${task.getProject().rootProject.name} ${rootProject.git.branch.current.name}"
            } else {
                attachment.title = "${task.getProject().rootProject.name} build ${status}"
                attachment.footer = "${task.getProject().rootProject.name}"
            }
        }

        if(System.env.containsKey('RUN_DISPLAY_URL')) {
            attachment.titleLink = System.env.RUN_DISPLAY_URL
        }

        return attachment
    }
}
