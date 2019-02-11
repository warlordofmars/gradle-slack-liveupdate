package com.github.warlordofmars.gradle.slack

import com.github.warlordofmars.gradle.slack.model.SlackAttachmentCreator

import com.github.seratch.jslack.Slack
import com.github.seratch.jslack.api.methods.request.channels.*;
import com.github.seratch.jslack.api.methods.response.channels.*;
import com.github.seratch.jslack.api.model.Channel
import com.github.seratch.jslack.api.model.Attachment
import com.github.seratch.jslack.api.methods.response.chat.ChatUpdateResponse
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse
import com.github.seratch.jslack.api.methods.response.files.FilesUploadResponse
import com.github.seratch.jslack.api.methods.response.reactions.ReactionsAddResponse
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest
import com.github.seratch.jslack.api.methods.request.chat.ChatUpdateRequest
import com.github.seratch.jslack.api.methods.request.files.FilesUploadRequest
import com.github.seratch.jslack.api.methods.request.reactions.ReactionsAddRequest

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.logging.StandardOutputListener
import org.gradle.api.tasks.TaskState

import org.ajoberstar.grgit.Grgit

import java.util.Date
import groovy.time.TimeCategory


class SlackPlugin implements Plugin<Project> {

    private static final String TIMESTAMP_FILE = "slack-timestamp.txt"
    SlackPluginExtension mExtension
    StringBuilder mTaskLogBuilder

    void apply(Project project) {

        mTaskLogBuilder = new StringBuilder()
        mExtension = project.extensions.create('slack', SlackPluginExtension)
        
        project.getRootProject().ext {
            completedTasks = []
            buildStartTime = new Date()
            getRuntime = {
                use(TimeCategory) {
                    def duration = new Date() - buildStartTime
                    return duration.seconds
                }
            }
            git = Grgit.open(dir: project.getRootProject().projectDir)
        }

        def timestampFile = project.file("${project.buildDir}/${TIMESTAMP_FILE}")
        if(timestampFile.exists()) {
            project.getRootProject().ext.firstMessageTimestamp = timestampFile.text
        }
        
        project.afterEvaluate {
            if (mExtension.channel != null && mExtension.enabled)
                monitorTasksLifecyle(project)
        }
    }

    void monitorTasksLifecyle(Project project) {

        
        project.getGradle().getTaskGraph().addTaskExecutionListener(new TaskExecutionListener() {
            @Override
            void beforeExecute(Task task) {
                task.logging.addStandardOutputListener(new StandardOutputListener() {
                    @Override
                    void onOutput(CharSequence charSequence) {
                        mTaskLogBuilder.append(charSequence)
                    }
                })
            }

            @Override
            void afterExecute(Task task, TaskState state) {
                handleTaskFinished(task, state, project)
            }
        })
    }

    void handleTaskFinished(Task task, TaskState state, Project project) {
        Throwable failure = state.getFailure()
        
        project.getRootProject().completedTasks << task
        
        Attachment slackMessage = SlackAttachmentCreator.buildSlackAttachment(task, state, mTaskLogBuilder.toString())
        
        String token = mExtension.token
        Slack slack = Slack.getInstance()

        ChannelsListResponse channelsResponse = slack.methods().channelsList(
            ChannelsListRequest.builder().token(token).build());

        Channel general = channelsResponse.getChannels().find { it.name == mExtension.channel}

        if(!project.hasProperty('firstMessageTimestamp')) {
            ChatPostMessageResponse postResponse = slack.methods().chatPostMessage(
                ChatPostMessageRequest.builder()
                    .token(token)
                    .channel(general.getId())
                    .attachments([slackMessage])
                    .build())
            recordFirstMessageTimestamp("${postResponse.getMessage().getTs()}", project)
        } else {
            ChatUpdateResponse updateResponse = slack.methods().chatUpdate(
                ChatUpdateRequest.builder()
                    .token(token)
                    .channel(general.getId())
                    .ts(project.firstMessageTimestamp)
                    .attachments([slackMessage])
                    .build())
        }

        boolean success = failure == null

        def rootProject = task.getProject().rootProject
        def gradle = rootProject.getGradle()
        def completedTasks = rootProject.completedTasks.size()
        def startingTasks = gradle.startParameter.taskNames.join(", ")
        def totalTasks = gradle.getTaskGraph().getAllTasks().size()

        def completed = completedTasks == totalTasks

        if(completed || failure) {

            def buildLog = FilesUploadRequest.builder()
                .token(token)
                .channels([general.getId()])
                .threadTs(project.firstMessageTimestamp)
                .content(mTaskLogBuilder.toString())
                .build()
            
            if(System.env.containsKey('BUILD_NUMBER')) {
                if(rootProject.hasProperty('git')) {
                    buildLog.title = "${project.name}-${rootProject.git.branch.current.name}-build-${System.env.BUILD_NUMBER}.log"
                } else {
                    buildLog.title = "${project.name}-build-${System.env.BUILD_NUMBER}.log"
                }
            } else {
                if(rootProject.hasProperty('git')) {
                    buildLog.title = "${project.name}-${rootProject.git.branch.current.name}-build.log"
                } else {
                    buildLog.title = "${project.name}-build.log"
                }
            }            

            FilesUploadResponse buildLogUpload = slack.methods().filesUpload(buildLog)


            ReactionsAddRequest reaction = ReactionsAddRequest.builder()
                        .token(token)
                        .channel(general.getId())
                        .timestamp(project.firstMessageTimestamp)
                        .build()
                        

            if(completed && success) {
                reaction.name = 'tada'
            }

            if(failure) {
                reaction.name = 'cry'
            }

            ReactionsAddResponse successReaction = slack.methods().reactionsAdd(reaction)
        }

        

        

        
    }

    void recordFirstMessageTimestamp(String firstMessageTimestamp, Project project) {
        project.ext.firstMessageTimestamp = firstMessageTimestamp
        project.file("${project.buildDir}").mkdirs()
        project.file("${project.buildDir}/${TIMESTAMP_FILE}").write(firstMessageTimestamp)
    }

}