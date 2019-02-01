package com.github.warlordofmars.gradle.slack

import com.github.warlordofmars.gradle.slack.model.SlackAttachmentCreator

import com.github.seratch.jslack.Slack
import com.github.seratch.jslack.api.methods.request.channels.*;
import com.github.seratch.jslack.api.methods.response.channels.*;
import com.github.seratch.jslack.api.model.Channel
import com.github.seratch.jslack.api.model.Attachment
import com.github.seratch.jslack.api.methods.response.chat.ChatUpdateResponse
import com.github.seratch.jslack.api.methods.response.chat.ChatPostMessageResponse
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest
import com.github.seratch.jslack.api.methods.request.chat.ChatUpdateRequest

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.logging.StandardOutputListener
import org.gradle.api.tasks.TaskState

import org.ajoberstar.grgit.Grgit


class SlackPlugin implements Plugin<Project> {

    private static final String TIMESTAMP_FILE = "slack-timestamp.txt"
    SlackPluginExtension mExtension
    StringBuilder mTaskLogBuilder

    void apply(Project project) {

        mTaskLogBuilder = new StringBuilder()
        mExtension = project.extensions.create('slack', SlackPluginExtension)
        
        project.getRootProject().ext {
            completedTasks = []
            startTime = System.nanoTime()
            getRuntime = {
                def diff = System.nanoTime() - startTime
                def seconds = diff/1000/1000/1000
                return String.format("%.2f", seconds)
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
                    .build());
        }
   
    }

    void recordFirstMessageTimestamp(String firstMessageTimestamp, Project project) {
        project.ext.firstMessageTimestamp = firstMessageTimestamp
        project.file("${project.buildDir}").mkdirs()
        project.file("${project.buildDir}/${TIMESTAMP_FILE}").write(firstMessageTimestamp)
    }

}