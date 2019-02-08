package com.github.warlordofmars.gradle.slack.model

import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;

import org.gradle.api.Task
import org.gradle.api.tasks.TaskState


class SlackAttachmentCreator {
    private static final String COLOR_PASSED = 'good'
    private static final String COLOR_FAILED = 'danger'
    private static final String PROJECT_TITLE = 'Project'
    private static final String STATUS_TITLE = 'Status'
    private static final String TASK_TITLE = 'Task'
    private static final String STARTING_TASKS_TITLE = 'Starting Tasks'
    private static final String FAILURE_TITLE = 'Failure'
    private static final String DESCRIPTION_TITLE = 'Description'
    private static final String TASK_RESULT_TITLE = 'Task Result'
    private static final String TASK_RESULT_PASSED = 'Passed'
    private static final String TASK_RESULT_FAILED = 'Failed'
    private static final String BRANCH_TITLE = 'Branch'
    private static final String AUTHOR_TITLE = 'Author'
    private static final String COMMIT_TITLE = 'Commit'
    private static final String TASKS_TITLE = 'Tasks'
    private static final String TESTS_TITLE = 'Tests'
    private static final String RUNTIME_TITLE = 'Runtime'
    private static final String RUNNING_STATUS = 'Running :woman-running:'
    private static final String COMPLETED_STATUS = 'Completed :white_check_mark:'
    private static final String FAILURE_STATUS = 'Failure :fire:'

    static Attachment buildSlackAttachment(Task task, TaskState state, String taskLog) {
     
        Throwable failure = state.getFailure()
        boolean success = failure == null

        def rootProject = task.getProject().rootProject
        def gradle = rootProject.getGradle()
        def completedTasks = rootProject.completedTasks.size()
        def startingTasks = gradle.startParameter.taskNames.join(", ")
        def totalTasks = gradle.getTaskGraph().getAllTasks().size()

        def completed = completedTasks == totalTasks

        def fields = []

        Field projectField = Field.builder()
            .title(PROJECT_TITLE)
            .value("${task.getProject().rootProject.name}")
            .valueShortEnough(true)
            .build()
        
        fields << projectField

        Field startingTasksField = Field.builder()
            .title(STARTING_TASKS_TITLE)
            .value("${startingTasks}")
            .valueShortEnough(true)
            .build()
        
        fields << startingTasksField

        Field statusField = Field.builder()
            .title(STATUS_TITLE)
            .value(failure ? FAILURE_STATUS : completed ? COMPLETED_STATUS : RUNNING_STATUS)
            .valueShortEnough(true)
            .build()
        
        fields << statusField

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
                .value(":${task.getProject().name}:${task.getName()}")
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
        
        if(rootProject.hasProperty('git')) {

            Field branchField = Field.builder()
                .title(BRANCH_TITLE)
                .value(rootProject.git.branch.current.name)
                .valueShortEnough(true)
                .build()
            
            fields << branchField

            def lastCommmit = rootProject.git.head()

            Field authorField = Field.builder()
                .title(AUTHOR_TITLE)
                .value(lastCommmit.author.email)
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

        if(rootProject.ext.has('getTestCounts')) {
            def testCounts = rootProject.getTestCounts()

            Field testsField = Field.builder()
                .title(TESTS_TITLE)
                .value("${testCounts['total']} total / ${testCounts['success']} successful / ${testCounts['failed']} failed / ${testCounts['skipped']} skipped")
                .valueShortEnough(false)
                .build()
            
            fields << testsField
        }

        if (!success && failure != null && failure.getCause() != null) {

            Field failureField = Field.builder()
                .title(FAILURE_TITLE)
                .value("```${failure.getCause()}```")
                .valueShortEnough(false)
                .build()
            
            fields << failureField

        }
        
        Attachment attachment = Attachment.builder()
            .fields(fields)
            .color(success ? COLOR_PASSED : COLOR_FAILED)
            .build()

        // String messageHeader = "Build " + ( success ? TASK_RESULT_PASSED : TASK_RESULT_FAILED)        
        // attachments.setFallback(messageHeader)

        return attachment
    }
}
